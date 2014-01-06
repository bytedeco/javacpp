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
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 *
 * @author Samuel Audet
 */
public class Parser {

    public static class Exception extends java.lang.Exception {
        public Exception(String message) { super(message); }
        public Exception(String message, Throwable cause) { super(message, cause); }
    }

    public static class Info implements Cloneable {
        public Info() { }
        public Info(String ... cppNames) { this.cppNames = cppNames; }

        String[] cppNames = null, javaNames = null, annotations = null, valueTypes = null, pointerTypes = null,
                 genericArgs = null, macroParams = null, templateParams = null;
        boolean cast = false, define = false, translate = false, complete = false, parse = false, skip = false;
        String parent = null, text = null;

        public Info cppNames(String ... cppNames) { this.cppNames = cppNames; return this; }
        public Info javaNames(String ... javaNames) { this.javaNames = javaNames; return this; }
        public Info annotations(String ... annotations) { this.annotations = annotations; return this; }
        public Info valueTypes(String ... valueTypes) { this.valueTypes = valueTypes; return this; }
        public Info pointerTypes(String ... pointerTypes) { this.pointerTypes = pointerTypes; return this; }
        public Info genericArgs(String ... genericArgs) { this.genericArgs = genericArgs; return this; }
        public Info macroParams(String ... macroParams) { this.macroParams = macroParams; return this; }
        public Info templateParams(String ... templateParams) { this.templateParams = templateParams; return this; }
        public Info cast(boolean cast) { this.cast = cast; return this;  }
        public Info define(boolean define) { this.define = define; return this; }
        public Info translate(boolean translate) { this.translate = translate; return this; }
        public Info complete(boolean complete) { this.complete = complete; return this; }
        public Info parse(boolean parse) { this.parse = parse; return this; }
        public Info skip(boolean skip) { this.skip = skip; return this; }
        public Info parent(String parent) { this.parent = parent; return this; }
        public Info text(String text) { this.text = text; return this; }

        @Override public Info clone() {
            Info i = new Info();
            i.cppNames = cppNames != null ? cppNames.clone() : null;
            i.javaNames = javaNames != null ? javaNames.clone() : null;
            i.annotations = annotations != null ? annotations.clone() : null;
            i.valueTypes = valueTypes != null ? valueTypes.clone() : null;
            i.pointerTypes = pointerTypes != null ? pointerTypes.clone() : null;
            i.genericArgs = genericArgs != null ? genericArgs.clone() : null;
            i.macroParams = macroParams != null ? macroParams.clone() : null;
            i.templateParams = templateParams != null ? templateParams.clone() : null;
            i.cast = cast;
            i.define = define;
            i.translate = translate;
            i.complete = complete;
            i.parse = parse;
            i.skip = skip;
            i.parent = parent;
            i.text = text;
            return i;
        }
    }

    public static class InfoMap extends HashMap<String,LinkedList<Info>> {
        public InfoMap() { this.parent = defaults; }
        public InfoMap(InfoMap parent) { this.parent = parent; }

        InfoMap parent = null;
        static final String[] simpleTypes = { "signed", "unsigned", "char", "short", "int", "long", "bool", "float", "double" };
        static { Arrays.sort(simpleTypes); }
        static final InfoMap defaults = new InfoMap(null)
            .put(new Info("void").valueTypes("void").pointerTypes("Pointer"))
            .put(new Info("FILE", "std::exception", "va_list").pointerTypes("Pointer").cast(true))

            .put(new Info("int8_t", "jbyte", "signed char")
                .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))
            .put(new Info("uint8_t", "char", "unsigned char")
                .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]").cast(true))

            .put(new Info("int16_t", "jshort", "short", "signed short", "short int", "signed short int")
                .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))
            .put(new Info("uint16_t", "unsigned short", "unsigned short int")
                .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]").cast(true))

            .put(new Info("int32_t", "jint", "int", "signed int", "signed")
                .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))
            .put(new Info("uint32_t", "unsigned int", "unsigned")
                .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]").cast(true))

            .put(new Info("int64_t", "__int64", "jlong", "long long", "signed long long", "long long int", "signed long long int")
                .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))
            .put(new Info("uint64_t", "__uint64", "unsigned long long", "unsigned long long int")
                .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]").cast(true))

            .put(new Info("long", "signed long", "long int", "signed long int")
                .valueTypes("long").pointerTypes("CLongPointer"))
            .put(new Info("unsigned long", "unsigned long int")
                .valueTypes("long").pointerTypes("CLongPointer").cast(true))

            .put(new Info("size_t").valueTypes("long").pointerTypes("SizeTPointer").cast(true))
            .put(new Info("float", "jfloat").valueTypes("float").pointerTypes("FloatPointer", "FloatBuffer", "float[]"))
            .put(new Info("double", "jdouble").valueTypes("double").pointerTypes("DoublePointer", "DoubleBuffer", "double[]"))
            .put(new Info("std::complex<float>").pointerTypes("FloatPointer", "FloatBuffer", "float[]").cast(true))
            .put(new Info("std::complex<double>").pointerTypes("DoublePointer", "DoubleBuffer", "double[]").cast(true))
            .put(new Info("bool", "jboolean").valueTypes("boolean").pointerTypes("BoolPointer").cast(true))
            .put(new Info("const char").valueTypes("byte").pointerTypes("@Cast(\"const char*\") BytePointer", "String"))
            .put(new Info("std::string").valueTypes("@StdString BytePointer", "@StdString String"))
            .put(new Info("wchar_t", "WCHAR").valueTypes("char").pointerTypes("CharPointer").cast(true))

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

        static String sort(String name) {
            return sort(name, false);
        }
        static String sort(String name, boolean normalize) {
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
            }
            if (normalize) {
                if (foundConst) {
                    name = name.substring(name.indexOf("const") + 5);
                }
                int template = name.indexOf('<');
                if (template >= 0) {
                    name = name.substring(0, template);
                }
            }
            return name.trim();
        }

        public LinkedList<Info> get(String cppName) {
            String key = sort(cppName, false);
            LinkedList<Info> infoList = super.get(key);
            if (infoList == null) {
                key = sort(cppName, true);
                infoList = super.get(key);
            }
            if (infoList == null && parent != null) {
                infoList = parent.get(cppName);
            }
            if (infoList == null) {
                infoList = new LinkedList<Info>();
            }
            return infoList;
        }

        public Info get(int index, String cppName) {
            LinkedList<Info> infoList = get(cppName);
            return infoList.size() > 0 ? infoList.get(index) : null;
        }

        public Info getFirst(String cppName) {
            LinkedList<Info> infoList = get(cppName);
            return infoList.size() > 0 ? infoList.getFirst() : null;
        }

        public InfoMap put(int index, Info info) {
            for (String cppName : info.cppNames != null ? info.cppNames : new String[] { null }) {
                String key = sort(cppName, false);
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

    static class Token implements Cloneable, Comparable<Token> {
        Token() { }
        Token(int type, String value) { this.type = type; this.value = value; }

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

        @Override public Token clone() {
            Token t = new Token();
            t.file = file;
            t.lineNumber = lineNumber;
            t.type = type;
            t.spacing = spacing;
            t.value = value;
            return t;
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

        boolean preprocess = true;
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
                if (info != null && info.macroParams != null && info.text != null) {
                    if (info.macroParams.length > 0 && (index + 1 >= array.length
                            || !array[index + 1].match('('))) {
                        return;
                    }
                    ArrayList<Token> tokens = new ArrayList<Token>();
                    for (int i = 0; i < index; i++) {
                        tokens.add(array[i]);
                    }
                    ArrayList<Token>[] args = new ArrayList[info.macroParams.length];
                    int count = 0, count2 = 0;
                    for (index += 2; index < array.length; index++) {
                        Token token = array[index];
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
                    try {
                        Tokenizer tokenizer = new Tokenizer(info.text);
                        Token token;
                        while (!(token = tokenizer.nextToken()).isEmpty()) {
                            boolean foundArg = false;
                            for (int i = 0; i < info.macroParams.length; i++) {
                                if (info.macroParams[i].equals(token.value)) {
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
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    for (index += 1; index < array.length; index++) {
                        tokens.add(array[index]);
                    }
                    array = tokens.toArray(new Token[tokens.size()]);
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
            int k = preprocess ? preprocess(index, i) : index + i;
            return k < array.length ? array[k] : Token.EOF;
        }
        Token next() {
            index = preprocess ? preprocess(index, 1) : index + 1;
            return index < array.length ? array[index] : Token.EOF;
        }
    }

    static class Context implements Cloneable {
        String namespace = null;
        Type group = null;
        Declarator[] variables = null;
        TemplateMap templateMap = null;

        @Override public Context clone() {
            Context c = new Context();
            c.namespace = namespace;
            c.group = group;
            c.variables = variables;
            c.templateMap = templateMap;
            return c;
        }
    }


    public Parser(Properties properties, InfoMap infoMap) {
        this.properties = properties;
        this.infoMap = infoMap;
    }

    Properties properties = null;
    InfoMap infoMap = null;
    TokenIndexer tokens = null;

    static String rescan(String lines, String spacing) {
        String text = "";
        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            text += spacing + scanner.nextLine();
            int newline = spacing.lastIndexOf('\n');
            spacing = newline >= 0 ? spacing.substring(newline) : "\n";
        }
        return text;
    }


    String vectors() {
        String definitions = "";
        LinkedList<Info> infoList = infoMap.get("std::vector");
        for (Info info : infoList) {
            if (info.genericArgs == null || info.genericArgs.length == 0 ||
                    info.pointerTypes == null || info.pointerTypes.length == 0) {
                continue;
            }
            String cppType = info.genericArgs[0];
            String cppVectorType = "std::vector<" + cppType + ">";
            String javaVectorType = info.pointerTypes[0];
            String annotations = "@ByRef ";
            String javaType = cppType;
            Info info2 = infoMap.getFirst(cppType);
            if (info2 != null) {
                if (info2.pointerTypes != null && info2.pointerTypes.length > 0) {
                    javaType = info2.pointerTypes[0];
                } else if (info2.valueTypes != null && info2.valueTypes.length > 0) {
                    javaType = info2.valueTypes[0];
                }
                int n = javaType.lastIndexOf(' ');
                if (n >= 0) {
                    annotations = javaType.substring(0, n + 1);
                    javaType = javaType.substring(n + 1);
                }
            }
            infoMap.put(new Info(cppVectorType).pointerTypes(javaVectorType));
            definitions = "\n" +
                    "@Name(\"" + cppVectorType + "\") public static class " + javaVectorType + " extends Pointer {\n" +
                    "    static { Loader.load(); }\n" +
                    "    public " + javaVectorType + "(Pointer p) { super(p); }\n" +
                    "    public " + javaVectorType + "(" + javaType + " ... array) { this(array.length); put(array); }\n" +
                    "    public " + javaVectorType + "()       { allocate();  }\n" +
                    "    public " + javaVectorType + "(long n) { allocate(n); }\n" +
                    "    private native void allocate();\n" +
                    "    private native void allocate(@Cast(\"size_t\") long n);\n\n" +

                    "    public native long size();\n" +
                    "    public native void resize(@Cast(\"size_t\") long n);\n\n" +

                    "    @Index public native " + annotations + javaType + " get(@Cast(\"size_t\") long i);\n" +
                    "    public native " + javaVectorType + " put(@Cast(\"size_t\") long i, " + javaType + " value);\n\n" +

                    "    public " + javaVectorType + " put(" + javaType + " ... array) {\n" +
                    "        if (size() < array.length) { resize(array.length); }\n" +
                    "        for (int i = 0; i < array.length; i++) {\n" +
                    "            put(i, array[i]);\n" +
                    "        }\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "}\n";
        }
        return definitions;
    }


    static class TemplateMap extends LinkedHashMap<String,String> {
        TemplateMap(TemplateMap defaults) {
            this.defaults = defaults;
        }
        LinkedHashMap<String,String> defaults = null;

        String get(String key) {
            String value = super.get(key);
            if (value == null && defaults != null) {
                return defaults.get(key);
            } else {
                return value;
            }
        }
    }

    TemplateMap template(Context context) throws Exception {
        if (!tokens.get().match(Token.TEMPLATE)) {
            return context.templateMap;
        }
        TemplateMap map = new TemplateMap(context.templateMap);

        tokens.next().expect('<');
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.IDENTIFIER)) {
                map.put(tokens.next().expect(Token.IDENTIFIER).value, null);
                token = tokens.next();
            }
            if (!token.match(',', '>')) {
                // ignore default argument
                int count = 0;
                for (token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
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


    static class Type {
        Type() { }
        Type(String name) { cppName = javaName = name; }

        boolean anonymous = false, constValue = false, destructor = false,
                operator = false, simpleType = false, staticMember = false;
        String annotations = "", cppName = "", javaName = "";

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
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match("::")) {
                type.cppName += token;
            } else if (token.match('<')) {
                type.cppName += token;
                for (token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                    Type type2 = type(context);
                    type.cppName += type2.cppName;
                    for (token = tokens.get(); !token.match(Token.EOF, ',', '>'); token = tokens.next()) {
                        type.cppName += token;
                    }
                    type.cppName += token;
                    if (token.expect(',', '>').match('>')) {
                        break;
                    }
                }
            } else if (token.match(Token.CONST)) {
                type.constValue = true;
            } else if (token.match('~')) {
                type.destructor = true;
            } else if (token.match(Token.STATIC)) {
                type.staticMember = true;
            } else if (token.match(Token.OPERATOR)) {
                break;
            } else if (token.match(Token.TYPEDEF, Token.USING, Token.ENUM, Token.EXPLICIT, Token.EXTERN,
                    Token.CLASS, Token.STRUCT, Token.UNION, Token.INLINE, Token.VIRTUAL)) {
                continue;
            } else if (token.match((Object[])InfoMap.simpleTypes)) {
                type.cppName += token.value + " ";
                type.simpleType = true;
            } else if (token.match(Token.IDENTIFIER)) {
                Info info = infoMap.getFirst(token.value);
                if (info != null && info.annotations != null) {
                    for (String s : info.annotations) {
                        type.annotations += s + " ";
                    }
                } else if (type.cppName.length() == 0 || type.cppName.endsWith("::")) {
                    type.cppName += token.value;
                } else {
                    info = infoMap.getFirst(tokens.get(1).value);
                    if ((info != null && info.annotations != null) ||
                            !tokens.get(1).match('*', '&', Token.IDENTIFIER, Token.CONST)) {
                        // we probably reached a variable or function name identifier
                        break;
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
        type.cppName = type.cppName.trim();
        boolean pointer = false;
        boolean reference = false;
        if ("...".equals(tokens.get().value)) {
            tokens.next();
            return null;
        } else if (type.operator) {
            for (Token token = tokens.get(); !token.match(Token.EOF, '('); token = tokens.next()) {
                type.cppName += token;
            }
            pointer = type.cppName.endsWith("*");
            reference = type.cppName.endsWith("&");

            type.annotations += "@Name(\"operator " + type.cppName + "\") ";
            if (type.constValue) {
                type.annotations += "@Const ";
            }
            if (pointer || reference) {
                type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
            }
        }
        if (context.templateMap != null && context.templateMap.get(type.cppName) != null) {
            type.cppName = context.templateMap.get(type.cppName);
        }
        type.javaName = type.cppName;
        Info info = infoMap.getFirst(type.cppName);
        boolean valueType = false;
        if (info != null) {
            if (!pointer && !reference && info.valueTypes != null && info.valueTypes.length > 0) {
                type.javaName = info.valueTypes[0];
                valueType = true;
            } else if (info.pointerTypes != null && info.pointerTypes.length > 0) {
                type.javaName = info.pointerTypes[0];
            }
        }
        if (type.operator && !valueType) {
            if (!pointer && !reference) {
                type.annotations += "@ByVal ";
            } else if (!pointer && reference) {
                type.annotations += "@ByRef ";
            }
        }
        return type;
    }

    static class Declarator {
        Type type;
        int infoNumber = 0, indices = 0;
        boolean constPointer = false;
        String cppName = "", javaName = "", convention = "", definitions = "", parameters = "";
    }

    Declarator declarator(Context context, String defaultName, int infoNumber,
            int varNumber, boolean arrayAsPointer, boolean pointerAsArray) throws Exception {
        boolean isTypedef = tokens.get().match(Token.TYPEDEF);
        boolean isUsing = tokens.get().match(Token.USING);
        Declarator dcl = new Declarator();
        Type type = dcl.type = type(context);
        if (type == null) {
            return null;
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
                cast += "*";
            } else if (token.match('&')) {
                reference = true;
                cast += "*";
            } else if (token.match(Token.CONST)) {
                dcl.constPointer = true;
                cast += "const";
            } else {
                break;
            }
        }

        int dims[] = new int[256];
        int indirections2 = 0;
        dcl.cppName = defaultName;
        if (tokens.get().match('(')) {
            // probably a function pointer declaration
            while (tokens.get().match('(')) {
                tokens.next();
            }
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                if (token.match(Token.IDENTIFIER)) {
                    dcl.cppName = token.value;
                } else if (token.match('*')) {
                    indirections2++;
                    dcl.convention = dcl.cppName;
                    dcl.cppName = defaultName;
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
            dcl.cppName = "";
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                if (token.match("::")) {
                    dcl.cppName += token;
                } else if (token.match(Token.OPERATOR)) {
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
        Info info = infoMap.getFirst(prefix + type.cppName);
        String ns = "";
        while (context.namespace != null && info == null && !ns.equals(context.namespace)) {
            int i = context.namespace.indexOf("::", ns.length() + 2);
            ns = i < 0 ? context.namespace : context.namespace.substring(0, i);
            info = infoMap.getFirst(prefix + ns + "::" + type.cppName);
        }
        if (info != null) {
            valueType = info.valueTypes != null && ((type.constValue && reference) ||
                    (indirections == 0 && !reference) || info.pointerTypes == null);
            needCast |= info.cast;
            implicitConst = info.cppNames[0].startsWith("const ");
            infoLength = valueType ? info.valueTypes.length :
                    info.pointerTypes != null ? info.pointerTypes.length : 1;
            dcl.infoNumber = infoNumber < 0 ? 0 : infoNumber % infoLength;
            type.javaName = valueType ? info.valueTypes[dcl.infoNumber] :
                    info.pointerTypes != null ? info.pointerTypes[dcl.infoNumber] : type.javaName;
            if (ns.length() > 0) {
                cast = ns + "::" + cast;
            }
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
                if (type.cppName.equals("void")) {
                    needCast = true;
                }
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

        dcl.javaName = dcl.cppName;
        info = infoMap.getFirst(dcl.cppName);
        if (defaultName == null && info != null && info.javaNames != null && info.javaNames.length > 0) {
            type.annotations += "@Name(\"" + dcl.cppName + "\") ";
            dcl.javaName = info.javaNames[0];
        }

        Parameters params = parameters(context, infoNumber);
        if (params != null) {
            dcl.infoNumber = Math.max(dcl.infoNumber, params.infoNumber);
            if (params.definitions.length() > 0) {
                dcl.definitions += params.definitions + "\n";
            }
            if (indirections2 == 0) {
                dcl.parameters = params.list;
            } else {
                String functionType = isTypedef ? dcl.javaName :
                        Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1) + params.signature;
                if (infoNumber <= params.infoNumber) {
                    dcl.definitions +=
                            "public static class " + functionType + " extends FunctionPointer {\n" +
                            "    static { Loader.load(); }\n" +
                            "    public    " + functionType + "(Pointer p) { super(p); }\n" +
                            "    protected " + functionType + "() { allocate(); }\n" +
                            "    private native void allocate();\n" +
                            "    public native " + type.annotations + type.javaName + " call" + params.list + ";\n" +
                            "}\n";
                }
                type.annotations = "";
                type.javaName = functionType;
                dcl.parameters = "";
            }
        }
        return dcl;
    }

    String commentBefore() throws Exception {
        String comment = "";
        tokens.preprocess = false;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            if (token.value.length() <= 3 || token.value.charAt(3) != '<') {
                comment += token.spacing + token.value;
            }
        }
        tokens.preprocess = true;
        return comment;
    }

    String commentAfter() throws Exception {
        String comment = "";
        tokens.preprocess = false;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            if (token.value.length() > 3 && token.value.charAt(3) == '<') {
                comment += (comment.length() > 0 ? " * " : "/**") + token.value.substring(4);
            }
        }
        if (comment.length() > 0) {
            if (!comment.endsWith("*/")) {
                comment += " */";
            }
            comment += "\n";
        }
        tokens.preprocess = true;
        return comment;
    }

    Declaration attribute() throws Exception {
        if (!tokens.get().match(Token.IDENTIFIER)) {
            return null;
        }
        Declaration decl = new Declaration();
        decl.text = tokens.get().spacing;
        if (!tokens.next().match('(')) {
            return decl;
        }

        int count = 1;
        tokens.preprocess = false;
        for (Token token = tokens.next(); !token.match(Token.EOF) && count > 0; token = tokens.next()) {
            if (token.match('(')) {
                count++;
            } else if (token.match(')')) {
                count--;
            }
        }
        tokens.preprocess = true;
        return decl;
    }

    String body() throws Exception {
        if (!tokens.get().match('{')) {
            return null;
        }

        int count = 1;
        tokens.preprocess = false;
        for (Token token = tokens.next(); !token.match(Token.EOF) && count > 0; token = tokens.next()) {
            if (token.match('{')) {
                count++;
            } else if (token.match('}')) {
                count--;
            }
        }
        tokens.preprocess = true;
        return "";
    }


    static class Parameters {
        int infoNumber = 0;
        String list = "", definitions = "", signature = "", names = "";
    }

    Parameters parameters(Context context, int infoNumber) throws Exception {
        if (!tokens.get().match('(')) {
            return null;
        }

        int count = 0;
        Parameters params = new Parameters();
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
            Declarator dcl = declarator(context, "arg" + count++, infoNumber, 0, true, false);
            if (dcl != null && !dcl.type.javaName.equals("void")) {
                params.infoNumber = Math.max(params.infoNumber, dcl.infoNumber);
                params.list += (count > 1 ? "," : "") + spacing + dcl.type.annotations + dcl.type.javaName + " " + dcl.javaName;
                params.definitions += dcl.definitions;
                params.signature += '_';
                for (char c : dcl.type.javaName.substring(dcl.type.javaName.lastIndexOf(' ') + 1).toCharArray()) {
                    if (Character.isDigit(c) || Character.isLetter(c) || c == '_') {
                        params.signature += c;
                    }
                }
                params.names += (count > 1 ? ", " : "") + dcl.javaName;
                if (dcl.javaName.startsWith("arg")) {
                    try {
                        count = Integer.parseInt(dcl.javaName.substring(3)) + 1;
                    } catch (NumberFormatException e) { /* don't care if not int */ }
                }
            }
            if (!tokens.get().match(',', ')')) {
                // output default argument as a comment
                params.list += "/*" + tokens.get();
                int count2 = 0;
                for (token = tokens.next(), token.spacing = ""; !token.match(Token.EOF); token = tokens.next()) {
                    if (count2 == 0 && token.match(',', ')')) {
                        break;
                    } else if (token.match('(')) {
                        count2++;
                    } else if (token.match(')')) {
                        count2--;
                    }
                    params.list += token.spacing + token;
                }
                params.list += "*/";
            }
            if (tokens.get().expect(',', ')').match(',')) {
                tokens.next();
            }
        }
        return params;
    }

    Declaration function(Context context) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public native ";
        Type type = type(context);
        Parameters params = parameters(context, 0);
        Declarator dcl = new Declarator();
        Declaration decl = new Declaration();
        if (type.javaName.length() == 0) {
            // not a function, probably an attribute
            tokens.index = backIndex;
            return null;
        } else if (context.group == null && params != null) {
            // this is a constructor definition, skip over
            body();
            decl.text = spacing;
            return decl;
        } else if ((type.equals(context.group) || type.operator) && params != null) {
            // this is a constructor, destructor, or cast operator
            decl.constructor = !type.destructor && !type.operator;
            dcl.cppName = type.cppName;
            dcl.javaName = type.operator ? "as" + type.javaName : type.javaName;
            dcl.parameters = params.list;
            dcl.definitions = params.definitions;
        } else {
            tokens.index = backIndex;
            dcl = declarator(context, null, 0, 0, false, false);
            type = dcl.type;
        }

        if (type.javaName.length() == 0 || dcl.parameters.length() == 0) {
            tokens.index = backIndex;
            return null;
        } else if (type.staticMember || context.group == null) {
            modifiers = "public static native ";
        }

        String definitions = "";
        LinkedList<Info> infoList = infoMap.get(dcl.cppName);
        if (infoList.size() == 0) {
            infoList.add(null);
        }
        for (Info info : infoList) {
            boolean template = false;
            if (info != null && info.genericArgs != null && context.templateMap != null) {
                int count = 0;
                for (Map.Entry<String,String> e : context.templateMap.entrySet()) {
                    if (count < info.genericArgs.length && e.getValue() == null) {
                        context.templateMap.put(e.getKey(), info.genericArgs[count++]);
                        template = true;
                    }
                }
            }

            LinkedList<Declarator> prevDcl = new LinkedList<Declarator>();
            for (int n = -1; n < Integer.MAX_VALUE; n++) {
                tokens.index = backIndex;
                if (decl.constructor || type.destructor || type.operator) {
                    type = type(context);
                    params = parameters(context, n);
                    dcl.cppName = type.cppName;
                    dcl.javaName = type.operator ? "as" + type.javaName : type.javaName;
                    dcl.parameters = params.list;
                    dcl.definitions = params.definitions;
                    if (tokens.get().match(':')) {
                        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                            if (token.match('{', ';')) {
                                break;
                            }
                        }
                    }
                } else {
                    dcl = declarator(context, null, n, 0, false, false);
                    type = dcl.type;
                }
                boolean found = false;
                for (Declarator d : prevDcl) {
                    found |= /* dcl.javaType.equals(d.javaType) && */ dcl.parameters.equals(d.parameters);
                }
                if (found && n > 0) {
                    break;
                } else if (dcl.javaName.length() > 0 && !found && !type.destructor) {
                    if (context.namespace != null && context.group == null) {
                        decl.text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (decl.constructor) {
                        decl.text += "public " + dcl.javaName + dcl.parameters + " { allocate" + params.names + "; }\n" +
                                     "private native void allocate" + dcl.parameters + ";\n";
                    } else {
                        decl.text += modifiers + type.annotations + type.javaName + " " + dcl.javaName + dcl.parameters + ";\n";
                    }
                    definitions += dcl.definitions;
                }
                prevDcl.add(dcl);
            }
            while (attribute() != null) { }
            if (tokens.get().match('{')) {
                body();
            } else {
                if (tokens.get().match('=')) {
                    tokens.next().expect("0");
                    tokens.next().expect(';');
                }
                tokens.next();
            }

            if (!template) {
                break;
            }
        }
        String comment = commentAfter();
        decl.text = rescan(definitions + comment + decl.text, spacing);
        return decl;
    }

    Declaration variable(Context context) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public static native ";
        String setterType = "void ";
        Declarator dcl = declarator(context, null, 0, 0, false, true);
        String name = dcl.javaName;
        if (name == null || !tokens.get().match('[', '=', ',', ':', ';')) {
            tokens.index = backIndex;
            return null;
        } else if (!dcl.type.staticMember && context.group != null) {
            modifiers = "public native ";
            setterType = context.group.javaName + " ";
        }

        Declaration decl = new Declaration();
        String definitions = "";
        for (Declarator metadcl : context.variables != null ? context.variables : new Declarator[] { null }) {
            for (int n = 0; n < Integer.MAX_VALUE; n++) {
                tokens.index = backIndex;
                dcl = declarator(context, null, -1, n, false, true);
                if (dcl == null) {
                    break;
                }
                name = dcl.javaName;
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
                        decl.text += "@Name({\"" + metadcl.cppName + "\", \"." + dcl.cppName + "\"}) ";
                        name = metadcl.javaName + "_" + dcl.javaName;
                    }
                    if (dcl.type.constValue) {
                        decl.text += "@MemberGetter ";
                    }
                    decl.text += modifiers + dcl.type.annotations + dcl.type.javaName + " " + name + "(" + indices + ");";
                    if (!dcl.type.constValue) {
                        if (indices.length() > 0) {
                            indices += ", ";
                        }
                        decl.text += " " + modifiers + setterType + name + "(" + indices + dcl.type.javaName + " " + name + ");";
                    }
                    decl.text += "\n";
                    definitions += dcl.definitions;
                }
                if (dcl.indices > 0) {
                    // in the case of arrays, also add a pointer accessor
                    tokens.index = backIndex;
                    dcl = declarator(context, null, -1, n, true, false);
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
                        decl.text += "@Name({\"" + metadcl.cppName + "\", \"." + dcl.cppName + "\"}) ";
                        name = metadcl.javaName + "_" + dcl.javaName;
                    }
                    decl.text += "@MemberGetter " + modifiers + dcl.type.annotations + dcl.type.javaName + " " + name + "(" + indices + ");\n";
                }
            }
        }
        String comment = commentAfter();
        decl.text = rescan(definitions + comment + decl.text, spacing);
        return decl;
    }

    Declaration macro(Context context) throws Exception {
        if (!tokens.get().match('#')) {
            return null;
        }
        tokens.preprocess = false;
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

        Declaration decl = new Declaration();
        if (keyword.match(Token.DEFINE) && beginIndex + 1 < endIndex) {
            tokens.index = beginIndex;
            String macroName = tokens.get().value;
            Token first = tokens.next();
            boolean hasArgs = first.spacing.length() == 0 && first.match('(');
            LinkedList<Info> infoList = infoMap.get(macroName);
            if (infoList.size() == 0) {
                infoList.add(null);
            }
            for (Info info : infoList) {
                if (hasArgs && info == null) {
                    // save declaration for expansion
                    info = new Info(macroName).macroParams(new String[endIndex - tokens.index]).text("");
                    int count = 0;
                    for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                        if (token.match(Token.IDENTIFIER)) {
                            info.macroParams[count++] = token.value;
                        } else if (token.match(')')) {
                            break;
                        }
                    }
                    info.macroParams = Arrays.copyOf(info.macroParams, count);
                    for (Token token = tokens.next(); tokens.index < endIndex; token = tokens.next()) {
                        info.text += token.spacing + token;
                    }
                    infoMap.put(info);
                } else if (info != null && info.text == null &&
                        info.genericArgs != null && info.genericArgs.length > (hasArgs ? 0 : 1)) {
                    // declare as a static native method
                    LinkedList<Declarator> prevDcl = new LinkedList<Declarator>();
                    for (int n = -1; n < Integer.MAX_VALUE; n++) {
                        int count = 1;
                        tokens.index = beginIndex + 2;
                        String params = "(";
                        for (Token token = tokens.get(); hasArgs && tokens.index < endIndex
                                && count < info.genericArgs.length; token = tokens.next()) {
                            if (token.match(Token.IDENTIFIER)) {
                                String type = info.genericArgs[count];
                                String name = token.value;
                                if (name.equals("...")) {
                                    name = "arg" + count;
                                }
                                params += type + " " + name;
                                if (++count < info.genericArgs.length) {
                                    params += ", ";
                                }
                            } else if (token.match(')')) {
                                break;
                            }
                        }
                        while (count < info.genericArgs.length) {
                            String type = info.genericArgs[count];
                            String name = "arg" + count;
                            params += type + " " + name;
                            if (++count < info.genericArgs.length) {
                                params += ", ";
                            }
                        }
                        params += ")";

                        TokenIndexer t = tokens;
                        tokens = new TokenIndexer(infoMap, new Tokenizer(info.genericArgs[0] + " " + macroName + params).tokenize());
                        Declarator dcl = declarator(context, null, n, 0, false, false);
                        tokens = t;

                        for (int i = 0; i < info.cppNames.length; i++) {
                            if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                                macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                                break;
                            }
                        }

                        boolean found = false;
                        for (Declarator d : prevDcl) {
                            found |= /* dcl.javaType.equals(d.javaType) && */ dcl.parameters.equals(d.parameters);
                        }
                        if (found && n > 0) {
                            break;
                        } else if (!found) {
                            decl.text += "public static native " + dcl.type.annotations + dcl.type.javaName + " " + macroName + dcl.parameters + ";\n";
                        }
                        prevDcl.add(dcl);
                    }
                } else if (info == null || (info.text == null &&
                        (info.genericArgs == null || info.genericArgs.length == 1))) {
                    // declare as a static final variable
                    String value = "";
                    String type = "int";
                    String cat = "";
                    tokens.index = beginIndex + 1;
                    Token prevToken = new Token();
                    boolean translate = true;
                    for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
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
                        if (info.genericArgs != null) {
                            TokenIndexer t = tokens;
                            tokens = new TokenIndexer(infoMap, new Tokenizer(info.genericArgs[0]).tokenize());
                            Declarator dcl = declarator(context, null, -1, 0, false, true);
                            tokens = t;
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
                    if (!translate) {
                        decl.text += "public static native @MemberGetter " + type + " " + macroName + "();\n";
                        value = " " + macroName + "()";
                    } else {
                        while (tokens.get(endIndex - tokens.index - 1).match(Token.COMMENT)) {
                            endIndex--;
                        }
                        for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                            value += token.spacing + token + (tokens.index + 1 < endIndex ? cat : "");
                        }
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

            if (decl.text.length() > 0) {
                tokens.index = endIndex;
                String comment = commentAfter();
                decl.text = rescan(comment + decl.text, spacing);
            }
        }

        if (decl.text.length() == 0) {
            // output whatever we did not process as comment
            tokens.index = beginIndex;
            while (tokens.get(endIndex - tokens.index - 1).match(Token.COMMENT)) {
                endIndex--;
            }
            int n = spacing.lastIndexOf('\n') + 1;
            decl.text += "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
            for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                decl.text += token.match("\n") ? "\n// " : token.spacing + token;
            }
            String comment = commentAfter();
            decl.text = spacing.substring(0, n) + comment + decl.text;
        }
        tokens.preprocess = true;
        return decl;
    }

    Declaration typedef(Context context) throws Exception {
        if (!tokens.get().match(Token.TYPEDEF)) {
            return null;
        }
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        Declarator dcl = declarator(context, null, 0, 0, true, false);
        tokens.next();

        Declaration decl = new Declaration();
        int endIndex = tokens.index;
        String name = dcl.cppName, typeName = dcl.type.cppName;
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        if (dcl.definitions.length() > 0) {
            // a function pointer or something
            decl.text = dcl.definitions;
            infoMap.put(new Info(name).valueTypes(dcl.javaName));
        } else if (typeName.equals("void")) {
            Info info = infoMap.getFirst(name);
            if (info == null || !info.skip) {
                decl.text = "@Opaque public static class " + dcl.javaName + " extends Pointer {\n" +
                            "    public " + dcl.javaName + "() { }\n" +
                            "    public " + dcl.javaName + "(Pointer p) { super(p); }\n" +
                            "}";
            }
        } else {
            Info info = infoMap.getFirst(typeName);
            info = info != null ? info.clone().cppNames(name) : new Info(name);
            if (info.valueTypes == null) {
                info.valueTypes(typeName);
            }
            if (info.pointerTypes == null) {
                info.pointerTypes(typeName);
            }
            infoMap.put(info.cast(true));

            tokens.index = backIndex;
            info = infoMap.getFirst(tokens.next().value);
            if (info != null && info.templateParams != null && tokens.next().match('<')) {
                info.genericArgs = new String[info.templateParams.length];
                TemplateMap map = new TemplateMap(context.templateMap);
                context = context.clone();
                context.templateMap = map;
                int count = 0;
                for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                    Type type = type(context);
                    info.genericArgs[count] = type.cppName;
                    map.put(info.templateParams[count], type.cppName);
                    count++;
                    if (tokens.get().expect(',', '>').match('>')) {
                        tokens.next();
                        break;
                    }
                }
                info.pointerTypes(dcl.javaName);
                TokenIndexer t = tokens;
                tokens = new TokenIndexer(infoMap, new Tokenizer(info.text).tokenize());
                decl = group(context);
                tokens = t;
            }
            tokens.index = endIndex;
        }

        String comment = commentAfter();
        decl.text = rescan(comment + decl.text, spacing);
        return decl;
    }

    Declaration using(Context context) throws Exception {
        if (!tokens.get().match(Token.USING)) {
            return null;
        }
        String spacing = tokens.get().spacing;
        Declarator dcl = declarator(context, null, 0, 0, true, false);
        tokens.next();

        Declaration decl = new Declaration();
        String name = dcl.type.cppName.substring(dcl.type.cppName.lastIndexOf("::") + 2);
        Info info = infoMap.getFirst(dcl.type.cppName);
        info = info != null ? info.clone() : new Info(name);
        if (info.valueTypes == null) {
            info.valueTypes(info.cppNames);
        }
        if (info.pointerTypes == null) {
            info.pointerTypes(info.cppNames);
        }
        infoMap.put(info.cppNames(name));

        String comment = commentAfter();
        decl.text = rescan(comment + dcl.definitions, spacing);
        return decl;
    }

    Declaration group(Context context) throws Exception {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        boolean isTypedef = tokens.get().match(Token.TYPEDEF);
        boolean foundGroup = false, accessible = true;
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.CLASS, Token.STRUCT, Token.UNION)) {
                foundGroup = true;
                accessible = !token.match(Token.CLASS);
                break;
            } else if (!token.match(Token.IDENTIFIER)) {
                break;
            }
        }
        if (!foundGroup) {
            tokens.index = backIndex;
            return null;
        }

        tokens.next().expect(Token.IDENTIFIER, '{');
        if (isTypedef && tokens.get(1).match('*')) {
            tokens.next();
        }
        if (!tokens.get().match('{') && tokens.get(1).match(Token.IDENTIFIER)
                && (isTypedef || !tokens.get(2).match(';'))) {
            tokens.next();
        }
        Type type = type(context);
        Type parent = new Type("Pointer");
        Declaration decl = new Declaration(type.annotations);
        String name = type.cppName;
        boolean anonymous = !isTypedef && name.length() == 0;
        if (name.length() > 0 && tokens.get().match(':')) {
            for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                boolean exposed = false;
                if (token.match(Token.VIRTUAL)) {
                    continue;
                } else if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC)) {
                    exposed = token.match(Token.PUBLIC);
                    tokens.next();
                }
                Type t = type(context);
                if (exposed) {
                    parent = t;
                }
                if (tokens.get().expect(',', '{').match('{')) {
                    break;
                }
            }
        }
        if (!tokens.get().match('{', ';')) {
            tokens.index = backIndex;
            return null;
        }
        int startIndex = tokens.index;
        ArrayList<Declarator> variables = new ArrayList<Declarator>();
        if (body() != null && !tokens.get().match(';')) {
            if (isTypedef) {
                for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                    if (token.match(';')) {
                        decl.text += token.spacing;
                        break;
                    } else {
                        name = type.cppName = type.javaName = token.value;
                    }
                }
            } else {
                int index = tokens.index - 1;
                for (int n = 0; n < Integer.MAX_VALUE; n++) {
                    tokens.index = index;
                    Declarator dcl = declarator(context, null, -1, n, false, true);
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
        Info info = infoMap.getFirst(name);
        if (info != null && info.skip) {
            decl.text = "";
            return decl;
        } else if (info != null && info.pointerTypes != null && info.pointerTypes.length > 0) {
            name = info.pointerTypes[0];
        }
        tokens.index = startIndex;
        if (name.length() > 0 && tokens.get().match(';')) {
            // incomplete type (forward declaration)
            tokens.next();
            if (info == null || !info.complete) {
                if (info != null && info.parent != null) {
                    parent.javaName = info.parent;
                }
                infoMap.put(new Info(type.cppName).complete(true));
                if (context.namespace != null) {
                    decl.text += "@Namespace(\"" + context.namespace + "\") ";
                }
                decl.text += "@Opaque public static class " + name + " extends " + parent.javaName + " {\n" +
                             "    public " + name + "() { }\n" +
                             "    public " + name + "(Pointer p) { super(p); }\n" +
                             "}";
            }
            String comment = commentAfter();
            decl.text = rescan(comment + decl.text, spacing);
            return decl;
        } else if (info == null) {
            infoMap.put(new Info(context.namespace == null ? name : context.namespace + "::" + name));
        }

        Context context2 = context.clone();
        context2.namespace = context.namespace == null ? name : context.namespace + "::" + name;
        if (!anonymous) {
            context2.group = type;
        }
        if (variables.size() > 0) {
            context2.variables = variables.toArray(new Declarator[variables.size()]);
        }
        String declarations = "";
        boolean implicitConstructor = true, defaultConstructor = false;
        if (tokens.get().match('{')) {
            tokens.next();
        }
        for (Token token = tokens.get(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC) && tokens.next().match(':')) {
                accessible = token.match(Token.PUBLIC);
                tokens.next();
            }
            Declaration decl2 = declaration(context2);
            if (decl2.constructor) {
                implicitConstructor = false;
                defaultConstructor |= decl2.text.contains("allocate()");
            }
            if (accessible) {
                declarations += decl2.text;
            }
        }

        if (!anonymous) {
            decl.text += spacing;
            if (context.namespace != null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            String templateArgs = "";
            if (info != null && info.genericArgs != null && context.templateMap != null) {
                int count = 0;
                templateArgs += '<';
                for (Map.Entry<String,String> e : context.templateMap.entrySet()) {
                    if (count < info.genericArgs.length && e.getValue() == null) {
                        context.templateMap.put(e.getKey(), info.genericArgs[count]);
                    }
                    if (count++ > 0) {
                        templateArgs += ',';
                    }
                    templateArgs += context.templateMap.get(e.getKey());
                }
                templateArgs += '>';
            }
            if (!name.equals(type.cppName) || templateArgs.length() > 0) {
                decl.text += "@Name(\"" + type.cppName + templateArgs + "\") ";
            }
            if (!implicitConstructor) {
                decl.text += "@NoOffset ";
            }
            if (info != null && info.parent != null) {
                parent.javaName = info.parent;
            }
            decl.text += "public static class " + name + " extends " + parent.javaName + " {\n" +
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
                if (!defaultConstructor) {
                    decl.text += "    public " + name + "() { }\n";
                }
                decl.text += "    public " + name + "(Pointer p) { super(p); }\n";
            }
        }
        String comment = commentBefore();
        decl.text += declarations + comment;
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
        if ((info == null || info.genericArgs == null) && context.templateMap != null) {
            // save text for specialization in typedef or parameters
            info = new Info(type.cppName).templateParams(new String[context.templateMap.size()]).text("");
            int count = 0;
            for (String key : context.templateMap.keySet()) {
                if (count < info.templateParams.length) {
                    info.templateParams[count++] = key;
                }
            }
            int endIndex = tokens.index;
            tokens.index = backIndex;
            tokens.preprocess = false;
            for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                info.text += token.spacing + token;
            }
            tokens.preprocess = true;
            infoMap.putFirst(info);
            decl.text = "";
        }
        return decl;
    }

    Declaration enumeration(Context context) throws Exception {
        int backIndex = tokens.index;
        String enumSpacing = tokens.get().spacing;
        boolean isTypedef = tokens.get().match(Token.TYPEDEF);
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
            return null;
        }

        if (isTypedef && !tokens.get(1).match('{') && tokens.get(2).match(Token.IDENTIFIER)) {
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
            return null;
        }
        for (Token token = tokens.next(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            Declaration macroDecl = macro(context);
            if (macroDecl != null) {
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
                    if (!translate) {
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
        infoMap.put(new Info(name).valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]").cast(true));
        tokens.next();
        decl.text += macroText + comment;
        return decl;
    }

    Declaration namespace(Context context) throws Exception {
        if (!tokens.get().match(Token.NAMESPACE)) {
            return null;
        }
        Declaration decl = new Declaration();
        String name = tokens.next().expect(Token.IDENTIFIER).value;
        tokens.next().expect('{');
        tokens.next();

        context = context.clone();
        context.namespace = context.namespace == null ? name : context.namespace + "::" + name;
        while (!tokens.get().match(Token.EOF, '}')) {
            decl.text += declaration(context).text;
        }
        decl.text += tokens.get().spacing;
        tokens.next();
        return decl;
    }

    Declaration extern(Context context) throws Exception {
        if (!tokens.get().match(Token.EXTERN) || !tokens.get(1).match(Token.STRING)) {
            return null;
        }
        Declaration decl = new Declaration();
        tokens.next().expect("\"C\"");
        if (!tokens.next().match('{')) {
            return decl;
        }
        tokens.next();

        while (!tokens.get().match(Token.EOF, '}')) {
            decl.text += declaration(context).text;
        }
        tokens.next();
        return decl;
    }


    class Declaration {
        Declaration() { }
        Declaration(String text) { this.text = text; }

        boolean constructor = false;
        String text = "";
    }

    Declaration declaration(Context context) throws Exception {
        while (tokens.get().match(';') && !tokens.get().match(Token.EOF)) {
            tokens.next();
        }
        if (context == null) {
            context = new Context();
        }
        String comment = commentBefore();
        Token token = tokens.get();
        String spacing = token.spacing;
        TemplateMap map = template(context);
        if (map != context.templateMap) {
            token = tokens.get();
            if (token.spacing.length() > 0) {
                token.spacing = token.spacing.substring(1);
            }
            context = context.clone();
            context.templateMap = map;
            comment += spacing;
        }
        Declaration decl = null;
        if ((decl = macro(context))       == null &&
            (decl = extern(context))      == null &&
            (decl = namespace(context))   == null &&
            (decl = enumeration(context)) == null &&
            (decl = group(context))       == null &&
            (decl = typedef(context))     == null &&
            (decl = using(context))       == null &&
            (decl = function(context))    == null &&
            (decl = variable(context))    == null &&
            (decl = attribute())          == null) {
            throw new Exception(token.file + ":" + token.lineNumber + ": Could not parse declaration at '" + token + "'");
        } else {
            decl.text = comment + decl.text;
            return decl;
        }
    }

    public void parse(String outputFilename, String ... inputFilenames) throws IOException, Exception {
        File[] files = new File[inputFilenames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(inputFilenames[i]);
        }
        parse(new File(outputFilename), files);
    }
    public void parse(File outputFile, File ... inputFiles) throws IOException, Exception {
        ArrayList<Token> tokenList = new ArrayList<Token>();
        String lineSeparator = "\n";
        for (File file : inputFiles) {
            Info info = infoMap.getFirst(file.getName());
            if (info != null && !info.parse) {
                continue;
            }
            System.out.println("Parsing header file: " + file);
            Token token = new Token();
            token.type = Token.COMMENT;
            token.value = "\n/* Wrapper for header file " + file + " */\n\n";
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

        Writer out = outputFile != null ? new FileWriter(outputFile) : new Writer() {
            @Override public void write(char[] cbuf, int off, int len) { }
            @Override public void flush() { }
            @Override public void close() { }
        };
        LinkedList<Info> infoList = infoMap.get(null);
        for (Info info : infoList) {
            if (info.text != null) {
                out.append(info.text.replaceAll("\n", lineSeparator));
            }
        }
        out.append("{" + lineSeparator);
        out.append("    static { Loader.load(); }" + lineSeparator);
        out.append(vectors());
        while (!tokens.get().match(Token.EOF)) {
            out.append(declaration(null).text.replaceAll("\n", lineSeparator));
        }
        String comment = commentBefore();
        if (comment != null) {
            out.append(comment.replaceAll("\n", lineSeparator));
        }
        out.append(lineSeparator + "}" + lineSeparator);
        out.close();
    }

    public File parse(String outputDirectory, Class cls) throws IOException, Exception {
        return parse(new File(outputDirectory), cls);
    }
    public File parse(File outputDirectory, Class cls) throws IOException, Exception {
        Loader.ClassProperties allProperties = Loader.loadProperties(cls, properties, true);
        Loader.ClassProperties clsProperties = Loader.loadProperties(cls, properties, false);
        LinkedList<File> allFiles = allProperties.getHeaderFiles();
        LinkedList<File> clsFiles = clsProperties.getHeaderFiles();
        LinkedList<String> allTargets = allProperties.get("parser.target");
        LinkedList<String> clsTargets = clsProperties.get("parser.target");
        String target = clsTargets.getFirst(); // there can only be one

        String text = "/* DO NOT EDIT THIS FILE - IT IS MACHINE GENERATED */\n\n";
        int n = target.lastIndexOf('.');
        if (n >= 0) {
            text += "package " + target.substring(0, n) + ";\n\n";
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
        text += "public class " + target.substring(n + 1) + " extends " + cls.getCanonicalName() + " ";
        infoMap.put(new Info().text(text));

        File targetFile = new File(outputDirectory, target.replace('.', '/') + ".java");
        System.out.println("Targeting file: " + targetFile);
        for (File f : allFiles) {
            if (!clsFiles.contains(f)) {
                parse(null, f);
            }
        }
        parse(targetFile, clsFiles.toArray(new File[clsFiles.size()]));
        return targetFile;
    }
}
