/*
 * Copyright (C) 2014 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacpp.tools;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 *
 * @author Samuel Audet
 */
class Tokenizer implements Closeable {
    Tokenizer(Reader reader) {
        this.reader = reader;
    }
    Tokenizer(String string) {
        this.reader = new StringReader(string);
    }
    Tokenizer(File file) throws FileNotFoundException {
        this.file = file;
        this.reader = new BufferedReader(new FileReader(file));
    }

    File file = null;
    Reader reader = null;
    String lineSeparator = null;
    int lastChar = -1, lineNumber = 1;
    StringBuilder buffer = new StringBuilder();

    @Override public void close() throws IOException {
        reader.close();
    }

    int readChar() throws IOException {
        if (lastChar != -1) {
            int c = lastChar;
            lastChar = -1;
            return c;
        }
        int c = reader.read();
        if (c == '\r' || c == '\n') {
            lineNumber++;
            int c2 = c == '\r' ? reader.read() : -1;
            if (lineSeparator == null) {
                lineSeparator = c == '\r' && c2 == '\n' ? "\r\n" :
                                c == '\r' ? "\r" : "\n";
            }
            if (c2 != '\n') {
                lastChar = c2;
            }
            c = '\n';
        }
        return c;
    }

    public Token nextToken() throws IOException {
        Token token = new Token();
        int c = readChar();

        buffer.setLength(0);
        if (Character.isWhitespace(c)) {
            buffer.append((char)c);
            while ((c = readChar()) != -1 && Character.isWhitespace(c)) {
                buffer.append((char)c);
            }
        }
        token.file = file;
        token.lineNumber = lineNumber;
        token.spacing = buffer.toString();

        buffer.setLength(0);
        if (Character.isLetter(c) || c == '_') {
            token.type = Token.IDENTIFIER;
            buffer.append((char)c);
            while ((c = readChar()) != -1 && (Character.isDigit(c) || Character.isLetter(c) || c == '_')) {
                buffer.append((char)c);
            }
            token.value = buffer.toString();
            lastChar = c;
        } else if (Character.isDigit(c) || c == '.' || c == '-' ||  c == '+') {
            token.type = c == '.' ? Token.FLOAT : Token.INTEGER;
            buffer.append((char)c);
            int prevc = 0;
            boolean exp = false, large = false, unsigned = false, hex = false;
            while ((c = readChar()) != -1 && (Character.isDigit(c) || c == '.' || c == '-' || c == '+' ||
                   (c >= 'a' && c <= 'f') || c == 'l' || c == 'u' || c == 'x' ||
                   (c >= 'A' && c <= 'F') || c == 'L' || c == 'U' || c == 'X')) {
                switch (c) {
                    case '.': token.type = Token.FLOAT;  break;
                    case 'e': case 'E': exp      = true; break;
                    case 'l': case 'L': large    = true; break;
                    case 'u': case 'U': unsigned = true; break;
                    case 'x': case 'X': hex      = true; break;
                }
                if (c != 'l' && c != 'L' && c != 'u' && c != 'U') {
                    buffer.append((char)c);
                }
                prevc = c;
            }
            if (!hex && (exp || prevc == 'f' || prevc == 'F')) {
                token.type = Token.FLOAT;
            }
            if (token.type == Token.INTEGER && !large) {
                try {
                    long high = Long.decode(buffer.toString()) >> 32;
                    large = high != 0 && high != 0xFFFFFFFF;
                } catch (NumberFormatException e) { /* not an integer? */ }
            }
            if (token.type == Token.INTEGER && (large || (unsigned && !hex))) {
                buffer.append('L');
            }
            token.value = buffer.toString();
            lastChar = c;
        } else if (c == '"') {
            token.type = Token.STRING;
            buffer.append('"');
            int prevc = 0;
            while ((c = readChar()) != -1 && (prevc == '\\' || c != '"')) {
                buffer.append((char)c);
                prevc = c;
            }
            buffer.append('"');
            token.value = buffer.toString();
        } else if (c == '/') {
            c = readChar();
            if (c == '/') {
                token.type = Token.COMMENT;
                buffer.append('/').append('/');
                int prevc = 0;
                while ((c = readChar()) != -1 && (prevc == '\\' || c != '\n')) {
                    buffer.append((char)c);
                    prevc = c;
                }
                token.value = buffer.toString();
                lastChar = c;
            } else if (c == '*') {
                token.type = Token.COMMENT;
                buffer.append('/').append('*');
                int prevc = 0;
                while ((c = readChar()) != -1 && (prevc != '*' || c != '/')) {
                    buffer.append((char)c);
                    prevc = c;
                }
                buffer.append('/');
                token.value = buffer.toString();
            } else {
                lastChar = c;
                token.type = '/';
            }
        } else if (c == ':') {
            int c2 = readChar();
            if (c2 == ':') {
                token.type = Token.SYMBOL;
                token.value = "::";
            } else {
                token.type = c;
                lastChar = c2;
            }
        } else if (c == '#') {
            int c2 = readChar();
            if (c2 == '#') {
                token.type = Token.SYMBOL;
                token.value = "##";
            } else {
                token.type = c;
                lastChar = c2;
            }
        } else {
            if (c == '\\') {
                int c2 = readChar();
                if (c2 == '\n') {
                    token.type = Token.COMMENT;
                    token.value = "\n";
                    return token;
                } else {
                    lastChar = c2;
                }
            }
            token.type = c;
        }
        return token;
    }

    Token[] tokenize() {
        ArrayList<Token> tokens = new ArrayList<Token>();
        try {
            Token token;
            while (!(token = nextToken()).isEmpty()) {
                tokens.add(token);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return tokens.toArray(new Token[tokens.size()]);
    }
}
