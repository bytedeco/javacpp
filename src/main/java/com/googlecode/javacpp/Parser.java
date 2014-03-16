/*
 * Copyright (C) 2013,2014 Samuel Audet
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

package com.googlecode.javacpp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * To do:
 * - Inherit constructors from helper classes, if possible
 * - etc.
 *
 * @author Samuel Audet
 */
public class Parser {

    public static class Exception extends java.lang.Exception {
        public Exception(String message) { super(message); }
        public Exception(String message, Throwable cause) { super(message, cause); }
    }

    public static class Info {
        public Info() { }
        public Info(String ... cppNames) { this.cppNames = cppNames; }
        public Info(Info i) {
            cppNames = i.cppNames != null ? i.cppNames.clone() : null;
            javaNames = i.javaNames != null ? i.javaNames.clone() : null;
            annotations = i.annotations != null ? i.annotations.clone() : null;
            cppTypes = i.cppTypes != null ? i.cppTypes.clone() : null;
            valueTypes = i.valueTypes != null ? i.valueTypes.clone() : null;
            pointerTypes = i.pointerTypes != null ? i.pointerTypes.clone() : null;
            cast = i.cast;
            define = i.define;
            translate = i.translate;
            skip = i.skip;
            base = i.base;
            cppText = i.cppText;
            javaText = i.javaText;
        }

        String[] cppNames = null, javaNames = null, annotations = null,
                 cppTypes = null, valueTypes = null, pointerTypes = null;
        boolean cast = false, define = false, translate = false, skip = false;
        String base = null, cppText = null, javaText = null;

        public Info cppNames(String ... cppNames) { this.cppNames = cppNames; return this; }
        public Info javaNames(String ... javaNames) { this.javaNames = javaNames; return this; }
        public Info annotations(String ... annotations) { this.annotations = annotations; return this; }
        public Info cppTypes(String ... cppTypes) { this.cppTypes = cppTypes; return this; }
        public Info valueTypes(String ... valueTypes) { this.valueTypes = valueTypes; return this; }
        public Info pointerTypes(String ... pointerTypes) { this.pointerTypes = pointerTypes; return this; }
        public Info cast(boolean cast) { this.cast = cast; return this;  }
        public Info define(boolean define) { this.define = define; return this; }
        public Info translate(boolean translate) { this.translate = translate; return this; }
        public Info skip(boolean skip) { this.skip = skip; return this; }
        public Info base(String base) { this.base = base; return this; }
        public Info cppText(String cppText) { this.cppText = cppText; return this; }
        public Info javaText(String javaText) { this.javaText = javaText; return this; }
    }

    public static class InfoMap extends HashMap<String,LinkedList<Info>> {
        public InfoMap() { this.parent = defaults; }
        public InfoMap(InfoMap parent) { this.parent = parent; }

        InfoMap parent = null;
        static final String[] containers = { "std::deque", "std::list", "std::map", "std::queue", "std::set", "std::stack", "std::vector", "std::valarray" };
        static final String[] simpleTypes = { "signed", "unsigned", "char", "short", "int", "long", "bool", "float", "double" };
        static { Arrays.sort(simpleTypes); }
        static final InfoMap defaults = new InfoMap(null)
            .put(new Info("void").valueTypes("void").pointerTypes("Pointer"))
            .put(new Info("va_list", "FILE", "std::exception", "std::istream", "std::ostream", "std::iostream",
                    "std::ifstream", "std::ofstream", "std::fstream").cast(true).pointerTypes("Pointer"))

            .put(new Info("int8_t", "jbyte", "signed char")
                .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))
            .put(new Info("uint8_t", "char", "unsigned char").cast(true)
                .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))

            .put(new Info("int16_t", "jshort", "short", "signed short", "short int", "signed short int")
                .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))
            .put(new Info("uint16_t", "unsigned short", "unsigned short int").cast(true)
                .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))

            .put(new Info("int32_t", "jint", "int", "signed int", "signed")
                .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))
            .put(new Info("uint32_t", "unsigned int", "unsigned").cast(true)
                .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))

            .put(new Info("int64_t", "__int64", "jlong", "long long", "signed long long", "long long int", "signed long long int")
                .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))
            .put(new Info("uint64_t", "__uint64", "unsigned long long", "unsigned long long int").cast(true)
                .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))

            .put(new Info("long", "signed long", "long int", "signed long int")
                .valueTypes("long").pointerTypes("CLongPointer"))
            .put(new Info("unsigned long", "unsigned long int").cast(true)
                .valueTypes("long").pointerTypes("CLongPointer"))

            .put(new Info("size_t", "ptrdiff_t").cast(true).valueTypes("long").pointerTypes("SizeTPointer"))
            .put(new Info("float", "jfloat").valueTypes("float").pointerTypes("FloatPointer", "FloatBuffer", "float[]"))
            .put(new Info("double", "jdouble").valueTypes("double").pointerTypes("DoublePointer", "DoubleBuffer", "double[]"))
            .put(new Info("std::complex<float>").cast(true).pointerTypes("FloatPointer", "FloatBuffer", "float[]"))
            .put(new Info("std::complex<double>").cast(true).pointerTypes("DoublePointer", "DoubleBuffer", "double[]"))
            .put(new Info("bool", "jboolean").cast(true).valueTypes("boolean").pointerTypes("BoolPointer"))
            .put(new Info("wchar_t", "WCHAR").cast(true).valueTypes("char").pointerTypes("CharPointer"))
            .put(new Info("const char").valueTypes("byte").pointerTypes("@Cast(\"const char*\") BytePointer", "String"))
            .put(new Info("std::string").annotations("@StdString").valueTypes("BytePointer", "String"))
            .put(new Info("std::vector").annotations("@StdVector"))

            .put(new Info("operator->").javaNames("access"))
            .put(new Info("operator()").javaNames("apply"))
            .put(new Info("operator[]").javaNames("get"))
            .put(new Info("operator=").javaNames("put"))
            .put(new Info("operator+").javaNames("add"))
            .put(new Info("operator-").javaNames("subtract"))
            .put(new Info("operator*").javaNames("multiply"))
            .put(new Info("operator/").javaNames("divide"))
            .put(new Info("operator%").javaNames("mod"))
            .put(new Info("operator++").javaNames("increment"))
            .put(new Info("operator--").javaNames("decrement"))
            .put(new Info("operator==").javaNames("equals"))
            .put(new Info("operator!=").javaNames("notEquals"))
            .put(new Info("operator<").javaNames("lessThan"))
            .put(new Info("operator>").javaNames("greaterThan"))
            .put(new Info("operator<=").javaNames("lessThanEquals"))
            .put(new Info("operator>=").javaNames("greaterThanEquals"))
            .put(new Info("operator!").javaNames("not"))
            .put(new Info("operator&&").javaNames("and"))
            .put(new Info("operator||").javaNames("or"))
            .put(new Info("operator&").javaNames("and"))
            .put(new Info("operator|").javaNames("or"))
            .put(new Info("operator^").javaNames("xor"))
            .put(new Info("operator~").javaNames("not"))
            .put(new Info("operator<<").javaNames("shiftLeft"))
            .put(new Info("operator>>").javaNames("shiftRight"))
            .put(new Info("operator+=").javaNames("addPut"))
            .put(new Info("operator-=").javaNames("subtractPut"))
            .put(new Info("operator*=").javaNames("multiplyPut"))
            .put(new Info("operator/=").javaNames("dividePut"))
            .put(new Info("operator%=").javaNames("modPut"))
            .put(new Info("operator&=").javaNames("andPut"))
            .put(new Info("operator|=").javaNames("orPut"))
            .put(new Info("operator^=").javaNames("xorPut"))
            .put(new Info("operator<<=").javaNames("shiftLeftPut"))
            .put(new Info("operator>>=").javaNames("shiftRightPut"))

            .put(new Info("allocate").javaNames("_allocate"))
            .put(new Info("deallocate").javaNames("_deallocate"))
            .put(new Info("position").javaNames("_position"))
            .put(new Info("limit").javaNames("_limit"))
            .put(new Info("capacity").javaNames("_capacity"));

        static String normalize(String name, boolean unconst, boolean untemplate) {
            if (name == null || name.length() == 0) {
                return name;
            }
            boolean foundConst = false, simpleType = true;
            Token[] tokens = new Tokenizer(name).tokenize();
            int n = tokens.length;
            for (int i = 0; i < n; i++) {
                if (tokens[i].match(Token.CONST)) {
                    foundConst = true;
                    for (int j = i + 1; j < n; j++) {
                        tokens[j - 1] = tokens[j];
                    }
                    i--; n--;
                } else if (Arrays.binarySearch(simpleTypes, tokens[i].value) < 0) {
                    simpleType = false;
                    break;
                }
            }
            if (simpleType) {
                Arrays.sort(tokens, 0, n);
                name = (foundConst ? "const " : "") + tokens[0].value;
                for (int i = 1; i < n; i++) {
                    name += " " + tokens[i].value;
                }
            } else if (untemplate) {
                int count = 0, template = -1;
                for (int i = 0; i < n; i++) {
                    if (tokens[i].match('<')) {
                        if (count == 0) {
                            template = i;
                        }
                        count++;
                    } else if (tokens[i].match('>')) {
                        count--;
                        if (count == 0 && i + 1 != n) {
                            template = -1;
                        }
                    }
                }
                if (template >= 0) {
                    name = foundConst ? "const " : "";
                    for (int i = 0; i < template; i++) {
                        name += tokens[i].value;
                    }
                }
            }
            if (unconst && foundConst) {
                name = name.substring(name.indexOf("const") + 5);
            }
            return name.trim();
        }

        public LinkedList<Info> get(String cppName) {
            return get(cppName, true);
        }
        public LinkedList<Info> get(String cppName, boolean partial) {
            String key = normalize(cppName, false, false);
            LinkedList<Info> infoList = super.get(key);
            if (infoList == null) {
                key = normalize(cppName, true, false);
                infoList = super.get(key);
            }
            if (infoList == null && partial) {
                key = normalize(cppName, true, true);
                infoList = super.get(key);
            }
            if (infoList == null) {
                infoList = new LinkedList<Info>();
            }
            if (parent != null) {
                LinkedList<Info> l = parent.get(cppName, partial);
                if (l != null && l.size() > 0) {
                    infoList = new LinkedList<Info>(infoList);
                    infoList.addAll(l);
                }
            }
            return infoList;
        }

        public Info get(int index, String cppName) {
            return get(index, cppName, true);
        }
        public Info get(int index, String cppName, boolean partial) {
            LinkedList<Info> infoList = get(cppName, partial);
            return infoList.size() > 0 ? infoList.get(index) : null;
        }

        public Info getFirst(String cppName) {
            return getFirst(cppName, true);
        }
        public Info getFirst(String cppName, boolean partial) {
            LinkedList<Info> infoList = get(cppName, partial);
            return infoList.size() > 0 ? infoList.getFirst() : null;
        }

        public InfoMap put(int index, Info info) {
            for (String cppName : info.cppNames != null ? info.cppNames : new String[] { null }) {
                String[] keys = { normalize(cppName, false, false),
                                  normalize(cppName, false, true) };
                for (String key : keys) {
                    LinkedList<Info> infoList = super.get(key);
                    if (infoList == null) {
                        super.put(key, infoList = new LinkedList<Info>());
                    }
                    if (!infoList.contains(info)) {
                        switch (index) {
                            case -1: infoList.add(info); break;
                            case  0: infoList.addFirst(info); break;
                            default: infoList.add(index, info); break;
                        }
                    }
                }
            }
            return this;
        }

        public InfoMap put(Info info) {
            return put(-1, info);
        }

        public InfoMap putFirst(Info info) {
            return put(0, info);
        }
    }

    public static interface InfoMapper {
        void map(InfoMap infoMap);
    }

    static class Token implements Comparable<Token> {
        Token() { }
        Token(int type, String value) { this.type = type; this.value = value; }
        Token(Token t) {
            file = t.file;
            lineNumber = t.lineNumber;
            type = t.type;
            spacing = t.spacing;
            value = t.value;
        }

        static final int
                INTEGER    = 1,
                FLOAT      = 2,
                STRING     = 3,
                COMMENT    = 4,
                IDENTIFIER = 5,
                SYMBOL     = 6;

        static final Token
                EOF       = new Token(),
                CONST     = new Token(IDENTIFIER, "const"),
                DEFINE    = new Token(IDENTIFIER, "define"),
                IF        = new Token(IDENTIFIER, "if"),
                IFDEF     = new Token(IDENTIFIER, "ifdef"),
                IFNDEF    = new Token(IDENTIFIER, "ifndef"),
                ELIF      = new Token(IDENTIFIER, "elif"),
                ELSE      = new Token(IDENTIFIER, "else"),
                ENDIF     = new Token(IDENTIFIER, "endif"),
                ENUM      = new Token(IDENTIFIER, "enum"),
                EXPLICIT  = new Token(IDENTIFIER, "explicit"),
                EXTERN    = new Token(IDENTIFIER, "extern"),
                FRIEND    = new Token(IDENTIFIER, "friend"),
                INLINE    = new Token(IDENTIFIER, "inline"),
                STATIC    = new Token(IDENTIFIER, "static"),
                CLASS     = new Token(IDENTIFIER, "class"),
                STRUCT    = new Token(IDENTIFIER, "struct"),
                UNION     = new Token(IDENTIFIER, "union"),
                TEMPLATE  = new Token(IDENTIFIER, "template"),
                TYPEDEF   = new Token(IDENTIFIER, "typedef"),
                TYPENAME  = new Token(IDENTIFIER, "typename"),
                USING     = new Token(IDENTIFIER, "using"),
                NAMESPACE = new Token(IDENTIFIER, "namespace"),
                OPERATOR  = new Token(IDENTIFIER, "operator"),
                PRIVATE   = new Token(IDENTIFIER, "private"),
                PROTECTED = new Token(IDENTIFIER, "protected"),
                PUBLIC    = new Token(IDENTIFIER, "public"),
                VIRTUAL   = new Token(IDENTIFIER, "virtual");

        File file = null;
        int lineNumber = 0, type = -1;
        String spacing = "", value = "";

        boolean match(Object ... tokens) {
            boolean found = false;
            for (Object t : tokens) {
                found = found || equals(t);
            }
            return found;
        }

        Token expect(Object ... tokens) throws Exception {
            if (!match(tokens)) {
                throw new Exception(file + ":" + lineNumber + ": Unexpected token '" + toString() + "'");
            }
            return this;
        }

        boolean isEmpty() {
            return type == -1 && spacing.isEmpty();
        }

        @Override public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (obj.getClass() == Integer.class) {
                return type == (Integer)obj;
            } else if (obj.getClass() == Character.class) {
                return type == (Character)obj;
            } else if (obj.getClass() == String.class) {
                return obj.equals(value);
            } else if (obj.getClass() == getClass()) {
                Token other = (Token)obj;
                return type == other.type && ((value == null && other.value == null) ||
                                               value != null && value.equals(other.value));
            } else {
                return false;
            }
        }

        @Override public int hashCode() {
            return type;
        }

        @Override public String toString() {
            return value != null && value.length() > 0 ? value : String.valueOf((char)type);
        }

        public int compareTo(Token t) {
            return toString().compareTo(t.toString());
        }
    }

    static class Tokenizer implements Closeable {
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

        public void close() throws IOException {
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
                boolean large = false, unsigned = false, hex = false;
                while ((c = readChar()) != -1 && (Character.isDigit(c) || c == '.' || c == '-' || c == '+' ||
                       (c >= 'a' && c <= 'f') || c == 'l' || c == 'u' || c == 'x' ||
                       (c >= 'A' && c <= 'F') || c == 'L' || c == 'U' || c == 'X')) {
                    switch (c) {
                        case '.': token.type = Token.FLOAT;  break;
                        case 'l': case 'L': large    = true; break;
                        case 'u': case 'U': unsigned = true; break;
                        case 'x': case 'X': hex      = true; break;
                    }
                    if (c != 'l' && c != 'L' && c != 'u' && c != 'U') {
                        buffer.append((char)c);
                    }
                    prevc = c;
                }
                if (!hex && (prevc == 'f' || prevc == 'F')) {
                    token.type = Token.FLOAT;
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

    static class TokenIndexer {
        TokenIndexer(InfoMap infoMap, Token[] array) {
            this.infoMap = infoMap;
            this.array = array;
        }

        boolean raw = false;
        InfoMap infoMap = null;
        Token[] array = null;
        int index = 0;

        void filter(int index) {
            if (index + 1 < array.length && array[index].match('#') &&
                    array[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
                ArrayList<Token> tokens = new ArrayList<Token>();
                for (int i = 0; i < index; i++) {
                    tokens.add(array[i]);
                }
                int count = 0;
                Info info = null;
                boolean define = true, defined = false;
                while (index < array.length) {
                    Token keyword = null;
                    if (array[index].match('#')) {
                        if (count == 0 && array[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
                            count++;
                            keyword = array[index + 1];
                        } else if (count == 1 && array[index + 1].match(Token.ELIF, Token.ELSE, Token.ENDIF)) {
                            keyword = array[index + 1];
                        }
                    }
                    if (keyword != null) {
                        tokens.add(array[index++]);
                        tokens.add(array[index++]);
                        if (keyword.match(Token.IF, Token.IFDEF, Token.IFNDEF, Token.ELIF)) {
                            String value = "";
                            while (index < array.length) {
                                if (array[index].spacing.indexOf('\n') >= 0) {
                                    break;
                                }
                                value += array[index].spacing + array[index];
                                tokens.add(array[index++]);
                            }
                            define = info == null || !defined;
                            info = infoMap.getFirst(value);
                            if (info != null) {
                                define = keyword.match(Token.IFNDEF) ? !info.define : info.define;
                            } else try {
                                define = Integer.parseInt(value.trim()) != 0;
                            } catch (NumberFormatException e) {
                                /* default define */
                            }
                        } else if (keyword.match(Token.ELSE)) {
                            define = info == null || !define;
                        } else if (keyword.match(Token.ENDIF)) {
                            count--;
                            if (count == 0) {
                                break;
                            }
                        }
                    } else if (define) {
                        tokens.add(array[index++]);
                    } else {
                        index++;
                    }
                    defined = define || defined;
                }
                while (index < array.length) {
                    tokens.add(array[index++]);
                }
                array = tokens.toArray(new Token[tokens.size()]);
            }
        }

        void expand(int index) {
            if (index < array.length && infoMap.containsKey(array[index].value)) {
                int startIndex = index;
                Info info = infoMap.getFirst(array[index].value);
                if (info != null && info.cppText != null) {
                    try {
                        Tokenizer tokenizer = new Tokenizer(info.cppText);
                        if (!tokenizer.nextToken().match('#')
                                || !tokenizer.nextToken().match(Token.DEFINE)
                                || !tokenizer.nextToken().match(info.cppNames[0])
                                || !tokenizer.nextToken().match('(')) {
                            return;
                        }
                        ArrayList<String> params = new ArrayList<String>();
                        Token token;
                        while (!(token = tokenizer.nextToken()).isEmpty()) {
                            if (token.match(Token.IDENTIFIER)) {
                                params.add(token.value);
                            } else if (token.match(')')) {
                                break;
                            }
                        }
                        if (params.size() > 0 && (index + 1 >= array.length
                                || !array[index + 1].match('('))) {
                            return;
                        }
                        ArrayList<Token> tokens = new ArrayList<Token>();
                        for (int i = 0; i < index; i++) {
                            tokens.add(array[i]);
                        }
                        ArrayList<Token>[] args = new ArrayList[params.size()];
                        int count = 0, count2 = 0;
                        for (index += 2; index < array.length; index++) {
                            token = array[index];
                            if (count2 == 0 && token.match(')')) {
                                break;
                            } else if (count2 == 0 && token.match(',')) {
                                count++;
                                continue;
                            } else if (token.match('(','[','{')) {
                                count2++;
                            } else if (token.match(')',']','}')) {
                                count2--;
                            }
                            if (args[count] == null) {
                                args[count] = new ArrayList<Token>();
                            }
                            args[count].add(token);
                        }
                        while (!(token = tokenizer.nextToken()).isEmpty()) {
                            boolean foundArg = false;
                            for (int i = 0; i < params.size(); i++) {
                                if (params.get(i).equals(token.value)) {
                                    if (tokens.size() == startIndex) {
                                        args[i].get(0).spacing = array[startIndex].spacing;
                                    }
                                    tokens.addAll(args[i]);
                                    foundArg = true;
                                    break;
                                }
                            }
                            if (!foundArg) {
                                if (tokens.size() == startIndex) {
                                    token.spacing = array[startIndex].spacing;
                                }
                                tokens.add(token);
                            }
                        }
                        for (index += 1; index < array.length; index++) {
                            tokens.add(array[index]);
                        }
                        array = tokens.toArray(new Token[tokens.size()]);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        int preprocess(int index, int count) {
            while (index < array.length) {
                filter(index);
                expand(index);
                if (!array[index].match(Token.COMMENT) && --count < 0) {
                    break;
                }
                index++;
            }
            filter(index);
            expand(index);
            return index;
        }

        Token get() {
            return get(0);
        }
        Token get(int i) {
            int k = raw ? index + i : preprocess(index, i);
            return k < array.length ? array[k] : Token.EOF;
        }
        Token next() {
            index = raw ? index + 1 : preprocess(index, 1);
            return index < array.length ? array[index] : Token.EOF;
        }
    }

    static class Context {
        Context() {
            usingList = new LinkedList<String>();
        }
        Context(Context c) {
            namespace = c.namespace;
            group = c.group;
            inaccessible = c.inaccessible;
            variables = c.variables;
            templateMap = c.templateMap;
            usingList = c.usingList;
        }

        String namespace = null;
        Type group = null;
        boolean inaccessible = false;
        Declarator[] variables = null;
        TemplateMap templateMap = null;
        LinkedList<String> usingList = null;

        String[] qualify(String cppName) {
            if (cppName == null || cppName.length() == 0) {
                return new String[0];
            }
            ArrayList<String> names = new ArrayList<String>();
            String ns = namespace != null ? namespace : "";
            while (ns != null) {
                String name = ns.length() > 0 ? ns + "::" + cppName : cppName;
                TemplateMap map = templateMap;
                while (map != null) {
                    if (name.equals(map.getName())) {
                        String args = "";
                        char separator = '<';
                        for (String s : map.values()) {
                            args += separator + s;
                            separator = ',';
                        }
                        args += args.endsWith(">") ? " >" : ">";
                        names.add(name + args);
                        break;
                    }
                    map = map.parent;
                }
                names.add(name);

                ns = InfoMap.normalize(ns, false, true);
                int i = ns.lastIndexOf("::");
                ns = i >= 0 ? ns.substring(0, i) : ns.length() > 0 ? "" : null;
            }
            for (String s : usingList) {
                String prefix = InfoMap.normalize(cppName, false, true);
                int i = s.lastIndexOf("::") + 2;
                ns = s.substring(0, i);
                String suffix = s.substring(i);
                if (suffix.length() == 0 || prefix.equals(suffix)) {
                    names.add(ns + cppName);
                }
            }
            return names.toArray(new String[names.size()]);
        }

        String shorten(String javaName) {
            if (group != null) {
                int lastDot = 0;
                String s1 = javaName, s2 = group.javaName + '.';
                for (int i = 0; i < s1.length() && i < s2.length(); i++) {
                    if (s1.charAt(i) != s2.charAt(i)) {
                        break;
                    } else if (s1.charAt(i) == '.') {
                        lastDot = i;
                    }
                }
                if (lastDot > 0) {
                    javaName = javaName.substring(lastDot + 1);
                }
            }
            return javaName;
        }
    }


    public Parser(Logger logger, Properties properties) {
        this.logger = logger;
        this.properties = properties;
    }
    Parser(Parser p, String text) {
        this.logger = p.logger;
        this.properties = p.properties;
        this.infoMap = p.infoMap;
        this.tokens = new TokenIndexer(infoMap, new Tokenizer(text).tokenize());
    }

    final Logger logger;
    final Properties properties;
    InfoMap infoMap = null, leafInfoMap = null;
    TokenIndexer tokens = null;

    String translate(String text) {
        int namespace = text.lastIndexOf("::");
        if (namespace >= 0) {
            Info info2 = infoMap.getFirst(text.substring(0, namespace));
            text = text.substring(namespace + 2);
            if (info2.pointerTypes != null) {
                text = info2.pointerTypes[0] + "." + text;
            }
        }
        return text;
    }


    void containers(Context context, DeclarationList declList) throws Exception {
        for (String containerName : InfoMap.containers) {
            LinkedList<Info> infoList = leafInfoMap.get(containerName);
            for (Info info : infoList) {
                Declaration decl = new Declaration();
                if (info == null || info.skip || !info.define) {
                    continue;
                }
                int dim = 1;
                boolean resizable = true;
                Type containerType = new Parser(this, info.cppNames[0]).type(context),
                        indexType, valueType, firstType = null, secondType = null;
                if (containerType.arguments == null || containerType.arguments.length == 0) {
                    continue;
                } else if (containerType.arguments.length > 1) {
                    resizable = false;
                    indexType = containerType.arguments[0];
                    valueType = containerType.arguments[1];
                } else {
                    indexType = new Type();
                    indexType.annotations = "@Cast(\"size_t\") ";
                    indexType.cppName = "size_t";
                    indexType.javaName = "long";
                    valueType = containerType.arguments[0];
                }
                while (valueType.cppName.startsWith(containerName)) {
                    dim++;
                    valueType = valueType.arguments[0];
                }
                if (valueType.cppName.startsWith("std::pair")) {
                    firstType = valueType.arguments[0];
                    secondType = valueType.arguments[1];
                    if (firstType.annotations == null || firstType.annotations.length() == 0) {
                        firstType.annotations = "@ByRef ";
                    }
                    if (secondType.annotations == null || secondType.annotations.length() == 0) {
                        secondType.annotations = "@ByRef ";
                    }
                }
                if (valueType.annotations == null || valueType.annotations.length() == 0) {
                    valueType.annotations = "@ByRef ";
                }
                String arrayBrackets = "";
                for (int i = 0; i < dim - 1; i++) {
                    arrayBrackets += "[]";
                }
                decl.text += "\n"
                        + "@Name(\"" + containerType.cppName + "\") public static class " + containerType.javaName + " extends Pointer {\n"
                        + "    static { Loader.load(); }\n"
                        + "    public " + containerType.javaName + "(Pointer p) { super(p); }\n" + (!resizable || firstType != null || secondType != null ? ""
                        : "    public " + containerType.javaName + "(" + valueType.javaName + arrayBrackets + " ... array) { this(array.length); put(array); }\n")
                        + "    public " + containerType.javaName + "()       { allocate();  }\n" + (!resizable ? ""
                        : "    public " + containerType.javaName + "(long n) { allocate(n); }\n")
                        + "    private native void allocate();\n"                                + (!resizable ? "\n"
                        : "    private native void allocate(@Cast(\"size_t\") long n);\n\n");

                for (int i = 0; i < dim; i++) {
                    String indexAnnotation = i > 0 ? ("@Index" + (i > 1 ? "(" + i + ") " : " " )) : "";
                    String indices = "";
                    String separator = "";
                    for (int j = 0; j < i; j++) {
                        indices += separator + indexType.annotations + indexType.javaName + " " + (char)('i' + j);
                        separator = ", ";
                    }

                    decl.text += "    public native " + indexAnnotation + "long size(" + indices + ");\n"  + (!resizable ? ""
                               : "    public native " + indexAnnotation + "void resize(" + indices + separator + "@Cast(\"size_t\") long n);\n");
                }

                String params = "";
                String separator = "";
                for (int i = 0; i < dim; i++) {
                    params += separator + indexType.annotations + indexType.javaName + " " + (char)('i' + i);
                    separator = ", ";
                }

                if (firstType != null && secondType != null) {
                    String indexAnnotation = "@Index" + (dim > 1 ? "(" + dim + ") " : " ");
                    decl.text += "\n"
                              +  "    " + indexAnnotation + "public native " + firstType.annotations + firstType.javaName + " first(" + params + ");"
                              +  " public native " + containerType.javaName + " first(" + params + separator + firstType.javaName + " first);\n"
                              +  "    " + indexAnnotation + "public native " + secondType.annotations + secondType.javaName + " second(" + params + "); "
                              +  " public native " + containerType.javaName + " second(" + params + separator + secondType.javaName + " second);\n";
                } else {
                    decl.text += "\n"
                              +  "    @Index public native " + valueType.annotations + valueType.javaName + " get(" + params + ");\n"
                              +  "    public native " + containerType.javaName + " put(" + params + separator + valueType.javaName + " value);\n";
                }

                if (resizable && firstType == null && secondType == null) {
                    decl.text += "\n"
                              +  "    public " + containerType.javaName + " put(" + valueType.javaName + arrayBrackets + " ... array) {\n";
                    String indent = "        ";
                    String indices = "";
                    String args = "";
                    separator = "";
                    for (int i = 0; i < dim; i++) {
                        char c = (char)('i' + i);
                        decl.text +=
                                indent + "if (size(" + args + ") != array" + indices + ".length) { resize(" + args + separator + "array" + indices + ".length); }\n" +
                                indent + "for (int " + c + " = 0; " + c + " < array" + indices + ".length; " + c + "++) {\n";
                        indent += "    ";
                        indices += "[" + c + "]";
                        args += separator + c;
                        separator = ", ";
                    }
                    decl.text += indent + "put(" + args + separator + "array" + indices + ");\n";
                    for (int i = 0; i < dim; i++) {
                        indent = indent.substring(4);
                        decl.text += indent + "}\n";
                    }
                    decl.text += "        return this;\n"
                              +  "    }\n";
                }
                decl.text += "}\n";
                declList.add(decl);
            }
        }
    }


    static class TemplateMap extends LinkedHashMap<String,String> {
        TemplateMap(TemplateMap parent) {
            this.parent = parent;
        }
        Type type = null;
        Declarator declarator = null;
        TemplateMap parent = null;

        String getName() {
            return type != null ? type.cppName : declarator != null ? declarator.cppName : null;
        }

        boolean full() {
            for (String s : values()) {
                if (s == null) {
                    return false;
                }
            }
            return true;
        }

        String get(String key) {
            String value = super.get(key);
            if (value == null && parent != null) {
                return parent.get(key);
            } else {
                return value;
            }
        }
    }

    TemplateMap template(Context context) throws Exception {
        if (!tokens.get().match(Token.TEMPLATE)) {
            return null;
        }
        TemplateMap map = new TemplateMap(context.templateMap);

        tokens.next().expect('<');
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.IDENTIFIER)) {
                String key = tokens.next().expect(Token.IDENTIFIER).value;
                map.put(key, map.get(key));
                token = tokens.next();
            }
            if (!token.match(',', '>')) {
                // ignore default argument
                int count = 0;
                for (token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                    if (count == 0 && token.match(',', '>')) {
                        break;
                    } else if (token.match('<')) {
                        count++;
                    } else if (token.match('>')) {
                        count--;
                    }
                }
            }
            if (token.expect(',', '>').match('>')) {
                tokens.next();
                break;
            }
        }
        return map;
    }

    Type[] templateArguments(Context context) throws Exception {
        if (!tokens.get().match('<')) {
            return null;
        }
        ArrayList<Type> arguments = new ArrayList<Type>();
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
            Type type = type(context);
            arguments.add(type);
            token = tokens.get();
            if (!token.match(',', '>')) {
                // may not actually be a type
                int count = 0;
                for (token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                    if (count == 0 && token.match(',', '>')) {
                        break;
                    } else if (token.match('<')) {
                        count++;
                    } else if (token.match('>')) {
                        count--;
                    }
                    type.cppName += token;
                }
                if (type.cppName.endsWith("*")) {
                    type.javaName = "PointerPointer";
                    type.annotations += "@Cast(\"" + type.cppName + "*\") ";
                }
            }
            if (token.expect(',', '>').match('>')) {
                break;
            }
        }
        return arguments.toArray(new Type[arguments.size()]);
    }


    static class Type {
        Type() { }
        Type(String name) { cppName = javaName = name; }

        boolean anonymous = false, constValue = false, constructor = false,
                destructor = false, operator = false, simple = false, staticMember = false;
        String annotations = "", cppName = "", javaName = "";
        Type[] arguments = null;
        Attribute[] attributes = null;

        @Override public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (obj.getClass() == getClass()) {
                Type other = (Type)obj;
                return cppName.equals(other.cppName) && javaName.equals(other.javaName);
            } else {
                return false;
            }
        }
    }

    Type type(Context context) throws Exception {
        Type type = new Type();
        if (tokens.get().match(Token.OPERATOR)) {
            type.operator = true;
            tokens.next();
        }
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match("::")) {
                type.cppName += token;
            } else if (token.match('<')) {
                type.arguments = templateArguments(context);
                char separator = '<';
                for (Type t : type.arguments) {
                    type.cppName += separator;
                    Info info = infoMap.getFirst(t.cppName);
                    type.cppName += info != null && info.cppTypes != null ? info.cppTypes[0] : t.cppName;
                    separator = ',';
                }
                type.cppName += type.cppName.endsWith(">") ? " >" : ">";
            } else if (token.match(Token.CONST)) {
                type.constValue = true;
            } else if (token.match('~')) {
                type.destructor = true;
            } else if (token.match(Token.STATIC)) {
                type.staticMember = true;
            } else if (token.match(Token.OPERATOR)) {
                break;
            } else if (token.match(Token.ENUM, Token.EXPLICIT, Token.EXTERN, Token.INLINE, Token.CLASS,
                    Token.STRUCT, Token.UNION, Token.TYPEDEF, Token.TYPENAME, Token.USING, Token.VIRTUAL)) {
                continue;
            } else if (token.match((Object[])InfoMap.simpleTypes)) {
                type.cppName += token.value + " ";
                type.simple = true;
            } else if (token.match(Token.IDENTIFIER)) {
                int backIndex = tokens.index;
                Attribute attr = attribute();
                if (attr != null && attr.annotation) {
                    tokens.index--;
                    type.annotations += attr.javaName;
                    attributes.add(attr);
                } else {
                    tokens.index = backIndex;
                    if (type.cppName.length() == 0 || type.cppName.endsWith("::")) {
                        type.cppName += token.value;
                    } else {
                        Info info = infoMap.getFirst(tokens.get(1).value);
                        if ((info != null && info.annotations != null) ||
                                !tokens.get(1).match('*', '&', Token.IDENTIFIER, Token.CONST)) {
                            // we probably reached a variable or function name identifier
                            break;
                        }
                    }
                }
            } else {
                if (token.match('}')) {
                    type.anonymous = true;
                    tokens.next();
                }
                break;
            }
        }
        if (attributes.size() > 0) {
            type.attributes = attributes.toArray(new Attribute[attributes.size()]);
        }
        type.cppName = type.cppName.trim();
        if ("...".equals(tokens.get().value)) {
            tokens.next();
            return null;
        } else if (type.operator) {
            for (Token token = tokens.get(); !token.match(Token.EOF, '('); token = tokens.next()) {
                type.cppName += token;
            }
        }
        boolean pointer = type.cppName.endsWith("*");
        boolean reference = type.cppName.endsWith("&");
        if (pointer || reference) {
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (context.templateMap != null && context.templateMap.get(type.cppName) != null) {
            type.cppName = context.templateMap.get(type.cppName);
        }
        int namespace = type.cppName.lastIndexOf("::");
        int template = type.cppName.indexOf('<');
        type.javaName = namespace >= 0 && template < 0 ? type.cppName.substring(namespace + 2) : type.cppName;
        Info info = null;
        for (String name : context.qualify(type.cppName)) {
            if ((info = infoMap.getFirst(name, false)) != null) {
                type.cppName = name;
                break;
            } else if (infoMap.getFirst(name) != null) {
                type.cppName = name;
            }
        }
        boolean valueType = false;
        if (info != null) {
            if (!pointer && !reference && info.valueTypes != null && info.valueTypes.length > 0) {
                type.javaName = info.valueTypes[0];
                valueType = true;
            } else if (info.pointerTypes != null && info.pointerTypes.length > 0) {
                type.javaName = info.pointerTypes[0];
            }
        }
        if (type.operator) {
            if (type.constValue) {
                type.annotations += "@Const ";
            }
            if (!valueType && !pointer && !reference) {
                type.annotations += "@ByVal ";
            } else if (!valueType && !pointer && reference) {
                type.annotations += "@ByRef ";
            }
            type.annotations += "@Name(\"operator " + (type.constValue ? "const " : "")
                    + type.cppName + (pointer ? "*" : reference ? "&" : "") + "\") ";
        }
        if (info != null && info.annotations != null) {
            for (String s : info.annotations) {
                type.annotations += s + " ";
            }
        }
        if (context.group != null && type.javaName.length() > 0) {
            if (type.cppName.equals(context.group.cppName)) {
                type.constructor = !type.destructor && !type.operator && tokens.get().match('(');
            }
            type.javaName = context.shorten(type.javaName);
        }
        return type;
    }

    static class Declarator {
        Type type = null;
        Parameters parameters = null;
        Declaration definition = null;
        int infoNumber = 0, indices = 0;
        boolean constPointer = false;
        String cppName = "", javaName = "", signature = "";
    }

    Declarator declarator(Context context, String defaultName, int infoNumber, boolean useDefaults,
            int varNumber, boolean arrayAsPointer, boolean pointerAsArray) throws Exception {
        boolean typedef = tokens.get().match(Token.TYPEDEF);
        boolean using = tokens.get().match(Token.USING);
        Declarator dcl = new Declarator();
        Type type = type(context);
        if (type == null) {
            return null;
        }
        Attribute attr = null;
        if (type.attributes != null) {
            for (Attribute a : type.attributes) {
                if (a.arguments.length() > 0 && Character.isJavaIdentifierStart(a.arguments.charAt(0))) {
                    attr = a;
                    for (char c : a.arguments.toCharArray()) {
                        if (!Character.isJavaIdentifierPart(c)) {
                            attr = null;
                            break;
                        }
                    }
                }
                if (attr != null) {
                    break;
                }
            }
        }

        int count = 0;
        for (Token token = tokens.get(); varNumber > 0 && !token.match(Token.EOF); token = tokens.next()) {
            if (token.match('(','[','{')) {
                count++;
            } else if (token.match(')',']','}')) {
                count--;
            } else if (count > 0) {
                continue;
            } else if (token.match(',')) {
                varNumber--;
            } else if (token.match(';')) {
                tokens.next();
                return null;
            }
        }

        String cast = type.cppName;
        int indirections = 0;
        boolean reference = false;
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match('*')) {
                indirections++;
            } else if (token.match('&')) {
                reference = true;
            } else if (token.match(Token.CONST)) {
                dcl.constPointer = true;
            } else {
                break;
            }
            cast += token;
        }
        if (indirections == 0 && reference) {
            cast = cast.replace('&', '*');
        }

        int dims[] = new int[256];
        int indirections2 = 0;
        dcl.cppName = "";
        Info groupInfo = null;
        Declaration definition = new Declaration();
        boolean operator = false;
        if (tokens.get().match('(')) {
            // probably a function pointer declaration
            while (tokens.get().match('(')) {
                tokens.next();
            }
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                if (token.match(Token.IDENTIFIER, "::")) {
                    dcl.cppName += token;
                } else if (token.match('*')) {
                    indirections2++;
                    if (dcl.cppName.endsWith("::")) {
                        dcl.cppName = dcl.cppName.substring(0, dcl.cppName.length() - 2);
                        for (String name : context.qualify(dcl.cppName)) {
                            if ((groupInfo = infoMap.getFirst(name, false)) != null) {
                                dcl.cppName = name;
                                break;
                            } else if (infoMap.getFirst(name) != null) {
                                dcl.cppName = name;
                            }
                        }
                        definition.text += "@Namespace(\"" + dcl.cppName + "\") ";
                    } else if (dcl.cppName.length() > 0) {
                        definition.text += "@Convention(\"" + dcl.cppName + "\") ";
                    }
                    dcl.cppName = "";
                } else if (token.match('[')) {
                    Token n = tokens.get(1);
                    dims[dcl.indices++] = n.match(Token.INTEGER) ? Integer.parseInt(n.value) : -1;
                } else if (token.match(')')) {
                    tokens.next();
                    break;
                }
            }
            while (tokens.get().match(')')) {
                tokens.next();
            }
        } else if (tokens.get().match(Token.IDENTIFIER)) {
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                if (token.match("::")) {
                    dcl.cppName += token;
                } else if (token.match(Token.OPERATOR)) {
                    operator = true;
                    dcl.cppName += "operator" + tokens.next();
                    for (token = tokens.next(); !token.match(Token.EOF, '('); token = tokens.next()) {
                        dcl.cppName += token;
                    }
                    break;
                } else if (token.match('<')) {
                    // template argument
                    dcl.cppName += token;
                    int count2 = 0;
                    for (token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                        dcl.cppName += token;
                        if (count2 == 0 && token.match(',', '>')) {
                            break;
                        } else if (token.match('<')) {
                            count2++;
                        } else if (token.match('>')) {
                            count2--;
                        }
                    }
                } else if (token.match(Token.IDENTIFIER) &&
                        (dcl.cppName.length() == 0 || dcl.cppName.endsWith("::"))) {
                    dcl.cppName += token;
                } else {
                    break;
                }
            }
        }
        if (dcl.cppName.length() == 0) {
            dcl.cppName = defaultName;
        }

        boolean bracket = false;
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (!bracket && token.match('[')) {
                bracket = true;
                Token n = tokens.get(1);
                dims[dcl.indices++] = n.match(Token.INTEGER) ? Integer.parseInt(n.value) : -1;
            } else if (!bracket) {
                break;
            } else if (bracket && token.match(']')) {
                bracket = false;
            }
        }
        while (dcl.indices > 0 && indirections2 > 0) {
            // treat complex combinations of arrays and pointers as multidimensional arrays
            dims[dcl.indices++] = -1;
            indirections2--;
        }
        if (arrayAsPointer && dcl.indices > 0) {
            // treat array as an additional indirection
            indirections++;
            String dimCast = "";
            for (int i = 1; i < dcl.indices; i++) {
                if (dims[i] > 0) {
                    dimCast += "[" + dims[i] + "]";
                }
            }
            //dcl.indices = 0;
            cast += dimCast.length() > 0 ? "(*)" + dimCast : "*";
        }
        if (pointerAsArray && indirections > (type.anonymous ? 0 : 1)) {
            // treat second indirection as an array, unless anonymous
            dims[dcl.indices++] = -1;
            indirections--;
            cast = cast.substring(0, cast.length() - 1);
        }

        if (tokens.get().match(':')) {
            // ignore bitfields
            type.annotations += "@NoOffset ";
            tokens.next().expect(Token.INTEGER);
            tokens.next().expect(',', ';');
        }

        int infoLength = 1;
        boolean valueType = false, needCast = arrayAsPointer && dcl.indices > 1, implicitConst = false;
        String prefix = type.constValue && indirections < 2 && !reference ? "const " : "";
        Info info = infoMap.getFirst(prefix + type.cppName, false);
        if (!typedef && (info == null || (info.cppTypes != null && info.cppTypes.length > 0))) {
            // substitute template types that have no info with appropriate adapter annotation
            Type type2 = type;
            if (info != null) {
                type2 = new Parser(this, info.cppTypes[0]).type(context);
            }
            LinkedList<Info> infoList = infoMap.get(type2.cppName);
            for (Info info2 : infoList) {
                if (type2.arguments != null && info2.annotations != null) {
                    indirections = 1;
                    reference = false;
                    type = type2.arguments[0];
                    cast = type.cppName + "*";
                    for (String s : info2.annotations) {
                        type.annotations += s + " ";
                    }
                    info = infoMap.getFirst(type.cppName, false);
                    break;
                }
            }
        }
        if (!using && info != null) {
            valueType = info.valueTypes != null && ((type.constValue && reference) ||
                    (indirections == 0 && !reference) || info.pointerTypes == null);
            implicitConst = info.cppNames[0].startsWith("const ");
            infoLength = valueType ? info.valueTypes.length :
                    info.pointerTypes != null ? info.pointerTypes.length : 1;
            dcl.infoNumber = infoNumber < 0 ? 0 : infoNumber % infoLength;
            type.javaName = valueType ? info.valueTypes[dcl.infoNumber] :
                    info.pointerTypes != null ? info.pointerTypes[dcl.infoNumber] : type.javaName;
            type.javaName = context.shorten(type.javaName);
            needCast |= info.cast && !type.cppName.equals(type.javaName);
        }

        if (!valueType) {
            if (indirections == 0 && !reference) {
                type.annotations += "@ByVal ";
            } else if (indirections == 0 && reference) {
                type.annotations += "@ByRef ";
            } else if (indirections == 1 && reference) {
                type.annotations += "@ByPtrRef ";
            } else if (indirections == 2 && !reference && infoNumber >= 0) {
                type.annotations += "@ByPtrPtr ";
                needCast |= type.cppName.equals("void");
            } else if (indirections >= 2) {
                dcl.infoNumber += infoLength;
                needCast = true;
                type.javaName = "PointerPointer";
                if (reference) {
                    type.annotations += "@ByRef ";
                }
            }

            if (!needCast && type.constValue && !implicitConst) {
                type.annotations = "@Const " + type.annotations;
            }
        }
        if (needCast) {
            if (type.constValue) {
                cast = "const " + cast;
            }
            if (!valueType && indirections == 0 && !reference) {
                type.annotations += "@Cast(\"" + cast + "*\") ";
            } else {
                type.annotations = "@Cast(\"" + cast + "\") " + type.annotations;
            }
        }

        dcl.javaName = attr != null ? attr.arguments : dcl.cppName;
        for (String name : context.qualify(dcl.cppName)) {
            if ((info = infoMap.getFirst(name, false)) != null) {
                dcl.cppName = name;
                break;
            } else if (infoMap.getFirst(name) != null) {
                dcl.cppName = name;
            }
        }
        if (attr == null && defaultName == null && info != null && info.javaNames != null && info.javaNames.length > 0
                && (operator || !info.cppNames[0].contains("<") || (context.templateMap != null && context.templateMap.type == null))) {
            dcl.javaName = info.javaNames[0];
        }
        if (dcl.cppName != null) {
            String localName = dcl.cppName;
            if (localName.startsWith(context.namespace + "::")) {
                localName = dcl.cppName.substring(context.namespace.length() + 2);
            }
            if (!localName.equals(dcl.javaName)) {
                type.annotations += "@Name(\"" + localName + "\") ";
            }
        }

        dcl.signature = dcl.javaName;
        dcl.parameters = parameters(context, infoNumber, useDefaults);
        if (dcl.parameters != null) {
            dcl.infoNumber = Math.max(dcl.infoNumber, dcl.parameters.infoNumber);
            if (indirections2 == 0) {
                dcl.signature += dcl.parameters.signature;
            } else {
                String functionType = Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1);
                if (typedef) {
                    functionType = dcl.javaName;
                } else if (dcl.parameters.signature.length() > 0) {
                    functionType += dcl.parameters.signature;
                } else if (!type.javaName.equals("void")) {
                    functionType = type.javaName + "_" + functionType;
                }
                definition.text += (tokens.get().match(Token.CONST) ? "@Const " : "") +
                        "public static class " + functionType + " extends FunctionPointer {\n" +
                        "    static { Loader.load(); }\n" +
                        "    public    " + functionType + "(Pointer p) { super(p); }\n" +
                    (groupInfo != null ? "" :
                        "    protected " + functionType + "() { allocate(); }\n" +
                        "    private native void allocate();\n") +
                        "    public native " + type.annotations + type.javaName + " call" +
                    (groupInfo != null ? "(" + groupInfo.pointerTypes[0] + " o" + (dcl.parameters.list.charAt(1) == ')' ?
                            ")" : ", " + dcl.parameters.list.substring(1)) : dcl.parameters.list) + ";\n" +
                        "}\n";
                definition.signature = functionType;
                definition.declarator = new Declarator();
                definition.declarator.parameters = dcl.parameters;
                dcl.definition = definition;
                dcl.parameters = null;
                type.annotations = "";
                type.javaName = functionType;
            }
        }
        dcl.type = type;
        return dcl;
    }

    String commentBefore() throws Exception {
        String comment = "";
        tokens.raw = true;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        boolean closeComment = false;
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            String s = token.value;
            if (s.startsWith("/**") || s.startsWith("/*!") || s.startsWith("///") || s.startsWith("//!")) {
                if (s.charAt(3) == '<') {
                    continue;
                } else if (s.startsWith("/// ") || s.startsWith("//!")) {
                    s = (comment.length() == 0 || comment.contains("*/")
                            || !comment.contains("/*") ? "/**" : " * ") + s.substring(3);
                    closeComment = true;
                } else if (!s.startsWith("///")) {
                    s = "/**" + s.substring(3);
                }
            }
            comment += token.spacing + s;
        }
        if (closeComment && !comment.endsWith("*/")) {
            comment += " */";
        }
        tokens.raw = false;
        return comment;
    }

    String commentAfter() throws Exception {
        String comment = "";
        tokens.raw = true;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        boolean closeComment = false;
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            String s = token.value;
            if (s.startsWith("/**") || s.startsWith("/*!") || s.startsWith("///") || s.startsWith("//!")) {
                if (s.charAt(3) != '<') {
                    continue;
                } else if (s.startsWith("///") || s.startsWith("//!")) {
                    s = (comment.length() == 0 || comment.contains("*/")
                            || !comment.contains("/*") ? "/**" : " * ") + s.substring(4);
                    closeComment = true;
                } else {
                    s = "/**" + s.substring(4);
                }
                comment += s;
            }
        }
        if (closeComment && !comment.endsWith("*/")) {
            comment += " */";
        }
        if (comment.length() > 0) {
            comment += "\n";
        }
        tokens.raw = false;
        return comment;
    }


    static class Attribute {
        boolean annotation = false;
        String cppName = "", javaName = "", arguments = "";
    }

    Attribute attribute() throws Exception {
        if (!tokens.get().match(Token.IDENTIFIER)) {
            return null;
        }
        Attribute attr = new Attribute();
        Info info = infoMap.getFirst(attr.cppName = tokens.get().value);
        if (attr.annotation = info != null && info.annotations != null
                && info.valueTypes == null && info.pointerTypes == null) {
            for (String s : info.annotations) {
                attr.javaName += s + " ";
            }
        }
        if (!tokens.next().match('(')) {
            return attr;
        }

        int count = 1;
        tokens.raw = true;
        for (Token token = tokens.next(); !token.match(Token.EOF) && count > 0; token = tokens.next()) {
            if (token.match('(')) {
                count++;
            } else if (token.match(')')) {
                count--;
            } else {
                attr.arguments += token.value;
            }
        }
        tokens.raw = false;
        return attr;
    }

    String body() throws Exception {
        if (!tokens.get().match('{')) {
            return null;
        }

        int count = 1;
        tokens.raw = true;
        for (Token token = tokens.next(); !token.match(Token.EOF) && count > 0; token = tokens.next()) {
            if (token.match('{')) {
                count++;
            } else if (token.match('}')) {
                count--;
            }
        }
        tokens.raw = false;
        return "";
    }


    static class Parameters {
        Declarator[] declarators = null;
        int infoNumber = 0;
        String list = "", signature = "", names = "";
    }

    Parameters parameters(Context context, int infoNumber, boolean useDefaults) throws Exception {
        if (!tokens.get().match('(')) {
            return null;
        }

        int count = 0;
        Parameters params = new Parameters();
        ArrayList<Declarator> dcls = new ArrayList<Declarator>();
        params.list = "(";
        params.names = "(";
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.get()) {
            String spacing = token.spacing;
            if (token.match(')')) {
                params.list += spacing + ")";
                params.names += ")";
                tokens.next();
                break;
            }
            Declarator dcl = declarator(context, "arg" + count++, infoNumber, useDefaults, 0, true, false);
            boolean hasDefault = !tokens.get().match(',', ')');
            if (dcl != null && !dcl.type.javaName.equals("void") && (!hasDefault || !useDefaults)) {
                params.infoNumber = Math.max(params.infoNumber, dcl.infoNumber);
                params.list += (count > 1 ? "," : "") + spacing + dcl.type.annotations + dcl.type.javaName + " " + dcl.javaName;
                params.signature += '_';
                for (char c : dcl.type.javaName.substring(dcl.type.javaName.lastIndexOf(' ') + 1).toCharArray()) {
                    params.signature += Character.isJavaIdentifierPart(c) ? c : '_';
                }
                params.names += (count > 1 ? ", " : "") + dcl.javaName;
                if (dcl.javaName.startsWith("arg")) {
                    try {
                        count = Integer.parseInt(dcl.javaName.substring(3)) + 1;
                    } catch (NumberFormatException e) { /* don't care if not int */ }
                }
            }
            if (!hasDefault || !useDefaults) {
                dcls.add(dcl);
            }
            if (hasDefault) {
                // output default argument as a comment
                if (!useDefaults) {
                    params.list += "/*" + tokens.get();
                }
                int count2 = 0;
                for (token = tokens.next(), token.spacing = ""; !token.match(Token.EOF); token = tokens.next()) {
                    if (count2 == 0 && token.match(',', ')')) {
                        break;
                    } else if (token.match('(')) {
                        count2++;
                    } else if (token.match(')')) {
                        count2--;
                    }
                    if (!useDefaults) {
                        params.list += token.spacing + token;
                    }
                }
                if (!useDefaults) {
                    params.list += "*/";
                }
            }
            if (tokens.get().expect(',', ')').match(',')) {
                tokens.next();
            }
        }
        params.declarators = dcls.toArray(new Declarator[dcls.size()]);
        return params;
    }

    boolean function(Context context, DeclarationList declList) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public native ";
        Type type = type(context);
        Parameters params = parameters(context, 0, false);
        Declarator dcl = new Declarator();
        Declaration decl = new Declaration();
        if (type.javaName.length() == 0) {
            // not a function, probably an attribute
            tokens.index = backIndex;
            return false;
        } else if (context.group == null && params != null) {
            // this is a constructor definition or specialization, skip over
            if (tokens.get().match('{')) {
                body();
            } else {
                tokens.next();
            }
            decl.text = spacing;
            declList.add(decl);
            return true;
        } else if (type.constructor || type.destructor || type.operator) {
            // this is a constructor, destructor, or cast operator
            dcl.type = type;
            dcl.parameters = params;
            dcl.cppName = type.cppName;
            dcl.javaName = type.javaName.substring(type.javaName.lastIndexOf(' ') + 1);
            if (type.operator) {
                dcl.cppName = "operator " + dcl.cppName;
                dcl.javaName = "as" + Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1);
            }
            dcl.signature = dcl.javaName + params.signature;
        } else {
            tokens.index = backIndex;
            dcl = declarator(context, null, 0, false, 0, false, false);
            type = dcl.type;
        }

        int namespace = dcl.cppName.lastIndexOf("::");
        if (context.namespace != null && namespace < 0) {
            dcl.cppName = context.namespace + "::" + dcl.cppName;
        }
        Info info = infoMap.getFirst(dcl.cppName);
        String localName = dcl.cppName;
        if (localName.startsWith(context.namespace + "::")) {
            localName = dcl.cppName.substring(context.namespace.length() + 2);
        }
        if (type.javaName.length() == 0 || dcl.parameters == null) {
            tokens.index = backIndex;
            return false;
        } else if ((context.group == null && localName.contains("::")) || (info != null && info.skip)) {
            // this is a member function definition or specialization, skip over
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
                if (attribute() == null) {
                    break;
                }
            }
            if (tokens.get().match('{')) {
                body();
            } else {
                tokens.next();
            }
            decl.text = spacing;
            declList.add(decl);
            return true;
        } else if (type.staticMember || context.group == null) {
            modifiers = "public static native ";
        }

        LinkedList<Declarator> prevDcl = new LinkedList<Declarator>();
        boolean first = true;
        for (int n = -2; n < Integer.MAX_VALUE; n++) {
            decl = new Declaration();
            tokens.index = backIndex;
            if (type.constructor || type.destructor || type.operator) {
                type = type(context);
                params = parameters(context, n / 2, n % 2 != 0);
                dcl = new Declarator();
                dcl.type = type;
                dcl.parameters = params;
                dcl.cppName = type.cppName;
                dcl.javaName = type.javaName.substring(type.javaName.lastIndexOf(' ') + 1);
                if (type.operator) {
                    dcl.cppName = "operator " + dcl.cppName;
                    dcl.javaName = "as" + Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1);
                }
                dcl.signature = dcl.javaName + params.signature;
                if (tokens.get().match(':')) {
                    for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                        if (token.match('{', ';')) {
                            break;
                        }
                    }
                }
            } else {
                dcl = declarator(context, null, n / 2, n % 2 != 0, 0, false, false);
                type = dcl.type;
            }
            decl.declarator = dcl;
            if (context.namespace != null && context.group == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            if (type.constructor) {
                decl.text += "public " + dcl.javaName + dcl.parameters.list + " { allocate" + params.names + "; }\n" +
                             "private native void allocate" + dcl.parameters.list + ";\n";
            } else {
                decl.text += modifiers + type.annotations + type.javaName + " " + dcl.javaName + dcl.parameters.list + ";\n";
            }
            decl.signature = dcl.signature;

            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
                decl.constMember |= token.match(Token.CONST);
                if (attribute() == null) {
                    break;
                }
            }
            if (tokens.get().match('{')) {
                body();
            } else {
                if (tokens.get().match('=')) {
                    tokens.next().expect("0");
                    tokens.next().expect(';');
                    decl.abstractMember = true;
                }
                tokens.next();
            }
            if (info != null && info.javaText != null) {
                decl.text = info.javaText;
                decl.declarator = null;
            }
            String comment = commentAfter();
            if (first) {
                first = false;
                declList.spacing = spacing;
                decl.text = comment + decl.text;
            }

            boolean found = false;
            for (Declarator d : prevDcl) {
                found |= dcl.signature.equals(d.signature);
            }
            if (dcl.javaName.length() > 0 && !found && !type.destructor) {
                declList.add(decl);
            } else if (found && n / 2 > 0 && n % 2 == 0) {
                break;
            }
            prevDcl.add(dcl);
        }
        declList.spacing = null;
        return true;
    }

    boolean variable(Context context, DeclarationList declList) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public static native ";
        String setterType = "void ";
        Declarator dcl = declarator(context, null, 0, false, 0, false, true);
        Declaration decl = new Declaration();
        String cppName = dcl.cppName;
        String javaName = dcl.javaName;
        if (javaName == null || !tokens.get().match('[', '=', ',', ':', ';')) {
            tokens.index = backIndex;
            return false;
        } else if (!dcl.type.staticMember && context.group != null) {
            modifiers = "public native ";
            setterType = context.shorten(context.group.javaName) + " ";
        }

        int namespace = cppName.lastIndexOf("::");
        if (context.namespace != null && namespace < 0) {
            cppName = context.namespace + "::" + cppName;
        }
        Info info = infoMap.getFirst(cppName);
        if (info != null && info.skip) {
            decl.text = spacing;
            declList.add(decl);
            return true;
        }
        boolean first = true;
        for (Declarator metadcl : context.variables != null ? context.variables : new Declarator[] { null }) {
            for (int n = 0; n < Integer.MAX_VALUE; n++) {
                decl = new Declaration();
                tokens.index = backIndex;
                dcl = declarator(context, null, -1, false, n, false, true);
                if (dcl == null) {
                    break;
                }
                decl.declarator = dcl;
                javaName = dcl.javaName;
                if (metadcl == null || metadcl.indices == 0 || dcl.indices == 0) {
                    // arrays are currently not supported for both metadcl and dcl at the same time
                    String indices = "";
                    for (int i = 0; i < (metadcl == null || metadcl.indices == 0 ? dcl.indices : metadcl.indices); i++) {
                        if (i > 0) {
                            indices += ", ";
                        }
                        indices += "int " + (char)('i' + i);
                    }
                    if (context.namespace != null && context.group == null) {
                        decl.text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (metadcl != null && metadcl.cppName.length() > 0) {
                        decl.text += metadcl.indices == 0
                                ? "@Name(\"" + metadcl.cppName + "." + dcl.cppName + "\") "
                                : "@Name({\"" + metadcl.cppName + "\", \"." + dcl.cppName + "\"}) ";
                        javaName = metadcl.javaName + "_" + dcl.javaName;
                    }
                    if (dcl.type.constValue) {
                        decl.text += "@MemberGetter ";
                    }
                    decl.text += modifiers + dcl.type.annotations.replace("@ByVal ", "@ByRef ")
                              + dcl.type.javaName + " " + javaName + "(" + indices + ");";
                    if (!dcl.type.constValue) {
                        if (indices.length() > 0) {
                            indices += ", ";
                        }
                        decl.text += " " + modifiers + setterType + javaName + "(" + indices + dcl.type.javaName + " " + javaName + ");";
                    }
                    decl.text += "\n";
                }
                if (dcl.indices > 0) {
                    // in the case of arrays, also add a pointer accessor
                    tokens.index = backIndex;
                    dcl = declarator(context, null, -1, false, n, true, false);
                    String indices = "";
                    for (int i = 0; i < (metadcl == null ? 0 : metadcl.indices); i++) {
                        if (i > 0) {
                            indices += ", ";
                        }
                        indices += "int " + (char)('i' + i);
                    }
                    if (context.namespace != null && context.group == null) {
                        decl.text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (metadcl != null && metadcl.cppName.length() > 0) {
                        decl.text += metadcl.indices == 0
                                ? "@Name(\"" + metadcl.cppName + "." + dcl.cppName + "\") "
                                : "@Name({\"" + metadcl.cppName + "\", \"." + dcl.cppName + "\"}) ";
                        javaName = metadcl.javaName + "_" + dcl.javaName;
                    }
                    decl.text += "@MemberGetter " + modifiers + dcl.type.annotations.replace("@ByVal ", "@ByRef ")
                              + dcl.type.javaName + " " + javaName + "(" + indices + ");\n";
                }
                decl.signature = dcl.signature;
                if (info != null && info.javaText != null) {
                    decl.text = info.javaText;
                    decl.declarator = null;
                }
                while (!tokens.get().match(Token.EOF, ';')) {
                    tokens.next();
                }
                tokens.next();
                String comment = commentAfter();
                if (first) {
                    first = false;
                    declList.spacing = spacing;
                    decl.text = comment + decl.text;
                }
                decl.variable = true;
                declList.add(decl);
            }
        }
        declList.spacing = null;
        return true;
    }

    boolean macro(Context context, DeclarationList declList) throws Exception {
        int backIndex = tokens.index;
        if (!tokens.get().match('#')) {
            return false;
        }
        tokens.raw = true;
        String spacing = tokens.get().spacing;
        Token keyword = tokens.next();

        tokens.next();
        int beginIndex = tokens.index;
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.spacing.indexOf('\n') >= 0) {
                break;
            }
        }
        int endIndex = tokens.index;
        while (tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        int lastIndex = tokens.index;

        Declaration decl = new Declaration();
        if (keyword.match(Token.DEFINE) && beginIndex + 1 < endIndex) {
            tokens.index = beginIndex;
            String macroName = tokens.get().value;
            Token first = tokens.next();
            boolean hasArgs = first.spacing.length() == 0 && first.match('(');
            LinkedList<Info> infoList = infoMap.get(macroName);
            for (Info info : infoList.size() > 0 ? infoList : Arrays.asList(new Info[] { null })) {
                if (hasArgs && info == null) {
                    // save declaration for expansion
                    info = new Info(macroName).cppText("");
                    tokens.index = backIndex;
                    for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                        info.cppText += token.spacing + token;
                    }
                    infoMap.put(info);
                } else if (info != null && info.cppText == null &&
                        info.cppTypes != null && info.cppTypes.length > (hasArgs ? 0 : 1)) {
                    // declare as a static native method
                    LinkedList<Declarator> prevDcl = new LinkedList<Declarator>();
                    for (int n = -1; n < Integer.MAX_VALUE; n++) {
                        int count = 1;
                        tokens.index = beginIndex + 2;
                        String params = "(";
                        for (Token token = tokens.get(); hasArgs && tokens.index < lastIndex
                                && count < info.cppTypes.length; token = tokens.next()) {
                            if (token.match(Token.IDENTIFIER)) {
                                String type = info.cppTypes[count];
                                String name = token.value;
                                if (name.equals("...")) {
                                    name = "arg" + count;
                                }
                                params += type + " " + name;
                                if (++count < info.cppTypes.length) {
                                    params += ", ";
                                }
                            } else if (token.match(')')) {
                                break;
                            }
                        }
                        while (count < info.cppTypes.length) {
                            String type = info.cppTypes[count];
                            String name = "arg" + count;
                            params += type + " " + name;
                            if (++count < info.cppTypes.length) {
                                params += ", ";
                            }
                        }
                        params += ")";

                        Declarator dcl = new Parser(this, info.cppTypes[0] + " " + macroName + params).declarator(context, null, n, false, 0, false, false);
                        for (int i = 0; i < info.cppNames.length; i++) {
                            if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                                macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                                break;
                            }
                        }

                        boolean found = false;
                        for (Declarator d : prevDcl) {
                            found |= dcl.signature.equals(d.signature);
                        }
                        if (!found) {
                            decl.text += "public static native " + dcl.type.annotations + dcl.type.javaName + " " + macroName + dcl.parameters.list + ";\n";
                        } else if (found && n > 0) {
                            break;
                        }
                        prevDcl.add(dcl);
                    }
                } else if (info == null || (info.cppText == null &&
                        (info.cppTypes == null || info.cppTypes.length == 1))) {
                    // declare as a static final variable
                    String value = "";
                    String type = "int";
                    String cat = "";
                    tokens.index = beginIndex + 1;
                    Token prevToken = new Token();
                    boolean translate = true;
                    for (Token token = tokens.get(); tokens.index < lastIndex; token = tokens.next()) {
                        if (token.match(Token.STRING)) {
                            type = "String"; cat = " + "; break;
                        } else if (token.match(Token.FLOAT)) {
                            type = "double"; cat = ""; break;
                        } else if (token.match(Token.INTEGER) && token.value.endsWith("L")) {
                            type = "long"; cat = ""; break;
                        } else if ((prevToken.match(Token.IDENTIFIER) && token.match('(')) || token.match('{', '}')) {
                            translate = false;
                        }
                        prevToken = token;
                    }
                    if (info != null) {
                        if (info.cppTypes != null) {
                            Declarator dcl = new Parser(this, info.cppTypes[0]).declarator(context, null, -1, false, 0, false, true);
                            type = dcl.type.annotations + dcl.type.javaName;
                        }
                        for (int i = 0; i < info.cppNames.length; i++) {
                            if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                                macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                                break;
                            }
                        }
                        translate = info.translate;
                    }
                    tokens.index = beginIndex + 1;
                    if (translate) {
                        for (Token token = tokens.get(); tokens.index < lastIndex; token = tokens.next()) {
                            value += token.spacing + token + (tokens.index + 1 < lastIndex ? cat : "");
                        }
                        value = translate(value);
                    } else {
                        decl.text += "public static native @MemberGetter " + type + " " + macroName + "();\n";
                        value = " " + macroName + "()";
                    }
                    int i = type.lastIndexOf(' ');
                    if (i >= 0) {
                        type = type.substring(i + 1);
                    }
                    if (value.length() > 0) {
                        decl.text += "public static final " + type + " " + macroName + " =" + value + ";\n";
                    }
                }
            }
        }

        if (decl.text.length() == 0) {
            // output whatever we did not process as comment
            tokens.index = beginIndex;
            int n = spacing.lastIndexOf('\n') + 1;
            decl.text += "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
            for (Token token = tokens.get(); tokens.index < lastIndex; token = tokens.next()) {
                decl.text += token.match("\n") ? "\n// " : token.spacing + token;
            }
            spacing = spacing.substring(0, n);
        }
        if (decl.text.length() > 0) {
            tokens.index = lastIndex;
            String comment = commentAfter();
            decl.text = comment + decl.text;
        }
        tokens.raw = false;
        declList.spacing = spacing;
        declList.add(decl);
        declList.spacing = null;
        return true;
    }

    boolean typedef(Context context, DeclarationList declList) throws Exception {
        String spacing = tokens.get().spacing;
        if (!tokens.get().match(Token.TYPEDEF)) {
            return false;
        }
        Declaration decl = new Declaration();
        Declarator dcl = declarator(context, null, 0, false, 0, true, false);
        tokens.next();

        String typeName = dcl.type.cppName, defName = dcl.cppName;
        if (context.namespace != null) {
            defName = context.namespace + "::" + defName;
        }
        if (dcl.definition != null) {
            // a function pointer or something
            decl = dcl.definition;
            if (dcl.javaName.length() > 0 && context.group != null) {
                dcl.javaName = context.group.javaName + "." + dcl.javaName;
            }
            infoMap.put(new Info(defName).valueTypes(dcl.javaName));
        } else if (typeName.equals("void")) {
            // some opaque data type
            Info info = infoMap.getFirst(defName);
            if (info == null || !info.skip) {
                decl.text = "@Opaque public static class " + dcl.javaName + " extends Pointer {\n" +
                            "    public " + dcl.javaName + "() { }\n" +
                            "    public " + dcl.javaName + "(Pointer p) { super(p); }\n" +
                            "}";
            }
        } else {
            // point back to original type
            Info info = infoMap.getFirst(typeName);
            if (info == null || !info.skip) {
                info = info != null ? new Info(info).cppNames(defName) : new Info(defName);
                if (info.cppTypes == null) {
                    info.cppTypes(typeName);
                }
                if (info.pointerTypes == null) {
                    info.pointerTypes(typeName);
                }
                if (info.annotations == null) {
                    info.cast(!dcl.cppName.equals(info.pointerTypes[0]));
                }
                infoMap.put(info);
            }
        }

        String comment = commentAfter();
        decl.text = comment + decl.text;
        declList.spacing = spacing;
        declList.add(decl);
        declList.spacing = null;
        return true;
    }

    boolean using(Context context, DeclarationList declList) throws Exception {
        if (!tokens.get().match(Token.USING)) {
            return false;
        }
        String spacing = tokens.get().spacing;
        boolean namespace = tokens.get(1).match(Token.NAMESPACE);
        Declarator dcl = declarator(context, null, 0, false, 0, true, false);
        tokens.next();

        context.usingList.add(dcl.type.cppName + (namespace ? "::" : ""));

        Declaration decl = new Declaration();
        if (dcl.definition != null) {
            decl = dcl.definition;
        }
        String comment = commentAfter();
        decl.text = comment + decl.text;
        declList.spacing = spacing;
        declList.add(decl);
        declList.spacing = null;
        return true;
    }

    boolean group(Context context, DeclarationList declList) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        boolean typedef = tokens.get().match(Token.TYPEDEF);
        boolean foundGroup = false, friend = false;
        Context ctx = new Context(context);
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.CLASS, Token.STRUCT, Token.UNION)) {
                foundGroup = true;
                ctx.inaccessible = token.match(Token.CLASS);
                break;
            } else if (token.match(Token.FRIEND)) {
                friend = true;
            } else if (!token.match(Token.IDENTIFIER)) {
                break;
            }
        }
        if (!foundGroup) {
            tokens.index = backIndex;
            return false;
        }

        tokens.next().expect(Token.IDENTIFIER, '{');
        if (typedef && tokens.get(1).match('*')) {
            tokens.next();
        }
        if (!tokens.get().match('{') && tokens.get(1).match(Token.IDENTIFIER)
                && (typedef || !tokens.get(2).match(';'))) {
            tokens.next();
        }
        Type type = type(context);
        ArrayList<Type> baseClasses = new ArrayList<Type>();
        Declaration decl = new Declaration();
        decl.text = type.annotations;
        String name = type.javaName;
        boolean anonymous = !typedef && type.cppName.length() == 0, derivedClass = false;
        if (type.cppName.length() > 0 && tokens.get().match(':')) {
            derivedClass = true;
            for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                boolean accessible = false;
                if (token.match(Token.VIRTUAL)) {
                    continue;
                } else if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC)) {
                    accessible = token.match(Token.PUBLIC);
                    tokens.next();
                }
                Type t = type(context);
                if (accessible) {
                    baseClasses.add(t);
                }
                if (tokens.get().expect(',', '{').match('{')) {
                    break;
                }
            }
        }
        if (!tokens.get().match('{', ';')) {
            tokens.index = backIndex;
            return false;
        }
        int startIndex = tokens.index;
        ArrayList<Declarator> variables = new ArrayList<Declarator>();
        if (body() != null && !tokens.get().match(';')) {
            if (typedef) {
                for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                    if (token.match(';')) {
                        decl.text += token.spacing;
                        break;
                    } else {
                        name = type.javaName = type.cppName = token.value;
                    }
                }
            } else {
                int index = tokens.index - 1;
                for (int n = 0; n < Integer.MAX_VALUE; n++) {
                    tokens.index = index;
                    Declarator dcl = declarator(context, null, -1, false, n, false, true);
                    if (dcl == null) {
                        break;
                    } else {
                        variables.add(dcl);
                    }
                }
                int n = spacing.lastIndexOf('\n');
                if (n >= 0) {
                    decl.text += spacing.substring(0, n);
                }
            }
        }
        int namespace = type.cppName.lastIndexOf("::");
        if (context.namespace != null && namespace < 0) {
            type.cppName = context.namespace + "::" + type.cppName;
        }
        Info info = infoMap.getFirst(type.cppName);
        if (info != null && info.skip) {
            decl.text = "";
            declList.add(decl);
            return true;
        } else if (info != null && info.pointerTypes != null && info.pointerTypes.length > 0) {
            name = type.javaName = info.pointerTypes[0];
        } else if (info == null) {
            if (type.javaName.length() > 0 && context.group != null) {
                type.javaName = context.group.javaName + "." + type.javaName;
            }
            infoMap.put(info = new Info(type.cppName).pointerTypes(type.javaName));
        }
        Type base = new Type("Pointer");
        if (baseClasses.size() > 0) {
            base = baseClasses.remove(0);
        }
        String casts = "";
        if (baseClasses.size() > 0) {
            for (Type t : baseClasses) {
                casts += "    public " + t.javaName + " as" + t.javaName + "() { return as" + t.javaName + "(this); }\n"
                        + "    @Namespace public static native @Name(\"static_cast<" + t.cppName + "*>\") "
                        + t.javaName + " as" + t.javaName + "(" + type.javaName + " pointer);\n";
            }
        }
        decl.signature = type.javaName;
        tokens.index = startIndex;
        if (name.length() > 0 && tokens.get().match(';')) {
            // incomplete type (forward or friend declaration)
            tokens.next();
            if (friend) {
                decl.text = "";
                declList.add(decl);
                return true;
            } else if (info != null && info.base != null) {
                base.javaName = info.base;
            }
            String fullName = context.namespace != null ? context.namespace + "::" + name : name;
            if (!fullName.equals(type.cppName)) {
                decl.text += "@Name(\"" + type.cppName + "\") ";
            } else if (context.namespace != null && context.group == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            decl.text += "@Opaque public static class " + name + " extends " + base.javaName + " {\n" +
                         "    public " + name + "() { }\n" +
                         "    public " + name + "(Pointer p) { super(p); }\n" +
                         "}";
            decl.type = type;
            decl.incomplete = true;
            String comment = commentAfter();
            decl.text = comment + decl.text;
            declList.spacing = spacing;
            declList.add(decl);
            declList.spacing = null;
            return true;
        } else if (tokens.get().match('{')) {
            tokens.next();
        }

        if (!anonymous) {
            ctx.namespace = type.cppName;
            ctx.group = type;
        }
        if (variables.size() > 0) {
            ctx.variables = variables.toArray(new Declarator[variables.size()]);
        }
        DeclarationList declList2 = new DeclarationList();
        declarations(ctx, declList2);
        boolean implicitConstructor = true, defaultConstructor = false, intConstructor = false, abstractClass = false, haveVariables = false;
        for (Declaration d : declList2) {
            if (d.declarator != null && d.declarator.type != null && d.declarator.type.constructor) {
                implicitConstructor = false;
                Declarator[] paramDcls = d.declarator.parameters.declarators;
                defaultConstructor |= paramDcls.length == 0 && !d.inaccessible;
                intConstructor |= paramDcls.length == 1 && paramDcls[0].type.javaName.equals("int") && !d.inaccessible;
            } else if (d.abstractMember) {
                implicitConstructor = false;
                abstractClass = true;
            }
            haveVariables |= d.variable;
        }
        if (!anonymous) {
            String fullName = context.namespace != null ? context.namespace + "::" + name : name;
            if (!fullName.equals(type.cppName)) {
                decl.text += "@Name(\"" + type.cppName + "\") ";
            } else if (context.namespace != null && context.group == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            if ((!implicitConstructor || derivedClass) && haveVariables) {
                decl.text += "@NoOffset ";
            }
            if (info != null && info.base != null) {
                base.javaName = info.base;
            }
            decl.text += "public static class " + name + " extends " + base.javaName + " {\n" +
                         "    static { Loader.load(); }\n";

            if (implicitConstructor) {
                decl.text += "    public " + name + "() { allocate(); }\n" +
                             "    public " + name + "(int size) { allocateArray(size); }\n" +
                             "    public " + name + "(Pointer p) { super(p); }\n" +
                             "    private native void allocate();\n" +
                             "    private native void allocateArray(int size);\n" +
                             "    @Override public " + name + " position(int position) {\n" +
                             "        return (" + name + ")super.position(position);\n" +
                             "    }\n";
            } else {
                if (!defaultConstructor || abstractClass) {
                    decl.text += "    public " + name + "() { }\n";
                }
                decl.text += "    public " + name + "(Pointer p) { super(p); }\n";
                if (defaultConstructor && !abstractClass && !intConstructor) {
                    decl.text += "    public " + name + "(int size) { allocateArray(size); }\n" +
                                 "    private native void allocateArray(int size);\n" +
                                 "    @Override public " + name + " position(int position) {\n" +
                                 "        return (" + name + ")super.position(position);\n" +
                                 "    }\n";
                }
            }
            declList.spacing = spacing;
            decl.text = declList.rescan(decl.text + casts + "\n");
            declList.spacing = null;
        }
        for (Declaration d : declList2) {
            if (!d.inaccessible && (d.declarator == null || d.declarator.type == null || !d.declarator.type.constructor || !abstractClass)) {
                decl.text += d.text;
            }
        }
        decl.text += commentBefore(); // for comments at the end without declarations
        if (!anonymous) {
            decl.text += tokens.get().spacing + '}';
        }
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(';')) {
                decl.text += token.spacing;
                break;
            }
        }
        tokens.next();
        decl.type = type;
        if (info != null && info.javaText != null) {
            decl.text = info.javaText;
        }
        declList.add(decl);
        return true;
    }

    boolean enumeration(Context context, DeclarationList declList) throws Exception {
        int backIndex = tokens.index;
        String enumSpacing = tokens.get().spacing;
        boolean typedef = tokens.get().match(Token.TYPEDEF);
        boolean foundEnum = false;
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.ENUM)) {
                foundEnum = true;
                break;
            } else if (!token.match(Token.IDENTIFIER)) {
                break;
            }
        }
        if (!foundEnum) {
            tokens.index = backIndex;
            return false;
        }

        if (typedef && !tokens.get(1).match('{') && tokens.get(2).match(Token.IDENTIFIER)) {
            tokens.next();
        }
        boolean first = true;
        int count = 0;
        String countPrefix = " ";
        String enumerators = "";
        String macroText = "";
        String name = tokens.next().expect(Token.IDENTIFIER, '{').value;
        if (!tokens.get().match('{') && !tokens.next().match('{')) {
            tokens.index = backIndex;
            return false;
        }
        for (Token token = tokens.next(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            if (macro(context, declList)) {
                Declaration macroDecl = declList.removeLast();
                macroText += macroDecl.text;
                if (!first && !macroDecl.text.trim().startsWith("//")) {
                    enumerators += ";\n";
                    macroText += "\npublic static final int";
                    first = true;
                }
                continue;
            }
            String comment = commentBefore();
            Token enumerator = tokens.get();
            String spacing2 = " ";
            String separator = first ? "" : ",";
            if (tokens.next().match('=')) {
                spacing2 = tokens.get().spacing;
                countPrefix = " ";
                int count2 = 0;
                Token prevToken = new Token();
                boolean translate = true;
                for (token = tokens.next(); !token.match(Token.EOF, ',', '}') || count2 > 0; token = tokens.next()) {
                    countPrefix += token.spacing + token;
                    if (token.match('(')) {
                        count2++;
                    } else if (token.match(')')) {
                        count2--;
                    }
                    if (prevToken.match(Token.IDENTIFIER) && token.match('(')) {
                        translate = false;
                    }
                    prevToken = token;
                }
                try {
                    count = Integer.parseInt(countPrefix.trim());
                    countPrefix = " ";
                } catch (NumberFormatException e) {
                    count = 0;
                    if (translate) {
                        countPrefix = translate(countPrefix);
                    } else {
                        if (!first) {
                            separator = ";\n";
                            first = true;
                        }
                        separator += "public static native @MemberGetter int " + enumerator.value + "();\npublic static final int";
                        countPrefix = " " + enumerator.value + "()";
                    }
                }
            }
            first = false;
            enumerators += separator + macroText + comment;
            macroText = "";
            comment = commentAfter();
            if (comment.length() == 0 && tokens.get().match(',')) {
                tokens.next();
                comment = commentAfter();
            }
            String spacing = enumerator.spacing;
            if (comment.length() > 0) {
                enumerators += spacing + comment;
                int newline = spacing.lastIndexOf('\n');
                if (newline >= 0) {
                    spacing = spacing.substring(newline + 1);
                }
            }
            enumerators += spacing + enumerator.value + spacing2 + "=" + countPrefix;
            if (countPrefix.trim().length() > 0) {
                if (count > 0) {
                    enumerators += " + " + count;
                }
            } else {
                enumerators += count;
            }
            count++;
        }
        String comment = commentBefore();
        Declaration decl = new Declaration();
        Token token = tokens.next();
        if (token.match(Token.IDENTIFIER)) {
            // XXX: If !isTypedef, this is a variable declaration with anonymous type
            name = token.value;
            token = tokens.next();
        }
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        decl.text += enumSpacing + "/** enum " + name + " */\n";
        int newline = enumSpacing.lastIndexOf('\n');
        if (newline >= 0) {
            enumSpacing = enumSpacing.substring(newline + 1);
        }
        if (!Character.isWhitespace(enumerators.charAt(0))) {
            enumerators = " " + enumerators;
        }
        decl.text += enumSpacing + "public static final int" + enumerators + token.expect(';').spacing + ";";
        if (name.length() > 0) {
            infoMap.put(new Info(name).cast(true).valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"));
        }
        tokens.next();
        decl.text += macroText + comment;
        declList.add(decl);
        return true;
    }

    boolean namespace(Context context, DeclarationList declList) throws Exception {
        if (!tokens.get().match(Token.NAMESPACE)) {
            return false;
        }
        Declaration decl = new Declaration();
        String name = tokens.next().expect(Token.IDENTIFIER).value;
        tokens.next().expect('{');
        tokens.next();

        context = new Context(context);
        context.namespace = context.namespace != null ? context.namespace + "::" + name : name;
        declarations(context, declList);
        decl.text += tokens.get().expect('}').spacing;
        tokens.next();
        declList.add(decl);
        return true;
    }

    boolean extern(Context context, DeclarationList declList) throws Exception {
        if (!tokens.get().match(Token.EXTERN) || !tokens.get(1).match(Token.STRING)) {
            return false;
        }
        Declaration decl = new Declaration();
        tokens.next().expect("\"C\"");
        if (!tokens.next().match('{')) {
            declList.add(decl);
            return true;
        }
        tokens.next();

        declarations(context, declList);
        tokens.get().expect('}');
        tokens.next();
        declList.add(decl);
        return true;
    }


    static class Declaration {
        Type type = null;
        Declarator declarator = null;
        boolean abstractMember = false, constMember = false, inaccessible = false, incomplete = false, variable = false;
        String signature = "", text = "";
    }

    static class DeclarationList extends LinkedList<Declaration> {
        InfoMap infoMap = null;
        Context context = null;
        TemplateMap templateMap = null;
        ListIterator<Info> infoIterator = null;
        String spacing = null;

        String rescan(String lines) {
            if (spacing == null) {
                return lines;
            }
            String text = "";
            Scanner scanner = new Scanner(lines);
            while (scanner.hasNextLine()) {
                text += spacing + scanner.nextLine();
                int newline = spacing.lastIndexOf('\n');
                spacing = newline >= 0 ? spacing.substring(newline) : "\n";
            }
            return text;
        }

        @Override public boolean add(Declaration decl) {
            boolean add = true;
            if (templateMap != null && !templateMap.full() && (decl.type != null || decl.declarator != null)) {
                Type type = templateMap.type = decl.type;
                Declarator dcl = templateMap.declarator = decl.declarator;
                LinkedList<Info> infoList = infoMap.get(dcl != null ? dcl.cppName : type.cppName);
                infoIterator = infoList.size() > 0 ? infoList.listIterator() : null;
                add = false;
            } else if (decl.declarator != null && decl.declarator.type != null) {
                Info info = infoMap.getFirst(decl.declarator.type.cppName);
                if (info != null && info.skip && info.valueTypes == null && info.pointerTypes == null) {
                    add = false;
                } else if (decl.declarator.parameters != null) {
                    for (Declarator d : decl.declarator.parameters.declarators) {
                        if (d != null && d.type != null) {
                            info = infoMap.getFirst(d.type.cppName);
                            if (info != null && info.skip && info.valueTypes == null && info.pointerTypes == null) {
                                add = false;
                                break;
                            }
                        }
                    }
                }
            }
            if (!add) {
                return false;
            }

            LinkedList<Declaration> stack = new LinkedList<Declaration>();
            ListIterator<Declaration> it = stack.listIterator();
            it.add(decl); it.previous();
            while (it.hasNext()) {
                decl = it.next();
                Declarator dcl = decl.declarator;
                if (dcl != null && dcl.definition != null) {
                    it.add(dcl.definition); it.previous();
                }
                if (dcl != null && dcl.parameters != null && dcl.parameters.declarators != null) {
                    for (Declarator d : dcl.parameters.declarators) {
                        if (d != null && d.definition != null) {
                            it.add(d.definition); it.previous();
                        }
                    }
                }
            }

            while (!stack.isEmpty()) {
                decl = stack.removeLast();
                if (context != null) {
                    decl.inaccessible = context.inaccessible;
                }
                it = listIterator();
                boolean found = false;
                while (it.hasNext()) {
                    Declaration d = it.next();
                    if (d.signature.length() > 0 && d.signature.equals(decl.signature)) {
                        if ((d.constMember && !decl.constMember) || (d.inaccessible && !decl.inaccessible) || (d.incomplete && !decl.incomplete)) {
                            // add preferably non-const accessible complete versions of functions and types
                            it.remove();
                        } else {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    decl.text = rescan(decl.text);
                    super.add(decl);
                }
            }
            return true;
        }
    }

    void declarations(Context context, DeclarationList declList) throws Exception {
        for (Token token = tokens.get(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC) && tokens.next().match(':')) {
                context.inaccessible = !token.match(Token.PUBLIC);
                tokens.next();
            }
            Context ctx = context;
            String comment = commentBefore();
            token = tokens.get();
            String spacing = token.spacing;
            TemplateMap map = template(ctx);
            if (map != null) {
                token = tokens.get();
                token.spacing = spacing;
                ctx = new Context(ctx);
                ctx.templateMap = map;
            }
            Declaration decl = new Declaration();
            if (comment != null && comment.length() > 0) {
                decl.inaccessible = ctx.inaccessible;
                decl.text = comment;
                declList.add(decl);
            }
            int startIndex = tokens.index;
            declList.infoMap = infoMap;
            declList.context = ctx;
            declList.templateMap = map;
            declList.infoIterator = null;
            declList.spacing = null;
            do {
                if (map != null && declList.infoIterator != null && declList.infoIterator.hasNext()) {
                    Info info = declList.infoIterator.next();
                    if (info == null) {
                        continue;
                    }
                    Type type = new Parser(this, info.cppNames[0]).type(context);
                    if (type.arguments == null) {
                        continue;
                    }
                    int count = 0;
                    for (Map.Entry<String,String> e : map.entrySet()) {
                        if (count < type.arguments.length) {
                            e.setValue(type.arguments[count++].cppName);
                        }
                    }
                    tokens.index = startIndex;
                }

                if (!macro(ctx, declList) && !extern(ctx, declList) && !namespace(ctx, declList)
                        && !enumeration(ctx, declList) && !group(ctx, declList) && !typedef(ctx, declList)
                        && !using(ctx, declList) && !function(ctx, declList) && !variable(ctx, declList)) {
                    spacing = tokens.get().spacing;
                    if (attribute() != null) {
                        tokens.get().spacing = spacing;
                    } else {
                        throw new Exception(token.file + ":" + token.lineNumber + ": Could not parse declaration at '" + token + "'");
                    }
                }
                while (tokens.get().match(';') && !tokens.get().match(Token.EOF)) {
                    tokens.next();
                }
            } while (declList.infoIterator != null && declList.infoIterator.hasNext());
        }
    }

    void parse(String outputFilename, Context context, String[] includePath, String ... inputFilenames) throws IOException, Exception {
        File[] files = new File[inputFilenames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(inputFilenames[i]);
        }
        parse(new File(outputFilename), context, includePath, files);
    }
    void parse(File outputFile, Context context, String[] includePath, File ... inputFiles) throws IOException, Exception {
        ArrayList<Token> tokenList = new ArrayList<Token>();
        String lineSeparator = null;
        for (File file : inputFiles) {
            if (!file.exists()) {
                for (String s : includePath) {
                    File f = new File(s, file.getPath());
                    if (f.exists()) {
                        file = f;
                        break;
                    }
                }
            }
            Info info = infoMap.getFirst(file.getName());
            if (info != null && info.skip) {
                continue;
            }
            logger.info("Parsing " + file);
            Token token = new Token();
            token.type = Token.COMMENT;
            token.value = "\n// Parsed from " + file + "\n\n";
            tokenList.add(token);
            Tokenizer tokenizer = new Tokenizer(file);
            while (!(token = tokenizer.nextToken()).isEmpty()) {
                if (token.type == -1) {
                    token.type = Token.COMMENT;
                }
                tokenList.add(token);
            }
            if (lineSeparator == null) {
                lineSeparator = tokenizer.lineSeparator;
            }
            tokenizer.close();
            token = new Token();
            token.type = Token.COMMENT;
            token.spacing = "\n";
            tokenList.add(token);
        }
        tokens = new TokenIndexer(infoMap, tokenList.toArray(new Token[tokenList.size()]));

        final String newline = lineSeparator != null ? lineSeparator : "\n";
        Writer out = outputFile != null ? new FileWriter(outputFile) {
            @Override public Writer append(CharSequence text) throws IOException {
                return super.append(((String)text).replace("\n", newline).replace("\\u", "\\u005Cu"));
            }} : new Writer() {
            @Override public void write(char[] cbuf, int off, int len) { }
            @Override public void flush() { }
            @Override public void close() { }
        };
        LinkedList<Info> infoList = leafInfoMap.get(null);
        for (Info info : infoList) {
            if (info.javaText != null && !info.javaText.startsWith("import")) {
                out.append(info.javaText + "\n");
            }
        }
        out.append("    static { Loader.load(); }\n");

        DeclarationList declList = new DeclarationList();
        containers(context, declList);
        declarations(context, declList);
        for (Declaration d : declList) {
            out.append(d.text);
        }
        String comment = commentBefore();
        if (comment != null) {
            out.append(comment);
        }
        out.append("\n}\n").close();
    }

    public File parse(String outputDirectory, String[] classPath, Class cls) throws IOException, Exception {
        return parse(new File(outputDirectory), classPath, cls);
    }
    public File parse(File outputDirectory, String[] classPath, Class cls) throws IOException, Exception {
        Loader.ClassProperties allProperties = Loader.loadProperties(cls, properties, true);
        Loader.ClassProperties clsProperties = Loader.loadProperties(cls, properties, false);
        LinkedList<File> allFiles = allProperties.getHeaderFiles();
        LinkedList<File> clsFiles = clsProperties.getHeaderFiles();
        LinkedList<String> allTargets = allProperties.get("target");
        LinkedList<String> clsTargets = clsProperties.get("target");
        LinkedList<String> clsHelpers = clsProperties.get("helper");
        String target = clsTargets.getFirst(); // there can only be one
        LinkedList<Class> allInherited = allProperties.getInheritedClasses();

        infoMap = new Parser.InfoMap();
        for (Class c : allInherited) {
            try {
                ((InfoMapper)c.newInstance()).map(infoMap);
            } catch (ClassCastException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
                // fail silently as if the interface wasn't implemented
            }
        }
        leafInfoMap = new Parser.InfoMap();
        try {
            ((InfoMapper)cls.newInstance()).map(leafInfoMap);
        } catch (ClassCastException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
            // fail silently as if the interface wasn't implemented
        }
        infoMap.putAll(leafInfoMap);

        String version = Generator.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        String text = "// Targeted by JavaCPP version " + version + "\n\n";
        int n = target.lastIndexOf('.');
        if (n >= 0) {
            text += "package " + target.substring(0, n) + ";\n\n";
        }
        LinkedList<Info> infoList = leafInfoMap.get(null);
        for (Info info : infoList) {
            if (info.javaText != null && info.javaText.startsWith("import")) {
                text += info.javaText + "\n";
            }
        }
        text += "import com.googlecode.javacpp.*;\n" +
                "import com.googlecode.javacpp.annotation.*;\n" +
                "import java.nio.*;\n\n";
        for (String s : allTargets) {
            if (!target.equals(s)) {
                text += "import static " + s + ".*;\n";
            }
        }
        if (allTargets.size() > 1) {
            text += "\n";
        }
        text += "public class " + target.substring(n + 1) + " extends "
             + (clsHelpers.size() > 0 ? clsHelpers.getFirst() : cls.getCanonicalName()) + " {";
        leafInfoMap.putFirst(new Info().javaText(text));

        String targetPath = target.replace('.', File.separatorChar);
        File targetFile = new File(outputDirectory, targetPath + ".java");
        logger.info("Targeting " + targetFile);
        Context context = new Context();
        String[] includePath = classPath;
        n = targetPath.lastIndexOf(File.separatorChar);
        if (n >= 0) {
            includePath = classPath.clone();
            for (int i = 0; i < includePath.length; i++) {
                includePath[i] += File.separator + targetPath.substring(0, n);
            }
        }
        for (File f : allFiles) {
            if (!clsFiles.contains(f)) {
                parse(null, context, includePath, f);
            }
        }
        parse(targetFile, context, includePath, clsFiles.toArray(new File[clsFiles.size()]));
        return targetFile;
    }
}
