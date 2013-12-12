/*
 * Copyright (C) 2013 Samuel Audet
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
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * To do:
 * - Name overloaded operators
 * - Enhance support for templates
 * - Handle anonymous struct and union
 * - etc.
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

        String[] cppNames = null, javaNames = null, annotations = null,
                 valueTypes = null, pointerTypes = null, genericArgs = null;
        boolean cast = false, define = false, complex = false, opaque = false, parse = false;
        String parent = null, text = null;

        public Info cppNames(String ... cppNames) { this.cppNames = cppNames; return this; }
        public Info javaNames(String ... javaNames) { this.javaNames = javaNames; return this; }
        public Info annotations(String ... annotations) { this.annotations = annotations; return this; }
        public Info valueTypes(String ... valueTypes) { this.valueTypes = valueTypes; return this; }
        public Info pointerTypes(String ... pointerTypes) { this.pointerTypes = pointerTypes; return this; }
        public Info genericArgs(String ... genericArgs) { this.genericArgs = genericArgs; return this; }
        public Info cast(boolean cast) { this.cast = cast; return this;  }
        public Info define(boolean define) { this.define = define; return this; }
        public Info complex(boolean complex) { this.complex = complex; return this; }
        public Info opaque(boolean opaque) { this.opaque = opaque; return this; }
        public Info parse(boolean parse) { this.parse = parse; return this; }
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
            i.cast = cast;
            i.define = define;
            i.complex = complex;
            i.opaque = opaque;
            i.parse = parse;
            i.parent = parent;
            i.text = text;
            return i;
        }
    }

    public static class InfoMap extends HashMap<String,LinkedList<Info>> {
        public InfoMap() { this.parent = defaults; }
        public InfoMap(InfoMap parent) { this.parent = parent; }

        InfoMap parent = null;
        static final InfoMap defaults = new InfoMap(null)
            .put(new Info("void").valueTypes("void").pointerTypes("Pointer"))
            .put(new Info("FILE").pointerTypes("Pointer").cast(true))
            .put(new Info("va_list").pointerTypes("Pointer").cast(true))

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
            .put(new Info("bool", "jboolean").valueTypes("boolean").pointerTypes("BoolPointer").cast(true))
            .put(new Info("const char").valueTypes("byte").pointerTypes("@Cast(\"const char*\") BytePointer", "String"))
            .put(new Info("std::string").valueTypes("@StdString BytePointer", "@StdString String"))
            .put(new Info("wchar_t", "WCHAR").valueTypes("char").pointerTypes("CharPointer").cast(true))

            .put(new Info("position").javaNames("_position"))
            .put(new Info("limit").javaNames("_limit"))
            .put(new Info("capacity").javaNames("_capacity"));

        static String sort(String name) {
            return sort(name, false);
        }
        static String sort(String name, boolean unconst) {
            if (name == null) {
                return null;
            }
            TreeSet<String> set = new TreeSet<String>();
            try {
                Tokenizer tokenizer = new Tokenizer(name);
                Token token;
                while (!(token = tokenizer.nextToken()).isEmpty()) {
                    set.add(token.value);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            boolean foundConst = false;
            name = "";
            for (String s : set) {
                if ("const".equals(s)) {
                    foundConst = true;
                } else {
                    name += s + " ";
                }
            }
            if (!unconst && foundConst) {
                return "const " + name.trim();
            } else {
                return name.trim();
            }
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

        public InfoMap put(Info info) {
            for (String cppName : info.cppNames != null ? info.cppNames : new String[] { null }) {
                String key = sort(cppName, false);
                LinkedList<Info> infoList = super.get(key);
                if (infoList == null) {
                    infoList = new LinkedList<Info>();
                }
                if (!infoList.contains(info)) {
                    infoList.add(info);
                }
                super.put(key, infoList);
            }
            return this;
        }
    }

    public static interface InfoMapper {
        void map(InfoMap infoMap);
    }

    static class Token implements Cloneable {
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


    public Parser(Properties properties, InfoMap infoMap) {
        this.properties = properties;
        this.infoMap = infoMap;
    }

    Properties properties = null;
    InfoMap infoMap = null;
    Token[] tokenArray = null, subTokenArray = null;
    int tokenIndex = 0, subTokenIndex = 0;
    boolean preprocess = true;

    void filterToken(int index) {
        if (index + 1 < tokenArray.length && tokenArray[index].match('#') &&
                tokenArray[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
            ArrayList<Token> tokens = new ArrayList<Token>();
            for (int i = 0; i < index; i++) {
                tokens.add(tokenArray[i]);
            }
            int count = 0;
            Info info = null;
            boolean define = true, defined = false;
            while (index < tokenArray.length) {
                Token keyword = null;
                if (tokenArray[index].match('#')) {
                    if (count == 0 && tokenArray[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
                        count++;
                        keyword = tokenArray[index + 1];
                    } else if (count == 1 && tokenArray[index + 1].match(Token.ELIF, Token.ELSE, Token.ENDIF)) {
                        keyword = tokenArray[index + 1];
                    }
                }
                if (keyword != null) {
                    tokens.add(tokenArray[index++]);
                    tokens.add(tokenArray[index++]);
                    if (keyword.match(Token.IF, Token.IFDEF, Token.IFNDEF, Token.ELIF)) {
                        String value = "";
                        while (index < tokenArray.length) {
                            if (tokenArray[index].spacing.indexOf('\n') >= 0) {
                                break;
                            }
                            value += tokenArray[index].spacing + tokenArray[index];
                            tokens.add(tokenArray[index++]);
                        }
                        define = info == null || !defined;
                        info = null;
                        LinkedList<Info> infoList = infoMap.get(value);
                        if (infoList.size() > 0) {
                            info = infoList.getFirst();
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
                    tokens.add(tokenArray[index++]);
                } else {
                    index++;
                }
                defined = define || defined;
            }
            while (index < tokenArray.length) {
                tokens.add(tokenArray[index++]);
            }
            tokenArray = tokens.toArray(new Token[tokens.size()]);
        }
    }

    void expandToken(int index) {
        if (index < tokenArray.length && infoMap.containsKey(tokenArray[index].value)) {
            int startIndex = index;
            LinkedList<Info> infoList = infoMap.get(tokenArray[index].value);
            if (infoList.size() > 0 && infoList.getFirst().text != null) {
                Info info = infoList.getFirst();
                if (info.genericArgs != null && (index + 1 >= tokenArray.length
                        || !tokenArray[index + 1].match('('))) {
                    return;
                }
                ArrayList<Token> tokens = new ArrayList<Token>();
                for (int i = 0; i < index; i++) {
                    tokens.add(tokenArray[i]);
                }
                ArrayList<Token>[] args = null;
                if (info.genericArgs != null) {
                    args = new ArrayList[info.genericArgs.length];
                    int count = 0, count2 = 0;
                    for (index += 2; index < tokenArray.length; index++) {
                        Token token = tokenArray[index];
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
                }
                try {
                    Tokenizer tokenizer = new Tokenizer(info.text);
                    Token token;
                    while (!(token = tokenizer.nextToken()).isEmpty()) {
                        boolean foundArg = false;
                        for (int i = 0; info.genericArgs != null && i < info.genericArgs.length; i++) {
                            if (info.genericArgs[i].equals(token.value)) {
                                if (tokens.size() == startIndex) {
                                    args[i].get(0).spacing = tokenArray[startIndex].spacing;
                                }
                                tokens.addAll(args[i]);
                                foundArg = true;
                                break;
                            }
                        }
                        if (!foundArg) {
                            if (tokens.size() == startIndex) {
                                token.spacing = tokenArray[startIndex].spacing;
                            }
                            tokens.add(token);
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                for (index += 1; index < tokenArray.length; index++) {
                    tokens.add(tokenArray[index]);
                }
                tokenArray = tokens.toArray(new Token[tokens.size()]);
            }
        }
    }

    int preprocessToken(int index, int count) {
        while (index < tokenArray.length) {
            filterToken(index);
            expandToken(index);
            if (!tokenArray[index].match(Token.COMMENT) && --count < 0) {
                break;
            }
            index++;
        }
        filterToken(index);
        expandToken(index);
        return index;
    }

    Token getToken() {
        return getToken(0);
    }
    Token getToken(int i) {
        if (subTokenArray != null) {
            return subTokenIndex + i < subTokenArray.length ? subTokenArray[subTokenIndex + i] : Token.EOF;
        } else {
            int k = preprocess ? preprocessToken(tokenIndex, i) : tokenIndex + i;
            return k < tokenArray.length ? tokenArray[k] : Token.EOF;
        }
    }
    Token nextToken() {
        if (subTokenArray != null) {
            return ++subTokenIndex < subTokenArray.length ? subTokenArray[subTokenIndex] : Token.EOF;
        } else {
            tokenIndex = preprocess ? preprocessToken(tokenIndex, 1) : tokenIndex + 1;
            return tokenIndex < tokenArray.length ? tokenArray[tokenIndex] : Token.EOF;
        }
    }


    static class Context implements Cloneable {
        String namespace = null, group = null;
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

    static String rescan(String lines, String spacing) {
        String text = "";
        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            text += spacing + scanner.nextLine();
            int newline = spacing.lastIndexOf('\n');
            if (newline >= 0) {
                spacing = spacing.substring(newline);
            }
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
            LinkedList<Info> infoList2 = infoMap.get(cppType);
            if (infoList2.size() > 0) {
                Info info2 = infoList2.getFirst();
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
        if (!getToken().match(Token.TEMPLATE)) {
            return context.templateMap;
        }
        TemplateMap map = new TemplateMap(context.templateMap);

        nextToken().expect('<');
        for (Token token = nextToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match(Token.IDENTIFIER)) {
                map.put(nextToken().expect(Token.IDENTIFIER).value, null);
                token = nextToken();
            }
            if (token.expect(',', '>').match('>')) {
                nextToken();
                break;
            }
        }
        return map;
    }


    static class Type {
        boolean anonymous = false, constValue = false, destructor = false, staticMember = false;
        String annotations = "", cppName = "", javaName = "";
    }

    Type type(Context context) {
        Type type = new Type();
        int count = 0;
        boolean simpleType = false;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match("::")) {
                type.cppName += token;
            } else if (token.match('<')) {
                type.cppName += token;
                count++;
            } else if (token.match('>')) {
                type.cppName += token;
                count--;
            } else if (token.match(Token.CONST)) {
                type.constValue = true;
            } else if (token.match('~')) {
                type.destructor = true;
            } else if (token.match(Token.STATIC)) {
                type.staticMember = true;
            } else if (token.match(Token.TYPEDEF, Token.USING, Token.ENUM, Token.EXTERN,
                    Token.CLASS, Token.STRUCT, Token.UNION, Token.INLINE, Token.VIRTUAL)) {
                continue;
            } else if (token.match("signed", "unsigned", "char", "short", "int", "long", "bool", "float", "double")) {
                if (!simpleType && count == 0) {
                    type.cppName = token.value + " ";
                } else {
                    type.cppName += token.value + " ";
                }
                simpleType = true;
            } else if (token.match(Token.IDENTIFIER)) {
                LinkedList<Info> infoList = infoMap.get(token.value);
                if (infoList.size() > 0 && infoList.getFirst().annotations != null) {
                    for (String s : infoList.getFirst().annotations) {
                        type.annotations += s + " ";
                    }
                } else if (type.cppName.length() > 0 && !type.cppName.endsWith("::") && count == 0) {
                    infoList = infoMap.get(getToken(1).value);
                    if ((infoList.size() > 0 && infoList.getFirst().annotations != null) ||
                            !getToken(1).match('*', '&', Token.IDENTIFIER, Token.CONST)) {
                        // we probably reached a variable or function name identifier
                        break;
                    }
                } else {
                    if (type.cppName.endsWith("::") || count > 0) {
                        type.cppName += token.value;
                    } else {
                        type.cppName = token.value;
                    }
                }
            } else if (count == 0) {
                if (token.match('}')) {
                    type.anonymous = true;
                    nextToken();
                }
                break;
            }
        }
        type.cppName = type.cppName.trim();
        if (context.templateMap != null && context.templateMap.get(type.cppName) != null) {
            type.cppName = context.templateMap.get(type.cppName);
        }
        type.javaName = type.cppName;
        if ("...".equals(getToken().value)) {
            nextToken();
            return null;
        }
        return type;
    }

    static class Declarator {
        Type type;
        int infoNumber = 0, indices = 0;
        boolean constPointer = false;
        String name = "", convention = "", definitions = "", parameters = "";
    }

    Declarator declarator(Context context, String defaultName, int infoNumber,
            int varNumber, boolean arrayAsPointer, boolean pointerAsArray) throws Exception {
        boolean isTypedef = getToken().match(Token.TYPEDEF);
        boolean isUsing = getToken().match(Token.USING);
        Declarator decl = new Declarator();
        Type type = decl.type = type(context);
        if (type == null) {
            return null;
        }

        int count = 0;
        for (Token token = getToken(); varNumber > 0 && !token.match(Token.EOF); token = nextToken()) {
            if (token.match('(','[','{')) {
                count++;
            } else if (token.match(')',']','}')) {
                count--;
            } else if (count > 0) {
                continue;
            } else if (token.match(',')) {
                varNumber--;
            } else if (token.match(';')) {
                nextToken();
                return null;
            }
        }

        String cast = type.cppName;
        int indirections = 0;
        boolean reference = false;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match('*')) {
                indirections++;
                cast += "*";
            } else if (token.match('&')) {
                reference = true;
                cast += "*";
            } else if (token.match(Token.CONST)) {
                decl.constPointer = true;
                cast += "const";
            } else {
                break;
            }
        }

        int dims[] = new int[256];
        int indirections2 = 0;
        decl.name = defaultName;
        if (getToken().match('(')) {
            while (getToken().match('(')) { 
                nextToken();
            }
            for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
                if (token.match(Token.IDENTIFIER)) {
                    decl.name = token.value;
                } else if (token.match('*')) {
                    indirections2++;
                    decl.convention = decl.name;
                    decl.name = defaultName;
                } else if (token.match('[')) {
                    Token n = getToken(1);
                    dims[decl.indices++] = n.match(Token.INTEGER) ? Integer.parseInt(n.value) : -1;
                } else if (token.match(')')) {
                    nextToken();
                    break;
                }
            }
            while (getToken().match(')')) {
                nextToken();
            }
        } else if (getToken().match(Token.IDENTIFIER)) {
            decl.name = getToken().value;
            nextToken();
        }

        boolean bracket = false;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (!bracket && token.match('[')) {
                bracket = true;
                Token n = getToken(1);
                dims[decl.indices++] = n.match(Token.INTEGER) ? Integer.parseInt(n.value) : -1;
            } else if (!bracket) {
                break;
            } else if (bracket && token.match(']')) {
                bracket = false;
            }
        }
        while (decl.indices > 0 && indirections2 > 0) {
            // treat complex combinations of arrays and pointers as multidimensional arrays
            dims[decl.indices++] = -1;
            indirections2--;
        }
        if (arrayAsPointer && decl.indices > 0) {
            // treat array as an additional indirection
            indirections++;
            String dimCast = "";
            for (int i = 1; i < decl.indices; i++) {
                if (dims[i] > 0) {
                    dimCast += "[" + dims[i] + "]";
                }
            }
            //decl.indices = 0;
            cast += dimCast.length() > 0 ? "(*)" + dimCast : "*";
        }
        if (pointerAsArray && indirections > (type.anonymous ? 0 : 1)) {
            // treat second indirection as an array, unless anonymous
            dims[decl.indices++] = -1;
            indirections--;
            cast = cast.substring(0, cast.length() - 1);
        }

        if (getToken().match(':')) {
            // ignore bitfields
            type.annotations += "@NoOffset ";
            nextToken().expect(Token.INTEGER);
            nextToken().expect(',', ';');
        }

        int infoLength = 1;
        boolean valueType = false, needCast = arrayAsPointer && decl.indices > 1, implicitConst = false;
        String prefix = type.constValue && indirections < 2 && !reference ? "const " : "";
        LinkedList<Info> infoList = infoMap.get(prefix + type.cppName);
        String ns = "";
        while (context.namespace != null && infoList.size() == 0 && !ns.equals(context.namespace)) {
            int i = context.namespace.indexOf("::", ns.length() + 2);
            ns = i < 0 ? context.namespace : context.namespace.substring(0, i);
            infoList = infoMap.get(prefix + ns + "::" + type.cppName);
        }
        if (infoList.size() > 0) {
            Info info = infoList.getFirst();
            valueType = info.valueTypes != null &&
                    ((indirections == 0 && !reference) || info.pointerTypes == null);
            needCast |= info.cast;
            implicitConst = info.cppNames[0].startsWith("const ");
            infoLength = valueType ? info.valueTypes.length :
                    info.pointerTypes != null ? info.pointerTypes.length : 1;
            decl.infoNumber = infoNumber < 0 ? 0 : infoNumber % infoLength;
            type.javaName = valueType ? info.valueTypes[decl.infoNumber] :
                    info.pointerTypes != null ? info.pointerTypes[decl.infoNumber] : type.cppName;
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
                decl.infoNumber += infoLength;
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

        Parameters params = parameters(context, infoNumber);
        if (params != null) {
            decl.infoNumber = Math.max(decl.infoNumber, params.infoNumber);
            if (params.definitions.length() > 0) {
                decl.definitions += params.definitions + "\n";
            }
            if (indirections2 == 0) {
                decl.parameters = params.list;
            } else {
                String functionType = isTypedef ? decl.name :
                        Character.toUpperCase(decl.name.charAt(0)) + decl.name.substring(1) + params.signature;
                if (infoNumber <= params.infoNumber) {
                    decl.definitions +=
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
                decl.parameters = "";
            }
        }

        infoList = infoMap.get(decl.name);
        if (infoList.size() > 0) {
            Info info = infoList.getFirst();
            if (info.javaNames != null && info.javaNames.length > 0) {
                type.annotations += "@Name(\"" + decl.name + "\") ";
                decl.name = info.javaNames[0];
            }
        }
        return decl;
    }

    String commentBefore() throws Exception {
        String comment = "";
        preprocess = false;
        while (tokenIndex > 0 && getToken(-1).match(Token.COMMENT)) {
            tokenIndex--;
        }
        for (Token token = getToken(); token.match(Token.COMMENT); token = nextToken()) {
            if (token.value.length() <= 3 || token.value.charAt(3) != '<') {
                comment += token.spacing + token.value;
            }
        }
        preprocess = true;
        return comment;
    }

    String commentAfter() throws Exception {
        String comment = "";
        preprocess = false;
        while (tokenIndex > 0 && getToken(-1).match(Token.COMMENT)) {
            tokenIndex--;
        }
        for (Token token = getToken(); token.match(Token.COMMENT); token = nextToken()) {
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
        preprocess = true;
        return comment;
    }

    boolean attribute() throws Exception {
        if (!getToken().match(Token.IDENTIFIER)) {
            return false;
        }
        if (!nextToken().match('(')) {
            return true;
        }

        int count = 1;
        preprocess = false;
        for (Token token = nextToken(); !token.match(Token.EOF) && count > 0; token = nextToken()) {
            if (token.match('(')) {
                count++;
            } else if (token.match(')')) {
                count--;
            }
        }
        preprocess = true;
        return true;
    }

    boolean body() throws Exception {
        if (!getToken().match('{')) {
            return false;
        }

        int count = 1;
        preprocess = false;
        for (Token token = nextToken(); !token.match(Token.EOF) && count > 0; token = nextToken()) {
            if (token.match('{')) {
                count++;
            } else if (token.match('}')) {
                count--;
            }
        }
        preprocess = true;
        return true;
    }


    static class Parameters {
        int infoNumber = 0;
        String list = "", definitions = "", signature = "", names = "";
    }

    Parameters parameters(Context context, int infoNumber) throws Exception {
        if (!getToken().match('(')) {
            return null;
        }

        int count = 0;
        Parameters params = new Parameters();
        params.list = "(";
        params.names = "(";
        for (Token token = nextToken(); !token.match(Token.EOF); token = getToken()) {
            String spacing = token.spacing;
            if (token.match(')')) {
                params.list += spacing + ")";
                params.names += ")";
                nextToken();
                break;
            }
            Declarator decl = declarator(context, "arg" + count++, infoNumber, 0, true, false);
            if (decl != null && !decl.type.javaName.equals("void")) {
                params.infoNumber = Math.max(params.infoNumber, decl.infoNumber);
                params.list += (count > 1 ? "," : "") + spacing + decl.type.annotations + decl.type.javaName + " " + decl.name;
                params.definitions += decl.definitions;
                params.signature += '_';
                for (char c : decl.type.javaName.substring(decl.type.javaName.lastIndexOf(' ') + 1).toCharArray()) {
                    if (Character.isDigit(c) || Character.isLetter(c) || c == '_') {
                        params.signature += c;
                    }
                }
                params.names += (count > 1 ? ", " : "") + decl.name;
                if (decl.name.startsWith("arg")) {
                    try {
                        count = Integer.parseInt(decl.name.substring(3)) + 1;
                    } catch (NumberFormatException e) { /* don't care if not int */ }
                }
            }
            if (!getToken().match(',', ')')) {
                // output default argument as a comment
                params.list += "/*" + getToken();
                int count2 = 0;
                for (token = nextToken(), token.spacing = ""; !token.match(Token.EOF); token = nextToken()) {
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
            if (getToken().expect(',', ')').match(',')) {
                nextToken();
            }
        }
        return params;
    }

    String function(Context context) throws Exception {
        return function(context, false);
    }
    String function(Context context, boolean constructor) throws Exception {
        int backIndex = tokenIndex;
        String spacing = getToken().spacing;
        String modifiers = "public native ";
        Declarator decl = new Declarator();
        Type type = type(context);
        String name = type.cppName;
        Parameters params = parameters(context, 0);
        if (name.length() == 0) {
            // not a function, probably an attribute
            tokenIndex = backIndex;
            return null;
        } else if (context.group == null && params != null) {
            // this is a constructor definition, skip over
            body();
            return spacing;
        } else if (name.equals(context.group) && params != null) {
            // this is a constructor or destructor
            constructor = !type.destructor;
            decl.parameters = params.list;
            decl.definitions = params.definitions;
        } else if (!constructor) {
            tokenIndex = backIndex;
            decl = declarator(context, null, 0, 0, false, false);
            type = decl.type;
            name = decl.name;
        }

        if (name == null || decl.parameters.length() == 0) {
            tokenIndex = backIndex;
            return null;
        } else if (type.staticMember || context.group == null || context.group.length() == 0) {
            modifiers = "public static native ";
        }

        String text  = "";
        String definitions = "";
        LinkedList<Info> infoList = infoMap.get(name);
        if (infoList.size() == 0) {
            infoList.add(null);
        }
        for (Info info : infoList) {
            if (info != null) {
                if (info.genericArgs != null && context.templateMap != null) {
                    int count = 0;
                    for (String key : context.templateMap.keySet()) {
                        if (count < info.genericArgs.length) {
                            context.templateMap.put(key, info.genericArgs[count++]);
                        }
                    }
                }
                name = info.javaNames == null ? info.cppNames[0] : info.javaNames.length == 0 ? "" : info.javaNames[0];
                if (!name.equals(info.cppNames[0]) && name.length() > 0) {
                    name = "@Name(\"" + info.cppNames[0] + "\") " + name;
                }
            }

            LinkedList<Declarator> prevDecl = new LinkedList<Declarator>();
            for (int n = -1; n < Integer.MAX_VALUE; n++) {
                tokenIndex = backIndex;
                if (constructor || type.destructor) {
                    type = type(context);
                    name = type.cppName;
                    params = parameters(context, n);
                    decl.parameters = params.list;
                    decl.definitions = params.definitions;
                    if (getToken().match(':')) {
                        for (Token token = nextToken(); !token.match(Token.EOF); token = nextToken()) {
                            if (token.match('}', ';')) {
                                break;
                            }
                        }
                    }
                } else {
                    decl = declarator(context, null, n, 0, false, false);
                    type = decl.type;
                }
                boolean found = false;
                for (Declarator d : prevDecl) {
                    found |= /* decl.javaType.equals(d.javaType) && */ decl.parameters.equals(d.parameters);
                }
                if (found && n > 0) {
                    break;
                } else if (name.length() > 0 && !found && !type.destructor) {
                    if (context.namespace != null && context.group == null) {
                        text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (constructor) {
                        text += "public " + name + decl.parameters + " { allocate" + params.names + "; }\n" +
                                "private native void allocate" + decl.parameters + ";\n";
                    } else {
                        text += modifiers + type.annotations + type.javaName + " " + name + decl.parameters + ";\n";
                    }
                    definitions += decl.definitions;
                }
                prevDecl.add(decl);
            }
            while (attribute()) { }
            if (getToken().match('{')) {
                body();
            } else {
                nextToken();
            }
        }
        String comment = commentAfter();
        return rescan(definitions + comment + text, spacing);
    }

    String variable(Context context) throws Exception {
        int backIndex = tokenIndex;
        String spacing = getToken().spacing;
        String modifiers = "public native ";
        String setterType = context.group + " ";
        Declarator decl = declarator(context, null, 0, 0, false, true);
        String name = decl.name;
        if (name == null || !getToken().match('[', '=', ',', ':', ';')) {
            tokenIndex = backIndex;
            return null;
        } else if (decl.type.staticMember || context.group == null || context.group.length() == 0) {
            modifiers = "public static native ";
            setterType = "void ";
        }

        String text  = "";
        String definitions = "";
        for (Declarator metadecl : context.variables != null ? context.variables : new Declarator[] { null }) {
            for (int n = 0; n < Integer.MAX_VALUE; n++) {
                tokenIndex = backIndex;
                decl = declarator(context, null, -1, n, false, true);
                if (decl == null) {
                    break;
                }
                name = decl.name;
                if (metadecl == null || metadecl.indices == 0 || decl.indices == 0) {
                    // arrays are currently not supported for both metadecl and decl at the same time
                    String indices = "";
                    for (int i = 0; i < (metadecl == null || metadecl.indices == 0 ? decl.indices : metadecl.indices); i++) {
                        if (i > 0) {
                            indices += ", ";
                        }
                        indices += "int " + (char)('i' + i);
                    }
                    if (context.namespace != null && context.group == null) {
                        text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (metadecl != null && metadecl.name.length() > 0) {
                        text += "@Name({\"" + metadecl.name + "\", \"." + decl.name + "\"}) ";
                        name = metadecl.name + "_" + decl.name;
                    }
                    if (decl.type.constValue) {
                        text += "@MemberGetter ";
                    }
                    text += modifiers + decl.type.annotations + decl.type.javaName + " " + name + "(" + indices + ");";
                    if (!decl.type.constValue) {
                        if (indices.length() > 0) {
                            indices += ", ";
                        }
                        text += " " + modifiers + setterType + name + "(" + indices + decl.type.javaName + " " + name + ");";
                    }
                    text += "\n";
                    definitions += decl.definitions;
                }
                if (decl.indices > 0) {
                    // in the case of arrays, also add a pointer accessor
                    tokenIndex = backIndex;
                    decl = declarator(context, null, -1, n, true, false);
                    String indices = "";
                    for (int i = 0; i < (metadecl == null ? 0 : metadecl.indices); i++) {
                        if (i > 0) {
                            indices += ", ";
                        }
                        indices += "int " + (char)('i' + i);
                    }
                    if (context.namespace != null && context.group == null) {
                        text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (metadecl != null && metadecl.name.length() > 0) {
                        text += "@Name({\"" + metadecl.name + "\", \"." + decl.name + "\"}) ";
                        name = metadecl.name + "_" + decl.name;
                    }
                    text += "@MemberGetter " + modifiers + decl.type.annotations + decl.type.javaName + " " + name + "(" + indices + ");\n";
                }
            }
        }
        String comment = commentAfter();
        return rescan(definitions + comment + text, spacing);
    }

    String macro(Context context) throws Exception {
        if (!getToken().match('#')) {
            return null;
        }
        preprocess = false;
        String spacing = getToken().spacing;
        Token keyword = nextToken();

        nextToken();
        int beginIndex = tokenIndex;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.spacing.indexOf('\n') >= 0) {
                break;
            }
        }
        int endIndex = tokenIndex;

        String text = "";
        if (keyword.match(Token.DEFINE) && beginIndex + 1 < endIndex) {
            tokenIndex = beginIndex;
            String macroName = getToken().value;
            Token first = nextToken();
            boolean hasArgs = first.spacing.length() == 0 && first.match('(');
            LinkedList<Info> infoList = infoMap.get(macroName);
            if (infoList.size() == 0) {
                infoList.add(null);
            }
            for (Info info : infoList) {
                if (hasArgs && info == null) {
                    // save declaration for expansion
                    info = new Info(macroName).genericArgs(new String[endIndex - tokenIndex]).text("");
                    int count = 0;
                    for (Token token = getToken(); tokenIndex < endIndex; token = nextToken()) {
                        if (token.match(Token.IDENTIFIER)) {
                            info.genericArgs[count++] = token.value;
                        } else if (token.match(')')) {
                            break;
                        }
                    }
                    info.genericArgs = Arrays.copyOf(info.genericArgs, count);
                    for (Token token = nextToken(); tokenIndex < endIndex; token = nextToken()) {
                        info.text += token.spacing + token;
                    }
                    infoMap.put(info);
                } else if (info != null && info.text == null &&
                        info.genericArgs != null && info.genericArgs.length > (hasArgs ? 0 : 1)) {
                    // declare as a static native method
                    LinkedList<Declarator> prevDecl = new LinkedList<Declarator>();
                    for (int n = -1; n < Integer.MAX_VALUE; n++) {
                        int count = 1;
                        tokenIndex = beginIndex + 2;
                        String params = "(";
                        for (Token token = getToken(); hasArgs && tokenIndex < endIndex
                                && count < info.genericArgs.length; token = nextToken()) {
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

                        subTokenArray = new Tokenizer(info.genericArgs[0] + " " + macroName + params).tokenize();
                        subTokenIndex = 0;
                        Declarator decl = declarator(context, null, n, 0, false, false);
                        subTokenArray = null;

                        for (int i = 0; i < info.cppNames.length; i++) {
                            if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                                macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                                break;
                            }
                        }

                        boolean found = false;
                        for (Declarator d : prevDecl) {
                            found |= /* decl.javaType.equals(d.javaType) && */ decl.parameters.equals(d.parameters);
                        }
                        if (found && n > 0) {
                            break;
                        } else if (!found) {
                            text += "public static native " + decl.type.annotations + decl.type.javaName + " " + macroName + decl.parameters + ";\n";
                        }
                        prevDecl.add(decl);
                    }
                } else if (info == null || (info.text == null &&
                        (info.genericArgs == null || info.genericArgs.length == 1))) {
                    // declare as a static final variable
                    String value = "";
                    String type = "int";
                    String cat = "";
                    tokenIndex = beginIndex + 1;
                    Token prevToken = new Token();
                    boolean complex = false;
                    for (Token token = getToken(); tokenIndex < endIndex; token = nextToken()) {
                        if (token.match(Token.STRING)) {
                            type = "String"; cat = " + "; break;
                        } else if (token.match(Token.FLOAT)) {
                            type = "double"; cat = ""; break;
                        } else if (token.match(Token.INTEGER) && token.value.endsWith("L")) {
                            type = "long"; cat = ""; break;
                        } else if ((prevToken.match(Token.IDENTIFIER) && token.match('(')) || token.match('{', '}')) {
                            complex = true;
                        }
                        prevToken = token;
                    }
                    if (info != null) {
                        if (info.genericArgs != null) {
                            subTokenArray = new Tokenizer(info.genericArgs[0]).tokenize();
                            subTokenIndex = 0;
                            Declarator decl = declarator(context, null, -1, 0, false, true);
                            subTokenArray = null;
                            type = decl.type.annotations + decl.type.javaName;
                        }
                        for (int i = 0; i < info.cppNames.length; i++) {
                            if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                                macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                                break;
                            }
                        }
                        complex = info.complex;
                    }
                    tokenIndex = beginIndex + 1;
                    if (complex) {
                        text += "public static native @MemberGetter " + type + " " + macroName + "();\n";
                        value = " " + macroName + "()";
                    } else {
                        while (getToken(endIndex - tokenIndex - 1).match(Token.COMMENT)) {
                            endIndex--;
                        }
                        for (Token token = getToken(); tokenIndex < endIndex; token = nextToken()) {
                            value += token.spacing + token + (tokenIndex + 1 < endIndex ? cat : "");
                        }
                    }
                    int i = type.lastIndexOf(' ');
                    if (i >= 0) {
                        type = type.substring(i + 1);
                    }
                    if (value.length() > 0) {
                        text += "public static final " + type + " " + macroName + " =" + value + ";\n";
                    }
                }
            }

            if (text.length() > 0) {
                tokenIndex = endIndex;
                String comment = commentAfter();
                text = rescan(comment + text, spacing);
            }
        }

        if (text.length() == 0) {
            // output whatever we did not process as comment
            tokenIndex = beginIndex;
            while (getToken(endIndex - tokenIndex - 1).match(Token.COMMENT)) {
                endIndex--;
            }
            int n = spacing.lastIndexOf('\n') + 1;
            text += "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
            for (Token token = getToken(); tokenIndex < endIndex; token = nextToken()) {
                text += token.match("\n") ? "\n// " : token.spacing + token;
            }
            String comment = commentAfter();
            text = spacing.substring(0, n) + comment + text;
        }
        preprocess = true;
        return text;
    }

    String typedef(Context context) throws Exception {
        if (!getToken().match(Token.TYPEDEF)) {
            return null;
        }
        String spacing = getToken().spacing;
        Declarator decl = declarator(context, null, 0, 0, true, false);
        nextToken();

        String name = decl.name;
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        if (decl.definitions.length() > 0) {
            infoMap.put(new Info(name).valueTypes(decl.name));
        } else if (decl.type.cppName.equals("void")) {
            decl.definitions =
                    "@Opaque public static class " + name + " extends Pointer {\n" +
                    "    public " + name + "() { }\n" +
                    "    public " + name + "(Pointer p) { super(p); }\n" +
                    "}";
        } else {
            LinkedList<Info> infoList = infoMap.get(decl.type.cppName);
            Info info = infoList.size() > 0 ? infoList.getFirst().clone() : new Info(name);
            if (info.valueTypes == null) {
                info.valueTypes(info.cppNames);
            }
            if (info.pointerTypes == null) {
                info.pointerTypes(info.cppNames);
            }
            infoMap.put(info.cppNames(name).cast(true));
        }

        String comment = commentAfter();
        return rescan(comment + decl.definitions, spacing);
    }

    String using(Context context) throws Exception {
        if (!getToken().match(Token.USING)) {
            return null;
        }
        String spacing = getToken().spacing;
        Declarator decl = declarator(context, null, 0, 0, true, false);
        nextToken();

        String name = decl.type.cppName.substring(decl.type.cppName.lastIndexOf("::") + 2);
        LinkedList<Info> infoList = infoMap.get(decl.type.cppName);
        Info info = infoList.size() > 0 ? infoList.getFirst().clone() : new Info(name);
        if (info.valueTypes == null) {
            info.valueTypes(info.cppNames);
        }
        if (info.pointerTypes == null) {
            info.pointerTypes(info.cppNames);
        }
        infoMap.put(info.cppNames(name).cast(true));

        String comment = commentAfter();
        return rescan(comment + decl.definitions, spacing);
    }

    String group(Context context) throws Exception {
        int backIndex = tokenIndex;
        String spacing = getToken().spacing;
        boolean isTypedef = getToken().match(Token.TYPEDEF);
        boolean foundGroup = false, accessible = true;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match(Token.CLASS, Token.STRUCT, Token.UNION)) {
                foundGroup = true;
                accessible = !token.match(Token.CLASS);
                break;
            } else if (!token.match(Token.IDENTIFIER)) {
                break;
            }
        }
        if (!foundGroup) {
            tokenIndex = backIndex;
            return null;
        }

        nextToken().expect(Token.IDENTIFIER, '{');
        if (isTypedef && getToken(1).match('*')) {
            nextToken();
        }
        if (!getToken().match('{') && getToken(1).match(Token.IDENTIFIER)
                && (isTypedef || !getToken(2).match(';'))) {
            nextToken();
        }
        Type type = type(context);
        String text = type.annotations;
        String name = type.cppName;
        String parent = "Pointer";
        if (name.length() > 0 && getToken().match(':')) {
            for (Token token = nextToken(); !token.match(Token.EOF); token = nextToken()) {
                if (token.match('{')) {
                    break;
                } else if (token.match(Token.PUBLIC)) {
                    parent = nextToken().expect(Token.IDENTIFIER).value;
                }
            }
        }
        if (!getToken().match('{', ';')) {
            tokenIndex = backIndex;
            return null;
        }
        LinkedList<Info> infoList = infoMap.get(name);
        if (name.length() > 0 && getToken().match(';')) {
            nextToken();
            if (infoList.size() == 0 || infoList.getFirst().opaque) {
                if (infoList.size() > 0 && infoList.getFirst().parent != null) {
                    parent = infoList.getFirst().parent;
                }
                infoMap.put(new Info(name).opaque(false));
                if (context.namespace != null) {
                    text += "@Namespace(\"" + context.namespace + "\") ";
                }
                text += "@Opaque public static class " + name + " extends " + parent + " {\n" +
                        "    public " + name + "() { }\n" +
                        "    public " + name + "(Pointer p) { super(p); }\n" +
                        "}";
            }
            String comment = commentAfter();
            return rescan(comment + text, spacing);
        }
        int startIndex = tokenIndex;
        boolean anonymous = !isTypedef && name.length() == 0;
        ArrayList<Declarator> variables = new ArrayList<Declarator>();
        if (body() && !getToken().match(';')) {
            if (isTypedef) {
                for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
                    if (token.match(';')) {
                        text += token.spacing;
                        break;
                    } else {
                        name = token.value;
                    }
                }
            } else {
                int index = tokenIndex - 1;
                for (int n = 0; n < Integer.MAX_VALUE; n++) {
                    tokenIndex = index;
                    Declarator d = declarator(context, null, -1, n, false, true);
                    if (d == null) {
                        break;
                    } else {
                        variables.add(d);
                    }
                }
                int n = spacing.lastIndexOf('\n');
                if (n >= 0) {
                    text += spacing.substring(0, n);
                }
            }
            infoList = infoMap.get(name);
        }
        if (infoList.size() == 0) {
            infoMap.put(new Info(context.namespace == null ? name : context.namespace + "::" + name));
        }
        tokenIndex = startIndex;

        Context context2 = context.clone();
        context2.namespace = context.namespace == null ? name : context.namespace + "::" + name;
        if (!anonymous) {
            context2.group = name;
        }
        if (variables.size() > 0) {
            context2.variables = variables.toArray(new Declarator[variables.size()]);
        }
        String declarations = "";
        boolean implicitConstructor = true, defaultConstructor = false;
        if (getToken().match('{')) {
            nextToken();
        }
        for (Token token = getToken(); !token.match(Token.EOF, '}'); token = getToken()) {
            if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC) && nextToken().match(':')) {
                accessible = token.match(Token.PUBLIC);
                nextToken();
            }
            String t = function(context2, true);
            if (t == null) {
                t = declaration(context2);
            } else if (accessible) {
                implicitConstructor = false;
                if (t.contains("allocate()")) {
                    defaultConstructor = true;
                }
            }
            if (accessible) {
                declarations += t;
            }
        }

        if (!anonymous) {
            text += spacing;
            if (context.namespace != null) {
                text += "@Namespace(\"" + context.namespace + "\") ";
            }
            if (!implicitConstructor) {
                text += "@NoOffset ";
            }
            if (infoList.size() > 0 && infoList.getFirst().parent != null) {
                parent = infoList.getFirst().parent;
            }
            text += "public static class " + name + " extends " + parent + " {\n" +
                    "    static { Loader.load(); }\n";

            if (implicitConstructor) {
                text += "    public " + name + "() { allocate(); }\n" +
                        "    public " + name + "(int size) { allocateArray(size); }\n" +
                        "    public " + name + "(Pointer p) { super(p); }\n" +
                        "    private native void allocate();\n" +
                        "    private native void allocateArray(int size);\n" +
                        "    @Override public " + name + " position(int position) {\n" +
                        "        return (" + name + ")super.position(position);\n" +
                        "    }\n";
            } else {
                if (!defaultConstructor) {
                    text += "    public " + name + "() { }\n";
                }
                text += "    public " + name + "(Pointer p) { super(p); }\n";
            }
        }
        String comment = commentBefore();
        text += declarations + comment;
        if (!anonymous) {
            text += getToken().spacing + '}';
        }
        for (Token token = nextToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match(';')) {
                text += token.spacing;
                break;
            }
        }
        nextToken();
        return text;
    }

    String enumeration(Context context) throws Exception {
        int backIndex = tokenIndex;
        String enumSpacing = getToken().spacing;
        boolean isTypedef = getToken().match(Token.TYPEDEF);
        boolean foundEnum = false;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match(Token.ENUM)) {
                foundEnum = true;
                break;
            } else if (!token.match(Token.IDENTIFIER)) {
                break;
            }
        }
        if (!foundEnum) {
            tokenIndex = backIndex;
            return null;
        }

        if (isTypedef && !getToken(1).match('{') && getToken(2).match(Token.IDENTIFIER)) {
            nextToken();
        }
        boolean first = true;
        int count = 0;
        String countPrefix = " ";
        String enumerators = "";
        String macroText = "";
        String name = nextToken().expect(Token.IDENTIFIER, '{').value;
        if (!getToken().match('{') && !nextToken().match('{')) {
            tokenIndex = backIndex;
            return null;
        }
        for (Token token = nextToken(); !token.match(Token.EOF, '}'); token = getToken()) {
            String s = macro(context);
            if (s != null) {
                macroText += s;
                continue;
            }
            String comment = commentBefore();
            Token enumerator = getToken();
            String spacing2 = " ";
            String separator = first ? "" : ",";
            if (nextToken().match('=')) {
                spacing2 = getToken().spacing;
                countPrefix = " ";
                int count2 = 0;
                Token prevToken = new Token();
                boolean complex = false;
                for (token = nextToken(); !token.match(Token.EOF, ',', '}') || count2 > 0; token = nextToken()) {
                    countPrefix += token.spacing + token;
                    if (token.match('(')) {
                        count2++;
                    } else if (token.match(')')) {
                        count2--;
                    }
                    if (prevToken.match(Token.IDENTIFIER) && token.match('(')) {
                        complex = true;
                    }
                    prevToken = token;
                }
                try {
                    count = Integer.parseInt(countPrefix.trim());
                    countPrefix = " ";
                } catch (NumberFormatException e) {
                    count = 0;
                    if (complex) {
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
            if (comment.length() == 0 && getToken().match(',')) {
                nextToken();
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
        String text = "";
        Token token = nextToken();
        if (token.match(Token.IDENTIFIER)) {
            // XXX: If !isTypedef, this is a variable declaration with anonymous type
            name = token.value;
            token = nextToken();
        }
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        text += enumSpacing + "/** enum " + name + " */\n";
        int newline = enumSpacing.lastIndexOf('\n');
        if (newline >= 0) {
            enumSpacing = enumSpacing.substring(newline + 1);
        }
        if (!Character.isWhitespace(enumerators.charAt(0))) {
            enumerators = " " + enumerators;
        }
        text += enumSpacing + "public static final int" + enumerators + token.expect(';').spacing + ";";
        infoMap.put(new Info(name).valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]").cast(true));
        nextToken();
        return text + macroText + comment;
    }

    String namespace(Context context) throws Exception {
        if (!getToken().match(Token.NAMESPACE)) {
            return null;
        }
        String name = nextToken().expect(Token.IDENTIFIER).value;
        nextToken().expect('{');
        nextToken();

        String text = "";
        context = context.clone();
        context.namespace = context.namespace == null ? name : context.namespace + "::" + name;
        while (!getToken().match(Token.EOF, '}')) {
            text += declaration(context);
        }
        text += getToken().spacing;
        nextToken();
        return text;
    }

    String extern(Context context) throws Exception {
        if (!getToken(0).match(Token.EXTERN) || !getToken(1).match(Token.STRING)) {
            return null;
        }
        nextToken().expect("\"C\"");
        if (!nextToken().match('{')) {
            return "";
        }
        nextToken();

        String text = "";
        while (!getToken().match(Token.EOF, '}')) {
            text += declaration(context);
        }
        nextToken();
        return text;
    }

    String declaration(Context context) throws Exception {
        while (getToken().match(';') && !getToken().match(Token.EOF)) {
            nextToken();
        }
        if (context == null) {
            context = new Context();
        }
        String comment = commentBefore(), text;
        Token token = getToken();
        String spacing = token.spacing;
        TemplateMap map = template(context);
        if (map != context.templateMap) {
            context = context.clone();
            context.templateMap = map;
            comment += spacing.substring(0, spacing.lastIndexOf('\n'));
        }
        if ((text = macro(context))       != null) { return comment + text; }
        if ((text = extern(context))      != null) { return comment + text; }
        if ((text = namespace(context))   != null) { return comment + text; }
        if ((text = enumeration(context)) != null) { return comment + text; }
        if ((text = group(context))       != null) { return comment + text; }
        if ((text = typedef(context))     != null) { return comment + text; }
        if ((text = using(context))       != null) { return comment + text; }
        if ((text = function(context))    != null) { return comment + text; }
        if ((text = variable(context))    != null) { return comment + text; }
        if (attribute()                          ) { return comment + spacing; }
        throw new Exception(token.file + ":" + token.lineNumber + ": Could not parse declaration at '" + token + "'");
    }

    public void parse(String outputFilename, String ... inputFilenames) throws IOException, Exception {
        File[] files = new File[inputFilenames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(inputFilenames[i]);
        }
        parse(new File(outputFilename), files);
    }
    public void parse(File outputFile, File ... inputFiles) throws IOException, Exception {
        ArrayList<Token> tokens = new ArrayList<Token>();
        String lineSeparator = "\n";
        for (File file : inputFiles) {
            LinkedList<Info> infoList = infoMap.get(file.getName());
            if (infoList.size() > 0 && !infoList.getFirst().parse) {
                continue;
            }
            System.out.println("Parsing header file: " + file);
            Token token = new Token();
            token.type = Token.COMMENT;
            token.value = "\n/* Wrapper for header file " + file + " */\n\n";
            tokens.add(token);
            Tokenizer tokenizer = new Tokenizer(file);
            while (!(token = tokenizer.nextToken()).isEmpty()) {
                if (token.type == -1) {
                    token.type = Token.COMMENT;
                }
                tokens.add(token);
            }
            if (lineSeparator == null) {
                lineSeparator = tokenizer.lineSeparator;
            }
            tokenizer.close();
            token = new Token();
            token.type = Token.COMMENT;
            token.spacing = "\n";
            tokens.add(token);
        }
        tokenArray = tokens.toArray(new Token[tokens.size()]);
        tokenIndex = 0;

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
        while (!getToken().match(Token.EOF)) {
            out.append(declaration(null).replaceAll("\n", lineSeparator));
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
