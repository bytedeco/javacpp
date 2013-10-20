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

            .put(new Info("size_t").valueTypes("long").pointerTypes("SizeTPointer"))
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
                Tokenizer tokenizer = new Tokenizer(new StringReader(name));
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
                INLINE    = new Token(IDENTIFIER, "inline"),
                STATIC    = new Token(IDENTIFIER, "static"),
                CLASS     = new Token(IDENTIFIER, "class"),
                STRUCT    = new Token(IDENTIFIER, "struct"),
                UNION     = new Token(IDENTIFIER, "union"),
                TEMPLATE  = new Token(IDENTIFIER, "template"),
                TYPEDEF   = new Token(IDENTIFIER, "typedef"),
                TYPENAME  = new Token(IDENTIFIER, "typename"),
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
                if (prevc == 'f' || prevc == 'F') {
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
    }


    public Parser(Properties properties, InfoMap infoMap) {
        this.properties = properties;
        this.infoMap = infoMap;
    }

    Properties properties = null;
    InfoMap infoMap = null;
    Token[] tokenArray = null;
    int tokenIndex = 0;

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
                    Tokenizer tokenizer = new Tokenizer(new StringReader(info.text));
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
            expandToken(index);
            if (!tokenArray[index].match(Token.COMMENT) && --count < 0) {
                break;
            }
            index++;
        }
        expandToken(index);
        return index;
    }

    Token getToken() {
        return getToken(0);
    }
    Token getToken(int i) {
        return getToken(i, true);
    }
    Token getToken(int i, boolean preprocess) {
        int k = preprocess ? preprocessToken(tokenIndex, i) : tokenIndex + i;
        return k < tokenArray.length ? tokenArray[k] : Token.EOF;
    }
    Token nextToken() {
        return nextToken(true);
    }
    Token nextToken(boolean preprocess) {
        tokenIndex = preprocess ? preprocessToken(tokenIndex, 1) : tokenIndex + 1;
        return tokenIndex < tokenArray.length ? tokenArray[tokenIndex] : Token.EOF;
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
            if (token.match(Token.CLASS, Token.TYPENAME)) {
                map.put(nextToken().expect(Token.IDENTIFIER).value, null);
            }
            if (nextToken().expect(',', '>').match('>')) {
                nextToken();
                break;
            }
        }
        return map;
    }


    static class Declarator {
        int infoNumber = 0, indices = 0;
        boolean constValue = false, constPointer = false;
        String annotations = "", cppType = "", javaType = "", objectName = "", convention = "", definitions = "", parameters = "";
    }

    Declarator declarator(Context context, String defaultName, int infoNumber,
            int varNumber, boolean arrayAsPointer, boolean pointerAsArray) throws Exception {
        boolean isTypedef = getToken().match(Token.TYPEDEF);
        Declarator decl = new Declarator();
        int count = 0;
        String cppName = "";
        boolean anonymous = false;
        boolean simpleType = false;
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match("::")) {
                cppName += token;
            } else if (token.match('<')) {
                cppName += token;
                count++;
            } else if (token.match('>')) {
                cppName += token;
                count--;
            } else if (!token.match(Token.IDENTIFIER)) {
                if (token.match('}')) {
                    anonymous = true;
                    nextToken();
                }
                break;
            } else if (token.match(Token.CONST)) {
                decl.constValue = true;
                continue;
            } else if (token.match(Token.TYPEDEF, Token.ENUM, Token.CLASS, Token.STRUCT, Token.UNION)) {
                continue;
            } else if (token.match("signed", "unsigned", "char", "short", "int", "long", "bool", "float", "double")) {
                if (!simpleType && count == 0) {
                    cppName = token.value + " ";
                } else {
                    cppName += token.value + " ";
                }
                simpleType = true;
            } else {
                LinkedList<Info> infoList = infoMap.get(token.value);
                if (infoList.size() > 0 && infoList.getFirst().annotations != null) {
                    for (String s : infoList.getFirst().annotations) {
                        decl.annotations += s + " ";
                    }
                } else if (cppName.length() > 0 && !cppName.endsWith("::") && count == 0) {
                    infoList = infoMap.get(getToken(1).value);
                    if ((infoList.size() > 0 && infoList.getFirst().annotations != null) ||
                            !getToken(1).match('*', '&', Token.IDENTIFIER, Token.CONST)) {
                        // we probably reached a variable or function name identifier
                        break;
                    }
                } else {
                    if (cppName.endsWith("::") || count > 0) {
                        cppName += token.value;
                    } else {
                        cppName = token.value;
                    }
                }
            }
        }
        cppName = cppName.trim();
        if (context.templateMap != null && context.templateMap.containsKey(cppName)) {
            cppName = context.templateMap.get(cppName);
        }
        decl.cppType = decl.javaType = cppName;
        if ("...".equals(getToken().value)) {
            nextToken();
            return null;
        }

        count = 0;
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

        String cast = cppName;
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
        decl.objectName = defaultName;
        if (getToken().match('(')) {
            while (getToken().match('(')) { 
                nextToken();
            }
            for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
                if (token.match(Token.IDENTIFIER)) {
                    decl.objectName = token.value;
                } else if (token.match('*')) {
                    indirections2++;
                    decl.convention = decl.objectName;
                    decl.objectName = defaultName;
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
            decl.objectName = getToken().value;
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
        if (pointerAsArray && indirections > (anonymous ? 0 : 1)) {
            // treat second indirection as an array, unless anonymous
            dims[decl.indices++] = -1;
            indirections--;
            cast = cast.substring(0, cast.length() - 1);
        }

        if (getToken().match(':')) {
            // ignore bitfields
            decl.annotations += "@NoOffset ";
            nextToken().expect(Token.INTEGER);
            nextToken().expect(',', ';');
        }

        int infoLength = 1;
        boolean valueType = false, needCast = false, implicitConst = false;
        String prefix = decl.constValue && indirections < 2 && !reference ? "const " : "";
        LinkedList<Info> infoList = infoMap.get(prefix + decl.cppType);
        String ns = "";
        while (context.namespace != null && infoList.size() == 0 && !ns.equals(context.namespace)) {
            int i = context.namespace.indexOf("::", ns.length() + 2);
            ns = i < 0 ? context.namespace : context.namespace.substring(0, i);
            infoList = infoMap.get(prefix + ns + "::" + decl.cppType);
        }
        if (ns.length() > 0) {
            cast = ns + "::" + cast;
        }
        if (infoList.size() > 0) {
            Info info = infoList.getFirst();
            valueType = info.valueTypes != null &&
                    ((indirections == 0 && !reference) || info.pointerTypes == null);
            needCast = info.cast;
            implicitConst = info.cppNames[0].startsWith("const ");
            infoLength = valueType ? info.valueTypes.length :
                    info.pointerTypes != null ? info.pointerTypes.length : 1;
            decl.infoNumber = infoNumber < 0 ? 0 : infoNumber % infoLength;
            decl.javaType = valueType ? info.valueTypes[decl.infoNumber] :
                    info.pointerTypes != null ? info.pointerTypes[decl.infoNumber] : decl.cppType;
        }

        if (!valueType) {
            if (indirections == 0 && !reference) {
                decl.annotations += "@ByVal ";
            } else if (indirections == 0 && reference) {
                decl.annotations += "@ByRef ";
            } else if (indirections == 1 && reference) {
                decl.annotations += "@ByPtrRef ";
            } else if (indirections == 2 && !reference && infoNumber >= 0) {
                decl.annotations += "@ByPtrPtr ";
                if (decl.cppType.equals("void")) {
                    needCast = true;
                }
            } else if (indirections >= 2) {
                decl.infoNumber += infoLength;
                needCast = true;
                decl.javaType = "PointerPointer";
                if (reference) {
                    decl.annotations += "@ByRef ";
                }
            }

            if (!needCast && decl.constValue && !implicitConst) {
                decl.annotations = "@Const " + decl.annotations;
            }
        }
        if (needCast || (arrayAsPointer && decl.indices > 1)) {
            if (decl.constValue) {
                cast = "const " + cast;
            }
            if (!valueType && indirections == 0 && !reference) {
                decl.annotations += "@Cast(\"" + cast + "*\") ";
            } else {
                decl.annotations = "@Cast(\"" + cast + "\") " + decl.annotations;
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
                String functionType = isTypedef ? decl.objectName :
                        Character.toUpperCase(decl.objectName.charAt(0)) + decl.objectName.substring(1) + params.signature;
                if (infoNumber <= params.infoNumber) {
                    decl.definitions +=
                            "public static class " + functionType + " extends FunctionPointer {\n" +
                            "    static { Loader.load(); }\n" +
                            "    public    " + functionType + "(Pointer p) { super(p); }\n" +
                            "    protected " + functionType + "() { allocate(); }\n" +
                            "    private native void allocate();\n" +
                            "    public native " + decl.annotations + decl.javaType + " call" + params.list + ";\n" +
                            "}\n";
                }
                decl.annotations = "";
                decl.javaType = functionType;
                decl.parameters = "";
            }
        }

        infoList = infoMap.get(decl.objectName);
        if (infoList.size() > 0) {
            Info info = infoList.getFirst();
            if (info.javaNames != null && info.javaNames.length > 0) {
                decl.annotations += "@Name(\"" + decl.objectName + "\") ";
                decl.objectName = info.javaNames[0];
            }
        }
        return decl;
    }

    String commentBefore() throws Exception {
        String comment = "";
        while (tokenIndex > 0 && getToken(-1, false).match(Token.COMMENT)) {
            tokenIndex--;
        }
        for (Token token = getToken(0, false); token.match(Token.COMMENT); token = nextToken(false)) {
            if (token.value.length() <= 3 || token.value.charAt(3) != '<') {
                comment += token.spacing + token.value;
            }
        }
        return comment;
    }

    String commentAfter() throws Exception {
        String comment = "";
        while (tokenIndex > 0 && getToken(-1, false).match(Token.COMMENT)) {
            tokenIndex--;
        }
        for (Token token = getToken(0, false); token.match(Token.COMMENT); token = nextToken(false)) {
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
        for (Token token = nextToken(); !token.match(Token.EOF) && count > 0; token = nextToken(false)) {
            if (token.match('(')) {
                count++;
            } else if (token.match(')')) {
                count--;
            }
        }
        return true;
    }

    boolean body() throws Exception {
        if (!getToken().match('{')) {
            return false;
        }

        int count = 1;
        for (Token token = nextToken(); !token.match(Token.EOF) && count > 0; token = nextToken(false)) {
            if (token.match('{')) {
                count++;
            } else if (token.match('}')) {
                count--;
            }
        }
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
            if (decl != null && !decl.javaType.equals("void")) {
                params.infoNumber = Math.max(params.infoNumber, decl.infoNumber);
                params.list += (count > 1 ? "," : "") + spacing + decl.annotations + decl.javaType + " " + decl.objectName;
                params.definitions += decl.definitions;
                params.signature += '_';
                for (char c : decl.javaType.substring(decl.javaType.lastIndexOf(' ') + 1).toCharArray()) {
                    if (Character.isDigit(c) || Character.isLetter(c) || c == '_') {
                        params.signature += c;
                    }
                }
                params.names += (count > 1 ? ", " : "") + decl.objectName;
                if (decl.objectName.startsWith("arg")) {
                    try {
                        count = Integer.parseInt(decl.objectName.substring(3)) + 1;
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
        boolean destructor = false;
        String spacing = getToken().spacing;
        String access = context.group == null || context.group.length() == 0 ? "public static native " : "public native ";
        for (Token token = getToken(); !token.match(Token.EOF); token = nextToken()) {
            if (token.match(Token.STATIC)) {
                access = "public static native ";
            } else if (token.match('~')) {
                destructor = true;
            } else if (!token.match(Token.INLINE, Token.VIRTUAL)) {
                break;
            }
        }
        int startIndex = tokenIndex;
        Parameters params = null;
        Declarator decl = null;
        String name = null;
        if (constructor || destructor) {
            name = getToken().value;
            nextToken();
            params = parameters(context, 0);
            if (!name.equals(context.group) || params == null) {
                tokenIndex = backIndex;
                return null;
            }
            decl = new Declarator();
            decl.parameters = params.list;
            decl.definitions = params.definitions;
        } else {
            decl = declarator(context, null, 0, 0, false, false);
            name = decl.objectName;
        }
        if (name == null || decl.parameters.length() == 0) {
            tokenIndex = backIndex;
            return null;
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
                tokenIndex = startIndex;
                if (constructor || destructor) {
                    name = getToken().value;
                    nextToken();
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
                }
                boolean found = false;
                for (Declarator d : prevDecl) {
                    found |= /* decl.javaType.equals(d.javaType) && */ decl.parameters.equals(d.parameters);
                }
                if (found && n > 0) {
                    break;
                }
                if (name.length() > 0 && !found && !destructor) {
                    if (context.namespace != null && context.group == null) {
                        text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    if (constructor) {
                        text += "public " + name + decl.parameters + " { allocate" + params.names + "; }\n" +
                                "private native void allocate" + decl.parameters + ";\n";
                    } else {
                        text += access + decl.annotations + decl.javaType + " " + name + decl.parameters + ";\n";
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
        if (getToken().match(Token.EXTERN)) {
            nextToken();
        }
        int startIndex = tokenIndex;
        Declarator decl = declarator(context, null, 0, 0, false, true);
        String name = decl.objectName;
        if (name == null || !getToken().match('[', '=', ',', ':', ';')) {
            tokenIndex = backIndex;
            return null;
        }

        String text  = "";
        String definitions = "";
        for (Declarator metadecl : context.variables != null ? context.variables : new Declarator[] { null }) {
            for (int n = 0; n < Integer.MAX_VALUE; n++) {
                tokenIndex = startIndex;
                decl = declarator(context, null, -1, n, false, true);
                if (decl == null) {
                    break;
                }
                String access = context.group == null || context.group.length() == 0 ? "public static native " : "public native ";
                String setterType = context.group == null || context.group.length() == 0 ? "void " : context.group + " ";
                name = decl.objectName;
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
                    if (metadecl != null && metadecl.objectName.length() > 0) {
                        text += "@Name({\"" + metadecl.objectName + "\", \"." + decl.objectName + "\"}) ";
                        name = metadecl.objectName + "_" + decl.objectName;
                    }
                    if (decl.constValue) {
                        text += "@MemberGetter ";
                    }
                    text += access + decl.annotations + decl.javaType + " " + name + "(" + indices + ");";
                    if (!decl.constValue) {
                        if (indices.length() > 0) {
                            indices += ", ";
                        }
                        text += " " + access + setterType + name + "(" + indices + decl.javaType + " " + name + ");";
                    }
                    text += "\n";
                    definitions += decl.definitions;
                }
                if (decl.indices > 0) {
                    // in the case of arrays, also add a pointer accessor
                    tokenIndex = startIndex;
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
                    if (metadecl != null && metadecl.objectName.length() > 0) {
                        text += "@Name({\"" + metadecl.objectName + "\", \"." + decl.objectName + "\"}) ";
                        name = metadecl.objectName + "_" + decl.objectName;
                    }
                    text += "@MemberGetter " + access + decl.annotations + decl.javaType + " " + name + "(" + indices + ");\n";
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
        String spacing = getToken().spacing;
        Token keyword = nextToken(false);

        nextToken(false);
        int beginIndex = tokenIndex;
        for (Token token = getToken(0, false); !token.match(Token.EOF); token = nextToken(false)) {
            if (token.spacing.indexOf('\n') >= 0) {
                break;
            }
        }
        int endIndex = tokenIndex;

        String text = "";
        if (keyword.match(Token.DEFINE) && beginIndex + 1 < endIndex) {
            tokenIndex = beginIndex;
            String macroName = getToken(0, false).value;
            Token first = nextToken(false);
            LinkedList<Info> infoList = infoMap.get(macroName);
            if (first.spacing.length() == 0 && first.match('(')) {
                if (infoList.size() == 0) {
                    Info info = new Info(macroName).genericArgs(new String[endIndex - tokenIndex]).text("");
                    int count = 0;
                    for (Token token = getToken(0, false); tokenIndex < endIndex; token = nextToken(false)) {
                        if (token.match(Token.IDENTIFIER)) {
                            info.genericArgs[count++] = token.value;
                        } else if (token.match(')')) {
                            break;
                        }
                    }
                    info.genericArgs = Arrays.copyOf(info.genericArgs, count);
                    for (Token token = nextToken(false); tokenIndex < endIndex; token = nextToken(false)) {
                        info.text += token.spacing + token;
                    }
                    infoMap.put(info);
                } else
                // declare as a static native methods
                for (Info info : infoList) {
                    if (info.genericArgs == null || info.genericArgs.length == 0 || info.text != null) {
                        continue;
                    }

                    int count = 1;
                    tokenIndex = beginIndex + 2;
                    String params = "(";
                    for (Token token = getToken(0, false); tokenIndex < endIndex &&
                            count < info.genericArgs.length; token = nextToken(false)) {
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

                    String type = info.genericArgs[0];
                    for (int i = 0; i < info.cppNames.length; i++) {
                        if (macroName.equals(info.cppNames[i]) && info.javaNames != null) {
                            macroName = "@Name(\"" + info.cppNames[0] + "\") " + info.javaNames[i];
                            break;
                        }
                    }
                    text += "public static native " + type + " " + macroName + params + ";\n";
                }
            } else if (infoList.size() == 0 ||
                       infoList.getFirst().genericArgs == null ||
                       infoList.getFirst().genericArgs.length > 0) {
                // declare as a static final variable
                String value = "";
                String type = "int";
                String cat = "";
                tokenIndex = beginIndex + 1;
                Token prevToken = new Token();
                boolean complex = false;
                for (Token token = getToken(0, false); tokenIndex < endIndex; token = nextToken(false)) {
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
                if (infoList.size() > 0) {
                    Info info = infoList.getFirst();
                    if (info.genericArgs != null) {
                        type = info.genericArgs[0];
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
                    while (getToken(endIndex - tokenIndex - 1, false).match(Token.COMMENT)) {
                        endIndex--;
                    }
                    for (Token token = getToken(0, false); tokenIndex < endIndex; token = nextToken(false)) {
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

            if (text.length() > 0) {
                tokenIndex = endIndex;
                String comment = commentAfter();
                text = rescan(comment + text, spacing);
            }
        } else if (keyword.match(Token.IF, Token.IFDEF, Token.IFNDEF, Token.ELIF) && beginIndex < endIndex) {
            tokenIndex = beginIndex;
            String value = "";
            for (Token token = getToken(0, false); tokenIndex < endIndex; token = nextToken(false)) {
                value += token.spacing + token;
            }
            int n = spacing.lastIndexOf('\n') + 1;
            text = spacing.substring(0, n) + "// " + spacing.substring(n) + "#" + keyword.spacing + keyword + value;
            LinkedList<Info> infoList = infoMap.get(value);
            boolean define = true;
            if (infoList.size() > 0) {
                Info info = infoList.getFirst();
                define = keyword.match(Token.IFNDEF) ? !info.define : info.define;
            }
            if (!define) {
                int count = 1;
                Token prevToken = new Token();
                for (Token token = getToken(0, false); !token.match(Token.EOF) && count > 0; token = nextToken(false)) {
                    if (prevToken.match('#') && token.match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
                        count++;
                    } else if (prevToken.match('#') && token.match(Token.ELIF, Token.ELSE, Token.ENDIF)) {
                        count--;
                    }
                    prevToken = token;
                }
                tokenIndex -= 2;
            }
        }

        if (text.length() == 0) {
            // output whatever we did not process as comment
            tokenIndex = beginIndex;
            while (getToken(endIndex - tokenIndex - 1, false).match(Token.COMMENT)) {
                endIndex--;
            }
            int n = spacing.lastIndexOf('\n') + 1;
            text += "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
            for (Token token = getToken(0, false); tokenIndex < endIndex; token = nextToken(false)) {
                text += token.match("\n") ? "\n// " : token.spacing + token;
            }
            String comment = commentAfter();
            text = spacing.substring(0, n) + comment + text;
        }
        return text;
    }

    String typedef(Context context) throws Exception {
        if (!getToken().match(Token.TYPEDEF)) {
            return null;
        }
        String spacing = getToken().spacing;
        Declarator decl = declarator(context, null, 0, 0, true, false);
        nextToken();

        String name = decl.objectName;
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        if (decl.definitions.length() > 0) {
            infoMap.put(new Info(name).valueTypes(decl.objectName));
        } else if (decl.cppType.equals("void")) {
            decl.definitions =
                    "@Opaque public static class " + name + " extends Pointer {\n" +
                    "    public " + name + "() { }\n" +
                    "    public " + name + "(Pointer p) { super(p); }\n" +
                    "}";
        } else {
            LinkedList<Info> infoList = infoMap.get(decl.cppType);
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

        if (!getToken(1).match('{') && getToken(2).match(Token.IDENTIFIER)
                && (isTypedef || !getToken(3).match(';'))) {
            nextToken();
        }
        String text = "";
        String name = nextToken().expect(Token.IDENTIFIER, '{').value;
        String parent = "Pointer";
        if (getToken(0).match(Token.IDENTIFIER) && getToken(1).match(':')) {
            for (Token token = nextToken(); !token.match(Token.EOF); token = nextToken()) {
                if (token.match('{')) {
                    break;
                } else if (token.match(Token.PUBLIC)) {
                    parent = nextToken().expect(Token.IDENTIFIER).value;
                }
            }
        }
        if (!getToken(0).match('{', ';') && !getToken(1).match('{', ';')) {
            tokenIndex = backIndex;
            return null;
        }
        LinkedList<Info> infoList = infoMap.get(name);
        if (getToken().match(Token.IDENTIFIER) && nextToken().match(';')) {
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
        if (context.namespace != null) {
            name = context.namespace + "::" + name;
        }
        infoMap.put(new Info(name));
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
