/*
 * Copyright (C) 2013-2018 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.javacpp.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bytedeco.javacpp.ClassProperties;
import org.bytedeco.javacpp.Loader;

/**
 * The Parser, just like the Generator, is a mess that is not meant to support the
 * entirety of C++, but an appropriate subset as used by typical C/C++ header files.
 * To figure out what that subset is and what the output should be, the idea is to
 * apply it on as many C/C++ libraries as possible, and patch the code as we go.
 * At one point in time, when this prototype code appears to have stabilized, we can
 * start to redesign it in a more meaningful way.
 * <p>
 * That said, to understand how it is supposed to function in its present state,
 * one can step through the code at runtime: It is quite friendly to debuggers.
 * <p>
 * Moreover, it relies on {@link Info} objects created as part of the execution
 * of {@link InfoMapper#map(InfoMap)}. We can understand better how the parsing
 * is supposed to get accomplished by studying that documentation as well.
 * <p>
 * To do:
 * - Inherit constructors from helper classes, if possible
 * - etc.
 *
 * @see Info
 * @see InfoMap
 * @see InfoMapper
 *
 * @author Samuel Audet
 */
public class Parser {

    public Parser(Logger logger, Properties properties) {
        this(logger, properties, null, null);
    }
    public Parser(Logger logger, Properties properties, String encoding, String lineSeparator) {
        this.logger = logger;
        this.properties = properties;
        this.encoding = encoding;
        this.lineSeparator = lineSeparator;
    }
    Parser(Parser p, String text) {
        this.logger = p.logger;
        this.properties = p.properties;
        this.encoding = p.encoding;
        this.infoMap = p.infoMap;
        Token t = p.tokens != null ? p.tokens.get() : Token.EOF;
        this.tokens = new TokenIndexer(infoMap, new Tokenizer(text, t.file, t.lineNumber).tokenize(), false);
        this.lineSeparator = p.lineSeparator;
    }

    final Logger logger;
    final Properties properties;
    final String encoding;
    InfoMap infoMap = null;
    InfoMap leafInfoMap = null;
    TokenIndexer tokens = null;
    String lineSeparator = null;

    String translate(String text) {
        Info info = infoMap.getFirst(text);
        if (info != null && info.javaNames != null && info.javaNames.length > 0) {
            return info.javaNames[0];
        }
        int namespace = text.lastIndexOf("::");
        if (namespace >= 0) {
            Info info2 = infoMap.getFirst(text.substring(0, namespace));
            String localName = text.substring(namespace + 2);
            if (info2 != null && info2.pointerTypes != null) {
                text = info2.pointerTypes[0] + "." + localName;
            } else if (localName.length() > 0 && Character.isJavaIdentifierStart(localName.charAt(0))) {
                for (char c : localName.toCharArray()) {
                    if (!Character.isJavaIdentifierPart(c)) {
                        localName = null;
                        break;
                    }
                }
                if (localName != null) {
                    text = localName;
                }
            }
        }
        int castStart = text.lastIndexOf('(');
        int castEnd = text.indexOf(')', castStart);
        if (castStart >= 0 && castStart < castEnd) {
            Info info2 = infoMap.getFirst(text.substring(castStart + 1, castEnd));
            if (info2 != null && info2.valueTypes != null && info2.valueTypes.length > 0) {
                text = text.substring(0, castStart + 1) + info2.valueTypes[0] + text.substring(castEnd);
            }
        }
        return text;
    }

    void containers(Context context, DeclarationList declList) throws ParserException {
        List<String> basicContainers = new ArrayList<String>();
        for (Info info : infoMap.get("basic/containers")) {
            basicContainers.addAll(Arrays.asList(info.cppTypes));
        }
        for (String containerName : basicContainers) {
            LinkedHashSet<Info> infoSet = new LinkedHashSet<Info>();
            infoSet.addAll(leafInfoMap.get("const " + containerName));
            infoSet.addAll(leafInfoMap.get(containerName));
            for (Info info : infoSet) {
                Declaration decl = new Declaration();
                if (info == null || info.skip || !info.define) {
                    continue;
                }
                int dim = containerName.endsWith("pair") ? 0 : 1;
                boolean constant = info.cppNames[0].startsWith("const "), resizable = !constant;
                Type containerType = new Parser(this, info.cppNames[0]).type(context),
                        indexType, valueType, firstType = null, secondType = null;
                if (containerType.arguments == null || containerType.arguments.length == 0 || containerType.arguments[0] == null
                        || containerType.arguments[containerType.arguments.length - 1] == null) {
                    continue;
                } else if (containerType.arguments.length > 1 && containerType.arguments[1].javaName.length() > 0) {
                    resizable = false;
                    indexType = containerType.arguments[0];
                    valueType = containerType.arguments[1];
                } else {
                    resizable &= containerType.arguments.length == 1; // assume second argument is the fixed size
                    indexType = new Type();
                    indexType.annotations = "@Cast(\"size_t\") ";
                    indexType.cppName = "size_t";
                    indexType.javaName = "long";
                    valueType = containerType.arguments[0];
                }
                String indexFunction = "(function = \"at\")";
                boolean list = resizable; // also vector, etc
                if (valueType.javaName == null || valueType.javaName.length() == 0
                        || containerName.endsWith("bitset")) {
                    indexFunction = "";
                    valueType.javaName = "boolean";
                    resizable = false;
                } else if (containerName.endsWith("list") || containerName.endsWith("set")) {
                    indexType = null;
                    resizable = false;
                    list = containerName.endsWith("list");
                } else if (!constant && !resizable) {
                    indexFunction = ""; // maps need operator[] to be writable
                }
                while (valueType.cppName.startsWith(containerName)
                        && leafInfoMap.get(valueType.cppName, false).size() == 0) {
                    // increase dimension, unless the user has provided info for the intermediate type
                    dim++;
                    valueType = valueType.arguments[0];
                }
                int valueTemplate = valueType.cppName.indexOf("<");
                if (containerName.endsWith("pair")) {
                    firstType = containerType.arguments[0];
                    secondType = containerType.arguments[1];
                } else if (valueTemplate >= 0 && valueType.cppName.substring(0, valueTemplate).endsWith("pair")) {
                    firstType = valueType.arguments[0];
                    secondType = valueType.arguments[1];
                }
                if (firstType != null && (firstType.annotations == null || firstType.annotations.length() == 0)) {
                    firstType.annotations = (firstType.constValue ? "@Const " : "") + (firstType.indirections == 0 && !firstType.value ? "@ByRef " : "");
                }
                if (secondType != null && (secondType.annotations == null || secondType.annotations.length() == 0)) {
                    secondType.annotations = (secondType.constValue ? "@Const " : "") + (secondType.indirections == 0 && !secondType.value ? "@ByRef " : "");
                }
                if (indexType != null && (indexType.annotations == null || indexType.annotations.length() == 0)) {
                    indexType.annotations = (indexType.constValue ? "@Const " : "") + (indexType.indirections == 0 && !indexType.value ? "@ByRef " : "");
                }
                if (valueType != null && (valueType.annotations == null || valueType.annotations.length() == 0)) {
                    valueType.annotations = (valueType.constValue ? "@Const " : "") + (valueType.indirections == 0 && !valueType.value ? "@ByRef " : "");
                }
                Info valueInfo = infoMap.getFirst(valueType.cppName);
                if (valueInfo != null && valueInfo.cast) {
                    String cast = valueType.cppName;
                    if (valueType.constValue && !cast.startsWith("const ")) {
                        cast = "const " + cast;
                    }
                    if (valueType.constPointer && !cast.endsWith(" const")) {
                        cast = cast + " const";
                    }
                    if (valueType.indirections > 0) {
                        for (int i = 0; i < valueType.indirections; i++) {
                            cast += "*";
                        }
                    } else if (!valueType.value) {
                        cast += "*";
                    }
                    if (valueType.reference) {
                        cast += "&";
                    }
                    valueType.annotations = "@Cast(\"" + cast + "\") " + valueType.annotations;
                }
                String arrayBrackets = "";
                for (int i = 0; i < dim - 1; i++) {
                    arrayBrackets += "[]";
                }
                decl.type = new Type(containerType.javaName);
                decl.text += (dim == 0 ? "\n@NoOffset " : "\n")
                        + "@Name(\"" + containerType.cppName + "\") public static class " + containerType.javaName + " extends Pointer {\n"
                        + "    static { Loader.load(); }\n"
                        + "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n"
                        + "    public " + containerType.javaName + "(Pointer p) { super(p); }\n";
                if (!constant && (dim == 0 || containerType.arguments.length == 1) && firstType != null && secondType != null) {
                    String[] firstNames = firstType.javaNames != null ? firstType.javaNames : new String[] {firstType.javaName};
                    String[] secondNames = secondType.javaNames != null ? secondType.javaNames : new String[] {secondType.javaName};
                    String brackets = arrayBrackets + (dim > 0 ? "[]" : "");
                    for (int n = 0; n < firstNames.length || n < secondNames.length; n++) {
                        decl.text += "    public " + containerType.javaName + "(" + firstNames[Math.min(n, firstNames.length - 1)] + brackets + " firstValue, "
                                                                                  + secondNames[Math.min(n, secondNames.length - 1)] + brackets + " secondValue) "
                                  +  "{ this(" + (dim > 0 ? "Math.min(firstValue.length, secondValue.length)" : "") + "); put(firstValue, secondValue); }\n";
                    }
                } else if (resizable && firstType == null && secondType == null) {
                    for (String javaName : valueType.javaNames != null ? valueType.javaNames : new String[] {valueType.javaName}) {
                        if (dim < 2 && !javaName.equals("int") && !javaName.equals("long")) {
                            decl.text += "    public " + containerType.javaName + "(" + javaName + " value) { this(1); put(0, value); }\n";
                        }
                        decl.text += "    public " + containerType.javaName + "(" + javaName + arrayBrackets + " ... array) { this(array.length); put(array); }\n";
                    }
                }
                decl.text += "    public " + containerType.javaName + "()       { allocate();  }\n" + (!resizable ? ""
                           : "    public " + containerType.javaName + "(long n) { allocate(n); }\n")
                           + "    private native void allocate();\n"                                + (!resizable ? ""
                           : "    private native void allocate(@Cast(\"size_t\") long n);\n")
                           + "    public native @Name(\"operator=\") @ByRef " + containerType.javaName + " put(@ByRef " + containerType.javaName + " x);\n\n";

                for (int i = 0; i < dim; i++) {
                    String indexAnnotation = i > 0 ? ("@Index(" + (i > 1 ? "value = " + i + ", " : "" ) + "function = \"at\") ") : "";
                    String indices = "", indices2 = "", separator = "";
                    for (int j = 0; indexType != null && j < i; j++) {
                        indices += separator + indexType.annotations + indexType.javaName + " " + (char)('i' + j);
                        indices2 += separator + (char)('i' + j);
                        separator = ", ";
                    }

                    decl.text += "    public boolean empty(" + indices + ") { return size(" + indices2 + ") == 0; }\n"
                               + "    public native " + indexAnnotation + "long size(" + indices + ");\n"  + (!resizable ? ""
                               : "    public void clear(" + indices + ") { resize(" + indices2 + separator + "0); }\n"
                               + "    public native " + indexAnnotation + "void resize(" + indices + separator + "@Cast(\"size_t\") long n);\n");
                }

                String params = "", separator = "";
                for (int i = 0; indexType != null && i < dim; i++) {
                    params += separator + indexType.annotations + indexType.javaName + " " + (char)('i' + i);
                    separator = ", ";
                }

                if (firstType != null && secondType != null) {
                    String indexAnnotation = dim == 0 ? "@MemberGetter " : "@Index(" + (dim > 1 ? "value = " + dim + ", " : "") + "function = \"at\") ";
                    decl.text += "\n"
                              +  "    " + indexAnnotation + "public native " + firstType.annotations + firstType.javaName + " first(" + params + ");"
                              +  " public native " + containerType.javaName + " first(" + params + separator + firstType.javaName.substring(firstType.javaName.lastIndexOf(' ') + 1) + " first);\n"
                              +  "    " + indexAnnotation + "public native " + secondType.annotations + secondType.javaName + " second(" + params + "); "
                              +  " public native " + containerType.javaName + " second(" + params + separator + secondType.javaName.substring(secondType.javaName.lastIndexOf(' ') + 1) + " second);\n";
                    for (int i = 1; !constant && firstType.javaNames != null && i < firstType.javaNames.length; i++) {
                        decl.text += "    @MemberSetter @Index" + indexFunction + " public native " + containerType.javaName + " first(" + params + separator + firstType.annotations + firstType.javaNames[i] + " first);\n";
                    }
                    for (int i = 1; !constant && secondType.javaNames != null && i < secondType.javaNames.length; i++) {
                        decl.text += "    @MemberSetter @Index" + indexFunction + " public native " + containerType.javaName + " second(" + params + separator + secondType.annotations + secondType.javaNames[i] + " second);\n";
                    }
                } else {
                    if (indexType != null) {
                        decl.text += "\n"
                                  +  "    @Index" + indexFunction + " public native " + valueType.annotations + valueType.javaName + " get(" + params + ");\n";
                        if (!constant) {
                            decl.text += "    public native " + containerType.javaName + " put(" + params + separator + valueType.javaName.substring(valueType.javaName.lastIndexOf(' ') + 1) + " value);\n";
                        }
                        for (int i = 1; !constant && valueType.javaNames != null && i < valueType.javaNames.length; i++) {
                            decl.text += "    @ValueSetter @Index" + indexFunction + " public native " + containerType.javaName + " put(" + params + separator + valueType.annotations + valueType.javaNames[i] + " value);\n";
                        }
                    }
                    if (dim == 1 && !containerName.endsWith("bitset") && containerType.arguments.length >= 1 && containerType.arguments[containerType.arguments.length - 1].javaName.length() > 0) {
                        decl.text += "\n";
                        if (!constant) {
                            if (list) {
                                decl.text += "    public native @ByVal Iterator insert(@ByVal Iterator pos, " + valueType.annotations + valueType.javaName + " value);\n"
                                          +  "    public native @ByVal Iterator erase(@ByVal Iterator pos);\n";
                            } else if (indexType == null) {
                                decl.text += "    public native void insert(" + valueType.annotations + valueType.javaName + " value);\n"
                                          +  "    public native void erase(" + valueType.annotations + valueType.javaName + " value);\n";
                            }
                            // XXX: else need to figure out something for maps
                        }
                        if (indexType != null && !indexType.annotations.contains("@Const") && !indexType.annotations.contains("@Cast") && !indexType.value) {
                            indexType.annotations += "@Const ";
                        }
                        if (!valueType.annotations.contains("@Const") && !valueType.value) {
                            valueType.annotations += "@Const ";
                        }
                        decl.text += "    public native @ByVal Iterator begin();\n"
                                  +  "    public native @ByVal Iterator end();\n"
                                  +  "    @NoOffset @Name(\"iterator\") public static class Iterator extends Pointer {\n"
                                  +  "        public Iterator(Pointer p) { super(p); }\n"
                                  +  "        public Iterator() { }\n\n"

                                  +  "        public native @Name(\"operator++\") @ByRef Iterator increment();\n"
                                  +  "        public native @Name(\"operator==\") boolean equals(@ByRef Iterator it);\n"
                                  +  (containerType.arguments.length > 1 ?
                                         "        public native @Name(\"operator*().first\") @MemberGetter " + indexType.annotations + indexType.javaName + " first();\n"
                                       + "        public native @Name(\"operator*().second\") @MemberGetter " + valueType.annotations + valueType.javaName + " second();\n"
                                  :
                                         "        public native @Name(\"operator*\") " + valueType.annotations + valueType.javaName + " get();\n")
                                  +  "    }\n";
                    }
                    if (resizable) {
                        decl.text += "\n"
                                  +  "    public " + valueType.javaName + arrayBrackets + "[] get() {\n";
                        String indent = "        ", indices = "", args = "", brackets = arrayBrackets;
                        separator = "";
                        for (int i = 0; i < dim; i++) {
                            char c = (char)('i' + i);
                            decl.text +=
                                    indent + (i == 0 ? valueType.javaName + brackets + "[] " : "") + "array" + indices + " = new " + valueType.javaName
                                                + "[size(" + args + ") < Integer.MAX_VALUE ? (int)size(" + args + ") : Integer.MAX_VALUE]" + brackets + ";\n" +
                                    indent + "for (int " + c + " = 0; " + c + " < array" + indices + ".length; " + c + "++) {\n";
                            indent += "    ";
                            indices += "[" + c + "]";
                            args += separator + c;
                            brackets = brackets.length() < 2 ? "" : brackets.substring(2);
                            separator = ", ";
                        }
                        decl.text += indent + "array" + indices + " = get(" + args + ");\n";
                        for (int i = 0; i < dim; i++) {
                            indent = indent.substring(4);
                            decl.text += indent + "}\n";
                        }
                        decl.text += "        return array;\n"
                                  +  "    }\n"
                                  +  "    @Override public String toString() {\n"
                                  +  "        return java.util.Arrays." + (dim < 2 ? "toString" : "deepToString" ) + "(get());\n"
                                  +  "    }\n";
                    }
                }

                if (!constant && (dim == 0 || containerType.arguments.length == 1) && firstType != null && secondType != null) {
                    String[] firstNames = firstType.javaNames != null ? firstType.javaNames : new String[] {firstType.javaName};
                    String[] secondNames = secondType.javaNames != null ? secondType.javaNames : new String[] {secondType.javaName};
                    String brackets = arrayBrackets + (dim > 0 ? "[]" : "");
                    for (int n = 0; n < firstNames.length || n < secondNames.length; n++) {
                        decl.text += "\n"
                                  +  "    public " + containerType.javaName + " put(" + firstNames[Math.min(n, firstNames.length - 1)] + brackets + " firstValue, "
                                                                                     + secondNames[Math.min(n, secondNames.length - 1)] + brackets + " secondValue) {\n";
                        String indent = "        ", indices = "", args = "";
                        separator = "";
                        for (int i = 0; i < dim; i++) {
                            char c = (char)('i' + i);
                            decl.text +=
                                    indent + "for (int " + c + " = 0; " + c + " < firstValue" + indices + ".length && "
                                                                        + c + " < secondValue" + indices + ".length; " + c + "++) {\n";
                            indent += "    ";
                            indices += "[" + c + "]";
                            args += separator + c;
                            separator = ", ";
                        }
                        decl.text += indent + "first(" + args + separator + "firstValue" + indices + ");\n"
                                  +  indent + "second(" + args + separator + "secondValue" + indices + ");\n";
                        for (int i = 0; i < dim; i++) {
                            indent = indent.substring(4);
                            decl.text += indent + "}\n";
                        }
                        decl.text += "        return this;\n"
                                  +  "    }\n";
                    }
                } else if (resizable && firstType == null && secondType == null) {
                    boolean first = true;
                    for (String javaName : valueType.javaNames != null ? valueType.javaNames : new String[] {valueType.javaName}) {
                        decl.text += "\n";
                        if (dim < 2) {
                            if (first) {
                                decl.text += "    public " + javaName + " pop_back() {\n"
                                          +  "        long size = size();\n"
                                          +  "        " + javaName + " value = get(size - 1);\n"
                                          +  "        resize(size - 1);\n"
                                          +  "        return value;\n"
                                          +  "    }\n";
                            }
                            decl.text += "    public " + containerType.javaName + " push_back(" + javaName + " value) {\n"
                                      +  "        long size = size();\n"
                                      +  "        resize(size + 1);\n"
                                      +  "        return put(size, value);\n"
                                      +  "    }\n"
                                      +  "    public " + containerType.javaName + " put(" + javaName + " value) {\n"
                                      +  "        if (size() != 1) { resize(1); }\n"
                                      +  "        return put(0, value);\n"
                                      +  "    }\n";
                        }
                        decl.text += "    public " + containerType.javaName + " put(" + javaName + arrayBrackets + " ... array) {\n";
                        String indent = "        ", indices = "", args = "";
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
                        first = false;
                    }
                }
                decl.text += "}\n";
                declList.add(decl);
            }
        }
    }

    TemplateMap template(Context context) throws ParserException {
        if (!tokens.get().match(Token.TEMPLATE)) {
            return null;
        }
        TemplateMap map = new TemplateMap(context.templateMap);

        tokens.next().expect('<');
        for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(Token.IDENTIFIER)) {
                Token t = tokens.next();
                if (t.match("...")) {
                    map.variadic = true;
                    t = tokens.next();
                }
                if (t.match(Token.IDENTIFIER)) {
                    String key = t.value;
                    map.put(key, map.get(key));
                    token = tokens.next();
                }
            }
            if (!token.match(',', '>')) {
                // ignore default argument
                int count = 0;
                for (token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                    if (count == 0 && token.match(',', '>')) {
                        break;
                    } else if (token.match('<', '(')) {
                        count++;
                    } else if (token.match('>', ')')) {
                        count--;
                    }
                }
            }
            if (token.expect(',', '>').match('>')) {
                if (tokens.next().match(Token.TEMPLATE)) {
                    tokens.next().expect('<');
                } else {
                    break;
                }
            }
        }
        return map;
    }

    Type[] templateArguments(Context context) throws ParserException {
        if (!tokens.get().match('<')) {
            return null;
        }
        List<Type> arguments = new ArrayList<Type>();
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
                    } else if (token.match('<', '(')) {
                        count++;
                    } else if (token.match('>', ')')) {
                        count--;
                    }
                    type.cppName += token;
                    if (token.match(Token.CONST, Token.__CONST)) {
                        type.cppName += " ";
                    }
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

    Type type(Context context) throws ParserException {
        return type(context, false);
    }

    Type type(Context context, boolean definition) throws ParserException {
        Type type = new Type();
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
            if (token.match("::")) {
                Info info = infoMap.getFirst(type.cppName, false);
                if (info != null && info.pointerTypes != null && info.pointerTypes.length > 0
                        && !type.cppName.contains("::") && token.spacing.length() > 0) {
                    break;
                }
                type.cppName += token;
            } else if (token.match(Token.DECLTYPE)) {
                type.cppName += token.toString() + tokens.next().expect('(');
                int count = 0;
                for (token = tokens.next(); !token.match(')', Token.EOF); token = tokens.next()) {
                    type.cppName += token;
                }
                type.cppName += token;
                tokens.next();
                break;
            } else if (token.match('<')) {
                type.arguments = templateArguments(context);
                type.cppName += "<";
                String separator = "";
                for (Type t : type.arguments) {
                    if (t == null) {
                        // skip over variadic templates
                        continue;
                    }
                    type.cppName += separator;
                    Info info = infoMap.getFirst(t.cppName);
                    String s = info != null && info.cppTypes != null ? info.cppTypes[0] : t.cppName;
                    if (t.constValue && !s.startsWith("const ")) {
                        s = "const " + s;
                    }
                    if (t.constPointer && !s.endsWith(" const")) {
                        s = s + " const";
                    }
                    for (int i = 0; i < t.indirections; i++) {
                        s += "*";
                    }
                    if (t.reference) {
                        s += "&";
                    }
                    type.cppName += s;
                    separator = ",";
                }
                type.cppName += type.cppName.endsWith(">") ? " >" : ">";
            } else if (token.match(Token.CONST, Token.__CONST, Token.CONSTEXPR)) {
                int template = type.cppName.lastIndexOf('<');
                String simpleName = template >= 0 ? type.cppName.substring(0, template) : type.cppName;
                if (!simpleName.trim().contains(" ") || type.simple) {
                    type.constValue = true;
                } else {
                    type.constPointer = true;
                }
            } else if (token.match('*')) {
                type.indirections++;
                tokens.next();
                break;
            } else if (token.match('&')) {
                type.reference = true;
                tokens.next();
                break;
            } else if (token.match("&&")) {
                // rvalue reference... ignore?
            } else if (token.match('~')) {
                type.cppName += "~";
                type.destructor = true;
            } else if (token.match(Token.STATIC)) {
                type.staticMember = true;
            } else if (token.match(Token.OPERATOR)) {
                if (type.cppName.length() == 0) {
                    type.operator = true;
                    tokens.next();
                    continue;
                } else if (type.cppName.endsWith("::")) {
                    type.operator = true;
                    tokens.next();
                    break;
                } else {
                    break;
                }
            } else if (token.match(Token.USING)) {
                type.using = true;
            } else if (token.match(Token.FRIEND)) {
                type.friend = true;
            } else if (token.match(Token.TYPEDEF)) {
                type.typedef = true;
            } else if (token.match(Token.VIRTUAL)) {
                type.virtual = true;
            } else if (token.match(Token.AUTO, Token.ENUM, Token.EXPLICIT, Token.EXTERN, Token.INLINE, Token.CLASS, Token.FINAL,
                                   Token.INTERFACE, Token.__INTERFACE, Token.MUTABLE, Token.NAMESPACE, Token.STRUCT, Token.UNION,
                                   Token.TYPENAME, Token.REGISTER, Token.THREAD_LOCAL, Token.VOLATILE)) {
                token = tokens.next();
                continue;
            } else if (token.match((Object[])infoMap.getFirst("basic/types").cppTypes) && (type.cppName.length() == 0 || type.simple)) {
                type.cppName += token.value + " ";
                type.simple = true;
            } else if (token.match(Token.IDENTIFIER)) {
                int backIndex = tokens.index;
                Attribute attr = attribute();
                if (attr != null && attr.annotation) {
                    type.annotations += attr.javaName;
                    attributes.add(attr);
                    continue;
                } else {
                    tokens.index = backIndex;
                    if (type.cppName.length() == 0 || type.cppName.endsWith("::") || type.cppName.endsWith("~")) {
                        type.cppName += token.value;
                    } else {
                        Info info = infoMap.getFirst(tokens.get(1).value);
                        if ((info != null && info.annotations != null) ||
                                !tokens.get(1).match('*', '&', Token.IDENTIFIER, Token.CONST, Token.__CONST, Token.CONSTEXPR, Token.FINAL)) {
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
            tokens.next();
        }
        if (attributes.size() > 0) {
            type.attributes = attributes.toArray(new Attribute[attributes.size()]);
        }
        type.cppName = type.cppName.trim();
        if (tokens.get().match("...")) {
            // skip over variable arguments
            tokens.next();
            if (tokens.get().match(Token.IDENTIFIER)) {
                // skip over template parameter packs as well
                tokens.next();
            }
            return null;
        } else if (type.operator) {
            for (Token token = tokens.get(); !token.match(Token.EOF, '(', ';'); token = tokens.next()) {
                type.cppName += token;
            }
        }

        // remove * and & to query template map
        if (type.cppName.endsWith("*")) {
            type.indirections++;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (type.cppName.endsWith("&")) {
            type.reference = true;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }

        // perform template substitution
        if (context.templateMap != null) {
            String[] types = type.cppName.split("::");
            String separator = "";
            type.cppName = "";
            List<Type> arguments = new ArrayList<Type>();
            for (String t : types) {
                Type t2 = context.templateMap.get(t);
                type.cppName += separator + (t2 != null ? t2.cppName : t);
                if (t2 != null && t2.arguments != null) {
                    arguments.addAll(Arrays.asList(t2.arguments));
                }
                separator = "::";
            }
            if (arguments.size() > 0) {
                type.arguments = arguments.toArray(new Type[arguments.size()]);
            }
        }

        // remove const, * and & after template substitution for consistency
        if (type.cppName.startsWith("const ")) {
            type.constValue = true;
            type.cppName = type.cppName.substring(6);
        }
        if (type.cppName.endsWith("*")) {
            type.indirections++;
            if (type.reference) {
                type.constValue = false;
            }
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (type.cppName.endsWith("&")) {
            type.reference = true;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (type.cppName.endsWith(" const")) {
            type.constPointer = true;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 6);
        }

        Info info = null;
        String[] names = context.qualify(type.cppName);
        if (definition && names.length > 0) {
            String constName = type.constValue || type.constPointer ? "const " + names[0] : names[0];
            info = infoMap.getFirst(constName, false);
            type.cppName = names[0];
        } else {
            // guess the fully qualified C++ type with what's available in the InfoMap
            for (String name : names) {
                String constName = type.constValue || type.constPointer ? "const " + name : name;
                if ((info = infoMap.getFirst(constName, false)) != null) {
                    type.cppName = name;
                    break;
                } else if (infoMap.getFirst(constName) != null) {
                    type.cppName = name;
                }
            }
        }

        if (info != null && info.cppTypes != null && info.cppTypes.length > 0) {
            // use user defined type
            type.cppName = info.cppTypes[0];
        }

        // remove const, * and & after user defined substitution for consistency
        if (type.cppName.startsWith("const ")) {
            type.constValue = true;
            type.cppName = type.cppName.substring(6);
        }
        if (type.cppName.endsWith("*")) {
            type.indirections++;
            if (type.reference) {
                type.constValue = false;
            }
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (type.cppName.endsWith("&")) {
            type.reference = true;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 1);
        }
        if (type.cppName.endsWith(" const")) {
            type.constPointer = true;
            type.cppName = type.cppName.substring(0, type.cppName.length() - 6);
        }

        // produce some appropriate name for the peer Java class, relying on Info if available
        int namespace = type.cppName.lastIndexOf("::");
        int template = type.cppName.lastIndexOf('<');
        type.javaName = namespace >= 0 && template < 0 ? type.cppName.substring(namespace + 2) : type.cppName;
        if (info != null) {
            if (type.indirections == 0 && !type.reference && info.valueTypes != null && info.valueTypes.length > 0) {
                type.javaName = info.valueTypes[0];
                type.javaNames = info.valueTypes;
                type.value = true;
            } else if (info.pointerTypes != null && info.pointerTypes.length > 0) {
                type.javaName = info.pointerTypes[0];
                type.javaNames = info.pointerTypes;
            }
        }

        if (type.operator) {
            if (type.constValue) {
                type.annotations += "@Const ";
            }
            if (type.indirections == 0 && !type.reference && !type.value) {
                type.annotations += "@ByVal ";
            } else if (type.indirections == 0 && type.reference && !type.value) {
                type.annotations += "@ByRef ";
            }
            if (info != null && info.cast) {
                type.annotations += "@Cast(\"" + type.cppName + (type.indirections == 0 && !type.value ? "*" : "") + "\") ";
            }
            type.annotations += "@Name(\"operator " + (type.constValue ? "const " : "")
                    + type.cppName + (type.indirections > 0 ? "*" : type.reference ? "&" : "") + "\") ";
        }
        if (info != null && info.annotations != null) {
            for (String s : info.annotations) {
                type.annotations += s + " ";
            }
        }
        if (context.cppName != null && type.javaName.length() > 0) {
            String cppName = type.cppName;
            String groupName = context.cppName;
            int template2 = groupName != null ? groupName.lastIndexOf('<') : -1;
            if (template < 0 && template2 >= 0) {
                groupName = groupName.substring(0, template2);
            } else if (template >= 0 && template2 < 0) {
                cppName = cppName.substring(0, template);
            }
            int namespace2 = groupName != null ? groupName.lastIndexOf("::") : -1;
            if (namespace < 0 && namespace2 >= 0) {
                groupName = groupName.substring(namespace2 + 2);
            }
            if (cppName.equals(groupName)) {
                type.constructor = !type.destructor && !type.operator
                        && type.indirections == 0 && !type.reference && tokens.get().match('(', ':');
            }
            type.javaName = context.shorten(type.javaName);
        }
        return type;
    }

    Declarator declarator(Context context, String defaultName, int infoNumber, boolean useDefaults,
            int varNumber, boolean arrayAsPointer, boolean pointerAsArray) throws ParserException {
        boolean typedef = tokens.get().match(Token.TYPEDEF);
        boolean using = tokens.get().match(Token.USING);
        Declarator dcl = new Declarator();
        Type type = type(context);
        if (type == null) {
            return null;
        }
        typedef |= type.typedef;

        // pick the requested identifier out of the statement in the case of multiple variable declaractions
        int count = 0, number = 0;
        for (Token token = tokens.get(); number < varNumber && !token.match(Token.EOF); token = tokens.next()) {
            if (token.match('(','[','{')) {
                count++;
            } else if (token.match(')',']','}')) {
                count--;
            } else if (count > 0) {
                continue;
            } else if (token.match(',')) {
                number++;
            } else if (token.match(';')) {
                tokens.next();
                return null;
            }
        }

        // start building an appropriate cast for the C++ type
        String precast = null, cast = type.cppName;
        if (type.constPointer) {
            dcl.constPointer = true;
            cast += " const";
        }
        if (varNumber == 0 && type.indirections > 0) {
            dcl.indirections += type.indirections;
            for (int i = 0; i < type.indirections; i++) {
                cast += "*";
            }
        }
        if (varNumber == 0 && type.reference) {
            dcl.reference = true;
            cast += "&";
        }
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match('*')) {
                dcl.indirections++;
            } else if (token.match('&')) {
                dcl.reference = true;
            } else if (token.match("&&")) {
                // rvalue reference... ignore?
            } else if (token.match(Token.CONST, Token.__CONST, Token.CONSTEXPR)) {
                dcl.constPointer = true;
            } else {
                break;
            }
            cast += token;
        }

        // translate C++ attributes to equivalent Java annotations
        List<Attribute> attributes = new ArrayList<Attribute>();
        if (type.attributes != null) {
            attributes.addAll(Arrays.asList(type.attributes));
        }
        int backIndex = tokens.index;
        Attribute attr = attribute();
        while (attr != null && attr.annotation) {
            type.annotations += attr.javaName;
            attributes.add(attr);
            backIndex = tokens.index;
            attr = attribute();
        }

        // consider attributes of the form SOMETHING(name) as hints for an appropriate Java name
        attr = null;
        tokens.index = backIndex;
        for (Attribute a : attributes) {
            if (a.javaName != null && a.javaName.contains("@Name ")
                    && a.arguments.length() > 0 && Character.isJavaIdentifierStart(a.arguments.charAt(0))) {
                attr = a;
                for (char c : a.arguments.toCharArray()) {
                    if (!Character.isJavaIdentifierPart(c)) {
                        attr = null;
                        break;
                    }
                }
            }
            if (attr != null) {
                type.annotations = type.annotations.replace("@Name ", ""); // gets added back below
                break;
            }
        }

        // ignore superfluous parentheses
        count = 0;
        while (tokens.get().match('(') && tokens.get(1).match('(')) {
            tokens.next();
            count++;
        }

        int[] dims = new int[256];
        int indirections2 = 0;
        dcl.cppName = "";
        Info groupInfo = null;
        Declaration definition = new Declaration();
        boolean fieldPointer = false;
        Attribute convention = null;
        for (Attribute a : attributes) {
            if (a.annotation && a.javaName.length() == 0 && a.arguments.length() == 0) {
                // we may have a calling convention for function pointers
                convention = a;
            }
        }
        if (tokens.get().match('(') || (typedef && tokens.get(1).match('('))) {
            // probably a function pointer declaration
            if (tokens.get().match('(')) {
                tokens.next();
            }
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
                if (token.match(Token.CONST, Token.__CONST, Token.CONSTEXPR)) {
                    dcl.constPointer = true;
                } else if (token.match(Token.IDENTIFIER, "::")) {
                    int backIndex2 = tokens.index;
                    Attribute attr2 = attribute();
                    if (attr2 != null && attr2.annotation) {
                        type.annotations += attr2.javaName;
                        attributes.add(attr2);
                        convention = attr2;
                        continue;
                    } else {
                        tokens.index = backIndex2;
                        dcl.cppName += token;
                    }
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
                    } else if (convention != null || dcl.cppName.length() > 0) {
                        definition.text += "@Convention(\"" + (convention != null ? convention.cppName : dcl.cppName) + "\") ";
                        convention = null;
                    }
                    dcl.cppName = "";
                } else if (token.match('[')) {
                    Token n = tokens.get(1);
                    try {
                        dims[dcl.indices++] = n.match(Token.INTEGER) ? Integer.decode(n.value) : -1;
                    } catch (NumberFormatException e) {
                        dims[dcl.indices++] = -1;
                    }
                } else if (token.match('(', ')')) {
                    break;
                }
                tokens.next();
            }
            if (tokens.get().match(')')) {
                tokens.next();
            }
        } else if (tokens.get().match(Token.IDENTIFIER, "::")) {
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                if (dcl.cppName.length() > 0 && token.match('*')) {
                    // a data member pointer or something
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

                    for (token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
                        if (token.match('*')) {
                            indirections2++;
                        } else {
                            break;
                        }
                    }
                    dcl.cppName = token.match(Token.IDENTIFIER) ? token.toString() : "";
                    fieldPointer = groupInfo != null;
                } else if (token.match("::")) {
                    dcl.cppName += token;
                } else if (token.match(Token.OPERATOR)) {
                    dcl.operator = true;
                    if (!tokens.get(1).match(Token.IDENTIFIER) || tokens.get(1).match(Token.NEW, Token.DELETE)) {
                        // assume we can have any symbols until the first open parenthesis
                        dcl.cppName += "operator " + tokens.next();
                        for (token = tokens.next(); !token.match(Token.EOF, '('); token = tokens.next()) {
                            dcl.cppName += token;
                        }
                        break;
                    }
                } else if (token.match('<')) {
                    // template arguments
                    dcl.cppName += token;
                    int count2 = 0;
                    for (token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                        dcl.cppName += token;
                        if (count2 == 0 && token.match('>')) {
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
                try {
                    dims[dcl.indices++] = n.match(Token.INTEGER) ? Integer.decode(n.value) : -1;
                } catch (NumberFormatException e) {
                    dims[dcl.indices++] = -1;
                }
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
            dcl.indirections++;
            String dimCast = "";
            for (int i = 1; i < dcl.indices; i++) {
                if (dims[i] > 0) {
                    dimCast += "[" + dims[i] + "]";
                }
            }

            if (!dimCast.isEmpty()) {
                if (dims[0] != -1) {
                    // Annotate with the first dimension's value
                    cast += "(* /*[" + dims[0] + "]*/ )";
                } else {
                    // Unknown size
                    cast += "(*)";
                }
                cast += dimCast;
            } else {
                cast += "*";
            }
        }
        if (pointerAsArray && dcl.indirections > (type.anonymous ? 0 : 1)) {
            // treat second indirection as an array, unless anonymous
            dims[dcl.indices++] = -1;
            dcl.indirections--;
            cast = cast.substring(0, cast.length() - 1);
        }

        if (tokens.get().match(':')) {
            // ignore bitfields
            type.annotations += "@NoOffset ";
            tokens.next().expect(Token.INTEGER, Token.IDENTIFIER);
            tokens.next().expect(',', ';');
            if (dcl.cppName == null) {
                dcl.cppName = "";
            }
        }

        // if this is a function pointer, get parameters
        dcl.parameters = parameters(context, infoNumber, useDefaults);
        if (type.cppName.equals("void") && indirections2 == 1 && !typedef && tokens.get(1).match('(')) {
            // weird non-ANSI function declaration
            tokens.next().expect('(');
            tokens.next().expect(Token.IDENTIFIER);
            type(context); // ignore?
            indirections2 = 0;
        } else if (indirections2 == 1 && !typedef && tokens.get(1).match('[')) {
            // weird template function returning an array
            tokens.next().expect('[');
            tokens.next().expect(Token.IDENTIFIER);
            tokens.next().expect(']');
            dcl.indirections++;
            indirections2--;
        }

        int infoLength = 1;
        boolean valueType = false, needCast = arrayAsPointer && dcl.indices > 1, implicitConst = false;
        Info constInfo = infoMap.getFirst("const " + type.cppName, false);
        Info info = type.constValue && dcl.indirections < 2 && !dcl.reference ? constInfo
                  : infoMap.getFirst(type.cppName, false);
        if ((!typedef || dcl.parameters != null)
                && (constInfo == null || (constInfo.cppTypes != null && constInfo.cppTypes.length > 0))
                && (info == null || (info.cppTypes != null && info.cppTypes.length > 0))) {
            // substitute template types that have no info with appropriate adapter annotation
            Type type2 = type;
            if (info != null) {
                type2 = new Parser(this, info.cppTypes[0]).type(context);
            }
            List<Info> infoList = infoMap.get(type2.cppName);
            for (Info info2 : infoList) {
                if (type2.arguments != null && info2.annotations != null) {
                    type.constPointer = type2.arguments[0].constPointer;
                    type.constValue = type2.arguments[0].constValue;
                    type.simple = type2.arguments[0].simple;
                    type.indirections = type2.arguments[0].indirections;
                    type.reference = type2.arguments[0].reference;
                    type.annotations = type2.arguments[0].annotations;
                    type.cppName = type2.arguments[0].cppName;
                    type.javaName = type2.arguments[0].javaName;
                    dcl.indirections = 1;
                    dcl.reference = false;
                    if (context.virtualize) {
                        // force cast in callbacks
                        needCast = true;
                        precast = cast;
                    }
                    cast = type.cppName + "*";
                    if (type.constValue && !cast.startsWith("const ")) {
                        cast = "const " + cast;
                    }
                    if (type.constPointer && !cast.endsWith(" const")) {
                        cast = cast + " const";
                    }
                    if (type.indirections > 0) {
                        dcl.indirections += type.indirections;
                        for (int i = 0; i < type.indirections; i++) {
                            cast += "*";
                        }
                    }
                    if (type.reference) {
                        dcl.reference = true;
                        cast += "&";
                    }
                    for (String s : info2.annotations) {
                        type.annotations += s + " ";
                    }
                    info = infoMap.getFirst(type.cppName, false);
                    break;
                }
            }
        }
        if (!using && info != null) {
            valueType = (info.enumerate || info.valueTypes != null)
                    && ((type.constValue && dcl.indirections == 0 && dcl.reference)
                        || (dcl.indirections == 0 && !dcl.reference)
                        || info.pointerTypes == null);
            implicitConst = info.cppNames[0].startsWith("const ") && !info.define;
            infoLength = valueType ? (info.valueTypes != null ? info.valueTypes.length : 1)
                                   : (info.pointerTypes != null ? info.pointerTypes.length : 1);
            dcl.infoNumber = infoNumber < 0 ? 0 : infoNumber % infoLength;
            type.javaName = valueType ? (info.valueTypes != null ? info.valueTypes[dcl.infoNumber] : type.javaName)
                                      : (info.pointerTypes != null ? info.pointerTypes[dcl.infoNumber] : type.javaName);
            type.javaName = context.shorten(type.javaName);
            needCast |= info.cast && !type.cppName.equals(type.javaName);
        }

        if (!valueType || context.virtualize) {
            if (!valueType && dcl.indirections == 0 && !dcl.reference) {
                type.annotations += "@ByVal ";
            } else if (dcl.indirections == 0 && dcl.reference) {
                if (type.javaName.contains("@ByPtrPtr ")) {
                    type.javaName = type.javaName.replace("@ByPtrPtr ", "@ByPtrRef ");
                } else {
                    type.annotations += "@ByRef ";
                }
            } else if (!type.javaName.contains("@ByPtrRef ") && dcl.indirections == 1 && dcl.reference) {
                type.annotations += "@ByPtrRef ";
            } else if (!type.javaName.contains("@ByPtrPtr ") && dcl.indirections == 2 && !dcl.reference
                    && (infoNumber >= 0 || type.javaName.equals("PointerPointer"))) {
                type.annotations += "@ByPtrPtr ";
                needCast |= type.cppName.equals("void");
            } else if (dcl.indirections >= 2) {
                dcl.infoNumber += infoLength;
                needCast = true;
                if (type.javaName.contains("@ByPtrRef ") || dcl.reference) {
                    type.annotations += "@ByRef ";
                } else if (type.javaName.contains("@ByPtrPtr ") || dcl.indirections >= 3) {
                    type.annotations += "@ByPtrPtr ";
                }
                type.javaName = "PointerPointer";
            }

            if (!needCast && !type.javaName.contains("@Cast")) {
                if (type.constValue && !implicitConst && !type.constPointer) {
                    type.annotations = "@Const " + type.annotations;
                } else if (type.constPointer) {
                    type.annotations = "@Const({" + type.constValue + ", " + type.constPointer + "}) " + type.annotations;
                }
            }
        }
        if (needCast) {
            if (dcl.indirections == 0 && dcl.reference) {
                // consider as pointer type
                cast = cast.replace('&', '*');
            }
            if (valueType && type.constValue && dcl.reference) {
                // consider as value type
                cast = cast.substring(0, cast.length() - 1);
            }
            if (type.constValue && !cast.startsWith("const ")) {
                cast = "const " + cast;
            }
            if (precast != null) {
                type.annotations = "@Cast({\"" + cast + "\", \"" + precast + "\"}) " + type.annotations;
            } else if (!valueType && dcl.indirections == 0 && !dcl.reference) {
                type.annotations += "@Cast(\"" + cast + "*\") ";
            } else {
                type.annotations = "@Cast(\"" + cast + "\") " + type.annotations;
            }
        }

        // initialize shorten Java name and get fully qualified C++ name
        info = null;
        dcl.javaName = attr != null ? attr.arguments : dcl.cppName;
        if (defaultName == null) {
            // get Info for fully qualified C++ names only, which function arguments cannot have
            for (String name : context.qualify(dcl.cppName)) {
                if ((info = infoMap.getFirst(name, false)) != null) {
                    dcl.cppName = name;
                    break;
                } else if (infoMap.getFirst(name) != null) {
                    dcl.cppName = name;
                }
            }
        }

        // pick the Java name from the InfoMap if appropriate
        String originalName = fieldPointer ? groupInfo.pointerTypes[0] : dcl.javaName;
        if (attr == null && defaultName == null && info != null && info.javaNames != null && info.javaNames.length > 0
                && (dcl.operator || !info.cppNames[0].contains("<") || (context.templateMap != null && context.templateMap.type == null))) {
            dcl.javaName = info.javaNames[0];
        }

        if (info != null && info.annotations != null) {
            for (String s : info.annotations) {
                type.annotations += s + " ";
            }
        }

        // deal with function parameters and function pointers
        dcl.type = type;
        dcl.signature = dcl.javaName;
        if (dcl.parameters != null || fieldPointer) {
            if (dcl.parameters != null) {
                dcl.infoNumber = Math.max(dcl.infoNumber, dcl.parameters.infoNumber);
            }
            if (dcl.parameters != null && indirections2 == 0 && !typedef) {
                dcl.signature += dcl.parameters.signature;
            } else {
                if (convention != null) {
                    definition.text += "@Convention(\"" + convention.cppName + "\") ";
                }
                String cppType = "";
                if (dcl.type != null) {
                    cppType += dcl.type.cppName;
                    for (int i = 0; i < dcl.indirections; i++) {
                        cppType += "*";
                    }
                    if (dcl.reference) {
                        cppType += "&";
                    }
                }
                cppType += " (*)(";
                String separator = "";
                if (dcl.parameters != null) {
                    for (Declarator d : dcl.parameters.declarators) {
                        if (d != null) {
                            cppType += separator + d.type.cppName;
                            for (int i = 0; i < d.indirections; i++) {
                                cppType += "*";
                            }
                            if (d.reference) {
                                cppType += "&";
                            }
                            separator = ", ";
                        }
                    }
                }
                info = infoMap.getFirst(cppType += ")");
                if (info == null) {
                    info = infoMap.getFirst(dcl.cppName);
                }

                String functionType = null;
                if (originalName != null) {
                    functionType = Character.toUpperCase(originalName.charAt(0)) + originalName.substring(1);
                }
                if (info != null && info.pointerTypes != null && info.pointerTypes.length > 0) {
                    functionType = info.pointerTypes[0];
                } else if (typedef) {
                    functionType = originalName;
                } else if (dcl.parameters != null && dcl.parameters.signature.length() > 0) {
                    functionType += dcl.parameters.signature;
                } else if (!type.javaName.equals("void")) {
                    String s = type.javaName.trim();
                    int n = s.lastIndexOf(' ');
                    if (n > 0) {
                        s = s.substring(n + 1);
                    }
                    functionType = Character.toUpperCase(s.charAt(0)) + s.substring(1) + "_" + functionType;
                }
                if (info != null && info.annotations != null) {
                    for (String s : info.annotations) {
                        definition.text += s + " ";
                    }
                }
                functionType = functionType.substring(functionType.lastIndexOf(' ') + 1); // get rid of pointer annotations
                if (!functionType.equals("Pointer")) {
                    definition.type = new Type(functionType);
                    definition.text += (tokens.get().match(Token.CONST, Token.__CONST, Token.CONSTEXPR) ? "@Const " : "") +
                            "public static class " + functionType + " extends FunctionPointer {\n" +
                            "    static { Loader.load(); }\n" +
                            "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n" +
                            "    public    " + functionType + "(Pointer p) { super(p); }\n" +
                        (groupInfo != null ? "" :
                            "    protected " + functionType + "() { allocate(); }\n" +
                            "    private native void allocate();\n");
                    if (fieldPointer) {
                        definition.text +=
                            "    public native " + type.annotations + type.javaName + " get(" + groupInfo.pointerTypes[0] + " o);\n" +
                            "    public native " + functionType + " put(" + groupInfo.pointerTypes[0] + " o, " + type.annotations + type.javaName + " v);\n" +
                            "}\n";
                    } else {
                        definition.text +=
                            "    public native " + type.annotations + type.javaName + " call" +
                        (groupInfo != null ? "(" + groupInfo.pointerTypes[0] + " o" + (dcl.parameters.list.charAt(1) == ')' ?
                                ")" : ", " + dcl.parameters.list.substring(1)) : dcl.parameters.list) + ";\n" +
                            "}\n";
                    }
                }
                definition.signature = functionType;
                definition.declarator = new Declarator();
                definition.declarator.parameters = dcl.parameters;
                if (info == null || !info.skip) {
                    dcl.definition = definition;
                }
                dcl.indirections = indirections2;
                if (pointerAsArray && dcl.indirections > 1) {
                    // treat second indirection as an array
                    dims[dcl.indices++] = -1;
                    dcl.indirections--;
                }
                if (!fieldPointer) {
                    dcl.parameters = null;
                }
                if (dcl.indirections > 1) {
                    int n = cppType.indexOf('(');
                    type.annotations = "@Cast(\"" + cppType.substring(0, n + 1) + "*" + cppType.substring(n + 1) + "\") ";
                    type.javaName = "PointerPointer";
                } else {
                    type.annotations = info != null && info.cast ? "@Cast(\"" + cppType + "\") " : dcl.constPointer ? "@Const " : "";
                    type.javaName = functionType;
                }
            }
        }

        // annotate with @Name if the Java name doesn't match with the C++ name
        if (dcl.cppName != null) {
            String localName = dcl.cppName;
            if (context.namespace != null && localName.startsWith(context.namespace + "::")) {
                localName = dcl.cppName.substring(context.namespace.length() + 2);
            }
            int template = localName.lastIndexOf('<');
            String simpleName = template >= 0 ? localName.substring(0, template) : localName;
            if (!simpleName.equals(dcl.javaName) && (!localName.contains("::") || context.javaName == null)) {
                type.annotations += "@Name(\"" + localName + "\") ";
            }
        }

        // ignore superfluous parentheses
        while (tokens.get().match(')') && count > 0) {
            tokens.next();
            count--;
        }

        return dcl;
    }

    /** Tries to adapt a Doxygen-style documentation comment to Javadoc-style. */
    String commentDoc(String s, int startIndex) {
        if (startIndex < 0 || startIndex > s.length()) {
            return s;
        }
        int index = s.indexOf("/**", startIndex);
        StringBuilder sb = new StringBuilder(s);
        while (index < sb.length()) {
            char c = sb.charAt(index);
            String ss = sb.substring(index + 1);
            if (c == '`' && ss.startsWith("``") && sb.length() - index > 3) {
                sb.replace(index, index + 3, "<pre>{@code"
                        + (Character.isWhitespace(sb.charAt(index + 3)) ? "" : " "));
                index = sb.indexOf("```", index);
                if (index < 0) {
                    break;
                }
                sb.replace(index, index + 3, "}</pre>");
            } else if (c == '`') {
                sb.replace(index, index + 1, "{@code ");
                index = sb.indexOf("`", index);
                if (index < 0) {
                    break;
                }
                sb.replace(index, index + 1, "}");
            } else if ((c == '\\' || c == '@') && ss.startsWith("code")) {
                sb.replace(index, index + 5, "<pre>{@code"
                        + (Character.isWhitespace(sb.charAt(index + 5)) ? "" : " "));
                index = sb.indexOf(c + "endcode", index);
                if (index < 0) {
                    break;
                }
                sb.replace(index, index + 8, "}</pre>");
            } else if ((c == '\\' || c == '@') && ss.startsWith("verbatim")) {
                sb.replace(index, index + 9, "<pre>{@literal"
                        + (Character.isWhitespace(sb.charAt(index + 9)) ? "" : " "));
                index = sb.indexOf(c + "endverbatim", index);
                if (index < 0) {
                    break;
                }
                sb.replace(index, index + 12, "}</pre>");
            } else if (c == '\n' && ss.length() > 0 && ss.charAt(0) == '\n') {
                int n = 0;
                while (n < ss.length() && ss.charAt(n) == '\n') {
                    n++;
                }
                String indent = "";
                while (n < ss.length() && Character.isWhitespace(ss.charAt(n))) {
                    indent += ss.charAt(n);
                    n++;
                }
                sb.insert(index + 1, indent + "<p>");
            } else if (c == '\\' || c == '@') {
                boolean tagFound = false;
                for (DocTag tag : DocTag.docTags) {
                    Matcher matcher = tag.pattern.matcher(ss);
                    if (matcher.lookingAt()) {
                        StringBuffer sbuf = new StringBuffer();
                        matcher.appendReplacement(sbuf, tag.replacement);
                        sb.replace(index + 1+ matcher.start(),
                              index + 1 + matcher.end(), sbuf.toString());
                        sb.setCharAt(index, '@');
                        tagFound = true;
                        break;
                    }
                }
                if (!tagFound) {
                    // keep unmapped tags around as part of the comments
                    sb.setCharAt(index, '\\');
                }
            } else if (c == '*' && ss.charAt(0) == '/') {
                index = sb.indexOf("/**", index);
                if (index < 0) {
                    break;
                }
            }
            index++;
        }
        return sb.toString();
    }

    /** Converts Doxygen-like documentation comments placed before identifiers to Javadoc-style.
     *  Also leaves as is non-documentation comments. */
    String commentBefore() throws ParserException {
        String comment = "";
        tokens.raw = true;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        boolean closeComment = false;
        int startDoc = -1;
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            String s = token.value;
            if (s.startsWith("/**") || s.startsWith("/*!") || s.startsWith("///") || s.startsWith("//!")) {
                if (s.startsWith("//") && s.contains("*/") && s.indexOf("*/") < s.length() - 2) {
                    s = s.replace("*/", "* /");
                }
                if (s.length() > 3 && s.charAt(3) == '<') {
                    continue;
                } else if (s.length() >= 3 && (s.startsWith("///") || s.startsWith("//!"))
                        && !s.startsWith("////") && !s.startsWith("///*")) {
                    String lastComment = comment.trim();
                    int n2 = lastComment.indexOf('\n');
                    while (!lastComment.startsWith("/*") && n2 > 0) {
                        lastComment = n2 + 1 < lastComment.length() ? lastComment.substring(n2 + 1).trim() : "";
                        n2 = lastComment.indexOf('\n');
                    }
                    s = (comment.length() == 0 || comment.contains("*/")
                            || !lastComment.startsWith("/*") ? "/**" : " * ") + s.substring(3);
                    closeComment = true;
                } else if (s.length() > 3 && !s.startsWith("///")) {
                    s = "/**" + s.substring(3);
                }
            } else if (closeComment && !comment.endsWith("*/")) {
                closeComment = false;
                comment += " */";
            }
            if (startDoc < 0 && s.startsWith("/**")) {
                startDoc = comment.length();
            }
            comment += token.spacing + s;
        }
        if (closeComment && !comment.endsWith("*/")) {
            comment += " */";
        }
        tokens.raw = false;
        return commentDoc(comment, startDoc);
    }

    /** Converts Doxygen-like documentation comments placed after identifiers to Javadoc-style. */
    String commentAfter() throws ParserException {
        String comment = "";
        tokens.raw = true;
        while (tokens.index > 0 && tokens.get(-1).match(Token.COMMENT)) {
            tokens.index--;
        }
        boolean closeComment = false;
        int startDoc = -1;
        for (Token token = tokens.get(); token.match(Token.COMMENT); token = tokens.next()) {
            String s = token.value;
            String spacing = token.spacing;
            int n = spacing.lastIndexOf('\n') + 1;
            if (s.startsWith("/**") || s.startsWith("/*!") || s.startsWith("///") || s.startsWith("//!")) {
                if (s.length() > 3 && s.charAt(3) != '<') {
                    continue;
                } else if (s.length() > 4 && (s.startsWith("///") || s.startsWith("//!"))) {
                    String lastComment = comment.trim();
                    int n2 = lastComment.indexOf('\n');
                    while (!lastComment.startsWith("/*") && n2 > 0) {
                        lastComment = n2 + 1 < lastComment.length() ? lastComment.substring(n2 + 1).trim() : "";
                        n2 = lastComment.indexOf('\n');
                    }
                    s = (comment.length() == 0 || comment.contains("*/")
                            || !lastComment.startsWith("/*") ? "/**" : " * ") + s.substring(4);
                    closeComment = true;
                } else if (s.length() > 4) {
                    s = "/**" + s.substring(4);
                }
                if (startDoc < 0 && s.startsWith("/**")) {
                    startDoc = comment.length();
                }
                comment += spacing.substring(0, n) + s;
            }
        }
        if (closeComment && !comment.endsWith("*/")) {
            comment += " */";
        }
        if (comment.length() > 0) {
            comment += "\n";
        }
        tokens.raw = false;
        return commentDoc(comment, startDoc);
    }

    Attribute attribute() throws ParserException {
        return attribute(false);
    }
    Attribute attribute(boolean explicit) throws ParserException {
        // attributes might have arguments that start with '(', but not '<'
        if (!tokens.get().match(Token.IDENTIFIER) || tokens.get(1).match('<')) {
            return null;
        }
        Attribute attr = new Attribute();
        Info info = infoMap.getFirst(attr.cppName = tokens.get().value);
        if (attr.annotation = info != null && info.annotations != null
                && info.javaNames == null && info.valueTypes == null && info.pointerTypes == null) {
            for (String s : info.annotations) {
                attr.javaName += s + " ";
            }
        }
        if (explicit && !attr.annotation) {
            return null;
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
            } else if (info == null || !info.skip) {
                attr.arguments += token.value;
            }
        }
        tokens.raw = false;
        return attr;
    }

    String body() throws ParserException {
        String text = "";
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
            if (count > 0) {
                text += token.spacing + token;
            }
        }
        tokens.raw = false;
        return text;
    }

    Parameters parameters(Context context, int infoNumber, boolean useDefaults) throws ParserException {
        int backIndex = tokens.index;
        if (!tokens.get().match('(')) {
            return null;
        }

        int count = 0;
        Parameters params = new Parameters();
        List<Declarator> dcls = new ArrayList<Declarator>();
        params.list = "(";
        params.names = "(";
        int lastVarargs = -1;
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
            Token defaultToken = null;
            String defaultValue = "";
            if (dcl != null && hasDefault) {
                defaultToken = tokens.get();
                int count2 = 0;
                for (token = tokens.next(), token.spacing = ""; !token.match(Token.EOF); token = tokens.next()) {
                    if (count2 == 0 && token.match(',', ')', '}')) {
                        break;
                    } else if (token.match('(', '{')) {
                        count2++;
                    } else if (token.match(')', '}')) {
                        count2--;
                    }

                    // try to qualify all the identifiers
                    String cppName = token.value;
                    for (String name : context.qualify(cppName)) {
                        if (infoMap.getFirst(name, false) != null) {
                            cppName = name;
                            break;
                        } else if (infoMap.getFirst(name) != null) {
                            cppName = name;
                        }
                    }
                    if (token.match(Token.IDENTIFIER)) {
                        while (tokens.get(1).equals("::")) {
                            tokens.next();
                            Token t = tokens.next();
                            cppName += "::" + t.spacing + t;
                        }
                    }
                    defaultValue += token.spacing + (cppName != null && cppName.length() > 0 ? cppName : token);
                }
                for (String name : context.qualify(defaultValue)) {
                    if (infoMap.getFirst(name, false) != null) {
                        defaultValue = name;
                        break;
                    } else if (infoMap.getFirst(name) != null) {
                        defaultValue = name;
                    }
                }

                // insert default value as nullValue for pass by value or by reference
                String s = dcl.type.annotations;
                int n = s.indexOf("@ByVal ");
                if (n < 0) {
                    n = s.indexOf("@ByRef ");
                }
                if (n >= 0) {
                    if (!defaultValue.startsWith(dcl.type.cppName)) {
                        defaultValue = dcl.type.cppName + "(" + defaultValue + ")";
                    }
                    Info info = infoMap.getFirst(defaultValue);
                    if (info != null && info.skip) {
                        if (useDefaults) {
                            tokens.index = backIndex;
                            return parameters(context, infoNumber, false);
                        }
                    } else {
                        defaultValue = defaultValue.replaceAll("\"", "\\\\\"").replaceAll("\n(\\s*)", "\"\n$1 + \"");
                        s = s.substring(0, n + 6) + "(nullValue = \"" + defaultValue + "\")" + s.substring(n + 6);
                    }
                }
                dcl.type.annotations = s;
            }
            if (dcl != null && !dcl.type.javaName.equals("void") && (!hasDefault || !useDefaults)) {
                if (lastVarargs >= 0) {
                    // substitute varargs that are not last with array
                    params.list = params.list.substring(0, lastVarargs) + "[]" + params.list.substring(lastVarargs + 3);
                }
                int n = params.list.length();
                params.infoNumber = Math.max(params.infoNumber, dcl.infoNumber);
                params.list += (count > 1 ? "," : "") + spacing + dcl.type.annotations + dcl.type.javaName + " " + dcl.javaName;
                lastVarargs = params.list.indexOf("...", n);
                if (hasDefault && !dcl.type.annotations.contains("(nullValue = ")) {
                    // output default argument as a comment
                    params.list += "/*" + defaultToken + defaultValue + "*/";
                }
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
            if (tokens.get().expect(',', ')').match(',')) {
                tokens.next();
            }
        }
        if (context.templateMap == null && dcls.size() == 1 && (dcls.get(0) == null || dcls.get(0).type == null
                || dcls.get(0).type.cppName == null || dcls.get(0).type.cppName.length() == 0)) {
            // this looks more like a variable initialization
            tokens.index = backIndex;
            return null;
        }
        params.declarators = dcls.toArray(new Declarator[dcls.size()]);
        return params;
    }

    static String incorporateConstAnnotation(String annotations, int constValueIndex, boolean constValue) {
        int start = annotations.indexOf("@Const");
        int end = annotations.indexOf("@", start + 1);
        if (end == -1) {
            end = annotations.length();
        }
        String prefix = annotations.substring(0, start);
        String constAnnotation = annotations.substring(start, end);
        String suffix = " " + annotations.substring(end, annotations.length());

        String boolPatternStr = "(true|false)";
        Pattern boolPattern = Pattern.compile(boolPatternStr);
        Matcher matcher = boolPattern.matcher(constAnnotation);

        /** default value same with {@link org.bytedeco.javacpp.annotation.Const} **/
        boolean constArray[] = {true, false, false};
        int index = 0;
        while (matcher.find()) {
            constArray[index++] = Boolean.parseBoolean(matcher.group(1));
        }
        constArray[constValueIndex] = constValue;

        String incorporatedConstAnnotation = "@Const({" + constArray[0] + ", " + constArray[1] + ", " + constArray[2] + "})";
        return prefix + incorporatedConstAnnotation + suffix;
    }

    boolean function(Context context, DeclarationList declList) throws ParserException {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public native ";

        int startIndex = tokens.index;
        Type type = type(context);
        Parameters params = parameters(context, 0, false);
        Declarator dcl = new Declarator();
        Declaration decl = new Declaration();
        if (type.javaName.length() == 0) {
            // not a function, probably an attribute
            tokens.index = backIndex;
            return false;
        } else if (context.javaName == null && !type.operator && params != null) {
            // this is a constructor/destructor definition or specialization, skip over
            while (!tokens.get().match(':', '{', ';', Token.EOF)) {
                tokens.next();
            }
            if (tokens.get().match(':')) {
                int count = 0;
                for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                    if (token.match('(')) {
                        count++;
                    } else if (token.match(')')) {
                        count--;
                    }

                    // consider { ... } preceded by an identifier as an initializer list
                    if (count == 0 && !token.match(Token.IDENTIFIER) && tokens.get(1).match('{')) {
                        tokens.next();
                        break;
                    }
                    if (token.match(';')) {
                        break;
                    }
                }
            }
            if (tokens.get().match('{')) {
                body();
            } else {
                while (!tokens.get().match(';', Token.EOF)) {
                    tokens.next();
                }
            }
            decl.text = spacing;
            decl.function = true;
            declList.add(decl);
            return true;
        } else if ((type.constructor || type.destructor || type.operator) && params != null) {
            // this is a constructor, destructor, or cast operator
            dcl.type = type;
            dcl.parameters = params;
            dcl.cppName = type.cppName;
            dcl.javaName = type.javaName.substring(type.javaName.lastIndexOf(' ') + 1);
            if (type.operator) {
                dcl.cppName = "operator " + (dcl.type.constValue ? "const " : "")
                        + dcl.type.cppName + (dcl.type.indirections > 0 ? "*" : dcl.type.reference ? "&" : "");
                dcl.javaName = "as" + Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1);
            }
            dcl.signature = dcl.javaName + params.signature;
        } else {
            tokens.index = startIndex;
            dcl = declarator(context, null, 0, false, 0, false, false);
            type = dcl.type;
        }
        if (dcl.cppName == null || type.javaName.length() == 0 || dcl.parameters == null) {
            tokens.index = backIndex;
            return false;
        }

        int namespace = dcl.cppName.lastIndexOf("::");
        if (context.namespace != null && namespace < 0) {
            dcl.cppName = context.namespace + "::" + dcl.cppName;
        }
        Info info = null, fullInfo = null;
        String fullname = dcl.cppName, fullname2 = dcl.cppName;
        if (dcl.parameters != null) {
            fullname += "(";
            fullname2 += "(";
            String separator = "";
            for (Declarator d : dcl.parameters.declarators) {
                if (d != null) {
                    String s = d.type.cppName;
                    String s2 = d.type.cppName;
                    if (d.type.constValue && !s.startsWith("const ")) {
                        s = "const " + s;
                    }
                    if (d.type.constPointer && !s.endsWith(" const")) {
                        s = s + " const";
                    }
                    if (d.indirections > 0) {
                        for (int i = 0; i < d.indirections; i++) {
                            s += "*";
                            s2 += "*";
                        }
                    }
                    if (d.reference) {
                        s += "&";
                        s2 += "&";
                    }
                    fullname += separator + s;
                    fullname2 += separator + s2;
                    separator = ", ";
                }
            }
            info = fullInfo = infoMap.getFirst(fullname += ")");
            if (info == null) {
                info = infoMap.getFirst(fullname2 += ")");
            }
        }
        if (info == null) {
            info = infoMap.getFirst(dcl.cppName);
            if (!type.constructor && !type.destructor && !type.operator) {
                infoMap.put(info != null ? new Info(info).cppNames(fullname) : new Info(fullname));
            }
        }
        String localName = dcl.cppName;
        if (localName.startsWith(context.namespace + "::")) {
            localName = dcl.cppName.substring(context.namespace.length() + 2);
        }
        int localNamespace = 0;
        int templateCount = 0;
        for (int i = 0; i < localName.length(); i++) {
            int c = localName.charAt(i);
            if (c == '<') {
                templateCount++;
            } else if (c == '>') {
                templateCount--;
            } else if (templateCount == 0 && localName.substring(i).startsWith("::")) {
                localNamespace = i;
                break;
            }
        }
        if (type.friend || (context.javaName == null && localNamespace > 0) || (info != null && info.skip)) {
            // this is a friend declaration, or a member function definition or specialization, skip over
            while (!tokens.get().match(':', '{', ';', Token.EOF)) {
                tokens.next();
            }
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
                if (attribute() == null) {
                    break;
                }
            }
            if (tokens.get().match(':')) {
                int count = 0;
                for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                    if (token.match('(')) {
                        count++;
                    } else if (token.match(')')) {
                        count--;
                    }

                    // consider { ... } preceded by an identifier as an initializer list
                    if (count == 0 && !token.match(Token.IDENTIFIER) && tokens.get(1).match('{')) {
                        tokens.next();
                        break;
                    }
                    if (token.match(';')) {
                        break;
                    }
                }
            }
            if (tokens.get().match('{')) {
                body();
            } else {
                while (!tokens.get().match(';', Token.EOF)) {
                    tokens.next();
                }
            }
            decl.text = spacing;
            decl.function = true;
            declList.add(decl);
            return true;
        } else if (type.staticMember || context.javaName == null) {
            modifiers = "public static native ";
            if (tokens.isCFile) {
                modifiers = "@NoException " + modifiers;
            }
        }

        List<Declarator> prevDcl = new ArrayList<Declarator>();
        boolean first = true;
        for (int n = -2; n < Integer.MAX_VALUE; n++) {
            decl = new Declaration();
            tokens.index = startIndex;
            boolean useDefaults = (info == null || !info.skipDefaults) && n % 2 != 0;
            if ((type.constructor || type.destructor || type.operator) && params != null) {
                type = type(context);
                params = parameters(context, n / 2, useDefaults);
                dcl = new Declarator();
                dcl.type = type;
                dcl.parameters = params;
                dcl.cppName = type.cppName;
                dcl.javaName = type.javaName.substring(type.javaName.lastIndexOf(' ') + 1);
                if (type.operator) {
                    dcl.cppName = "operator " + (dcl.type.constValue ? "const " : "")
                            + dcl.type.cppName + (dcl.type.indirections > 0 ? "*" : dcl.type.reference ? "&" : "");
                    dcl.javaName = "as" + Character.toUpperCase(dcl.javaName.charAt(0)) + dcl.javaName.substring(1);
                }
                dcl.signature = dcl.javaName + params.signature;
                if (tokens.get().match(':')) {
                    int count = 0;
                    for (Token token = tokens.next(); !token.match(Token.EOF); token = tokens.next()) {
                        if (token.match('(')) {
                            count++;
                        } else if (token.match(')')) {
                            count--;
                        }

                        // consider { ... } preceded by an identifier as an initializer list
                        if (count == 0 && !token.match(Token.IDENTIFIER) && tokens.get(1).match('{')) {
                            tokens.next();
                            break;
                        }
                        if (token.match(';')) {
                            break;
                        }
                    }
                }
            } else {
                dcl = declarator(context, null, n / 2, (info == null || !info.skipDefaults) && n % 2 != 0, 0, false, false);
                type = dcl.type;
                namespace = dcl.cppName.lastIndexOf("::");
                if (context.namespace != null && namespace < 0) {
                    dcl.cppName = context.namespace + "::" + dcl.cppName;
                }
            }

            // check for const, other attributes, and pure virtual functions, ignoring the body if present
            for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.get()) {
                decl.constMember |= token.match(Token.CONST, Token.__CONST, Token.CONSTEXPR);
                Attribute attr = attribute();
                if (attr != null && attr.annotation) {
                    dcl.type.annotations += attr.javaName;
                } else if (attr == null) {
                    break;
                }
            }
            if (tokens.get().match('{')) {
                body();
            } else {
                // may be a pure virtual function
                if (tokens.get().match('=')) {
                    Token token = tokens.next().expect("0", Token.DELETE, Token.DEFAULT);
                    if (token.match("0")) {
                        decl.abstractMember = true;
                    } else if (token.match(Token.DELETE)) {
                        decl.text = spacing;
                        declList.add(decl);
                        return true;
                    }
                    tokens.next().expect(';');
                }
                tokens.next();
            }

            // add @Const annotation only for const virtual functions
            if (decl.constMember && type.virtual && context.virtualize) {
                if (type.annotations.contains("@Const")) {
                    type.annotations = incorporateConstAnnotation(type.annotations, 2, true);
                } else {
                    type.annotations += "@Const({false, false, true}) ";
                }
            }

            // add @Virtual annotation on user request only, inherited through context
            if (type.virtual && context.virtualize) {
                modifiers = "@Virtual" + (decl.abstractMember ? "(true) " : " ")
                          + (context.inaccessible ? "protected native " : "public native ");
            }

            // compose the text of the declaration with the info we got up until this point
            decl.declarator = dcl;
            if (context.namespace != null && context.javaName == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            // append annotations specified for a full function declaration only to avoid overlap with type.annotations
            if (fullInfo != null && fullInfo.annotations != null) {
                for (String s : fullInfo.annotations) {
                    decl.text += s + " ";
                }
            }
            if (type.constructor && params != null) {
                decl.text += "public " + context.shorten(context.javaName) + dcl.parameters.list + " { super((Pointer)null); allocate" + params.names + "; }\n" +
                             "private native " + type.annotations + "void allocate" + dcl.parameters.list + ";\n";
            } else {
                decl.text += modifiers + type.annotations + context.shorten(type.javaName) + " " + dcl.javaName + dcl.parameters.list + ";\n";
            }
            decl.signature = dcl.signature;

            if (useDefaults) {
                // we cannot override when leaving out parameters with default arguments
                decl.text = decl.text.replaceAll("@Override ", "");
            }

            // replace all of the declaration by user specified text
            if (info != null && info.javaText != null) {
                if (first) {
                    decl.signature = decl.text = info.javaText;
                } else {
                    break;
                }
            }
            String comment = commentAfter();
            if (first) {
                declList.spacing = spacing;
                decl.text = comment + decl.text;
            }
            decl.function = true;

            // only add nonduplicate declarations and ignore destructors
            boolean found = false;
            for (Declarator d : prevDcl) {
                found |= dcl.signature.equals(d.signature);
            }
            if (dcl.javaName.length() > 0 && !found && (!type.destructor || (info != null && info.javaText != null))) {
                if (declList.add(decl)) {
                    first = false;
                }
                if (type.virtual && context.virtualize) {
                    break;
                }
            } else if (found && n / 2 > 0 && n % 2 == 0 && n / 2 > Math.max(dcl.infoNumber, dcl.parameters.infoNumber)) {
                break;
            }
            prevDcl.add(dcl);
        }
        declList.spacing = null;
        return true;
    }

    boolean variable(Context context, DeclarationList declList) throws ParserException {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        String modifiers = "public static native ";
        String setterType = "void ";
        Declarator dcl = declarator(context, null, 0, false, 0, false, true);
        Declaration decl = new Declaration();
        String cppName = dcl.cppName;
        String javaName = dcl.javaName;

        Attribute attr = attribute();
        if (attr != null && attr.annotation) {
            dcl.type.annotations += attr.javaName;
        }

        if (cppName == null || javaName == null || !tokens.get().match('(', '[', '=', ',', ':', ';', '{')) {
            tokens.index = backIndex;
            return false;
        } else if (!dcl.type.staticMember && context.javaName != null) {
            modifiers = "public native ";
            setterType = context.shorten(context.javaName) + " ";
        }

        int namespace = cppName.lastIndexOf("::");
        if (context.namespace != null && namespace < 0) {
            cppName = context.namespace + "::" + cppName;
        }
        Info info = infoMap.getFirst(cppName);
        if (dcl.cppName.length() == 0 || (info != null && info.skip)) {
            decl.text = spacing;
            declList.add(decl);
            while (!tokens.get().match(Token.EOF, ';')) {
                tokens.next();
            }
            tokens.next();
            return true;
        } else if (info == null) {
            Info info2 = infoMap.getFirst(dcl.cppName);
            infoMap.put(info2 != null ? new Info(info2).cppNames(cppName) : new Info(cppName));
        }
        boolean first = true;
        Declarator metadcl = context.variable;
        for (int n = 0; n < Integer.MAX_VALUE; n++) {
            decl = new Declaration();
            tokens.index = backIndex;
            dcl = declarator(context, null, -1, false, n, false, true);
            if (dcl == null || dcl.cppName == null) {
                break;
            }
            decl.declarator = dcl;
            cppName = dcl.cppName;
            namespace = cppName.lastIndexOf("::");
            if (context.namespace != null && namespace < 0) {
                cppName = context.namespace + "::" + cppName;
            }
            info = infoMap.getFirst(cppName);

            namespace = cppName.lastIndexOf("::");
            String shortName = cppName;
            if (namespace >= 0) {
                shortName = cppName.substring(namespace + 2);
            }
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
                if (context.namespace != null && context.javaName == null) {
                    decl.text += "@Namespace(\"" + context.namespace + "\") ";
                }
                if (metadcl != null && metadcl.cppName.length() > 0) {
                    decl.text += metadcl.indices == 0
                            ? "@Name(\"" + metadcl.cppName + "." + shortName + "\") "
                            : "@Name({\"" + metadcl.cppName + "\", \"." + shortName + "\"}) ";
                    dcl.type.annotations = dcl.type.annotations.replaceAll("@Name\\(.*\\) ", "");
                    javaName = metadcl.javaName + "_" + shortName;
                }
                if (dcl.type.constValue || dcl.constPointer) {
                    decl.text += "@MemberGetter ";
                }
                decl.text += modifiers + dcl.type.annotations.replace("@ByVal ", "@ByRef ")
                          + dcl.type.javaName + " " + javaName + "(" + indices + ");";
                if (!dcl.type.constValue && !dcl.constPointer) {
                    if (indices.length() > 0) {
                        indices += ", ";
                    }
                    String javaTypeWithoutAnnotations = dcl.type.javaName.substring(dcl.type.javaName.lastIndexOf(" ") + 1);
                    decl.text += " " + modifiers + setterType + javaName + "(" + indices + javaTypeWithoutAnnotations + " " + javaName + ");";
                }
                decl.text += "\n";
                if ((dcl.type.constValue || dcl.constPointer) && dcl.type.staticMember && indices.length() == 0) {
                    String rawType = dcl.type.javaName.substring(dcl.type.javaName.lastIndexOf(' ') + 1);
                    if ("byte".equals(rawType) || "short".equals(rawType) || "int".equals(rawType) || "long".equals(rawType)
                            || "float".equals(rawType) || "double".equals(rawType) || "char".equals(rawType) || "boolean".equals(rawType)) {
                        // only mind of what looks like constants that we can keep without hogging memory
                        decl.text += "public static final " + rawType + " " + javaName + " = " + javaName + "();\n";
                    }
                }
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
                if (context.namespace != null && context.javaName == null) {
                    decl.text += "@Namespace(\"" + context.namespace + "\") ";
                }
                if (metadcl != null && metadcl.cppName.length() > 0) {
                    decl.text += metadcl.indices == 0
                            ? "@Name(\"" + metadcl.cppName + "." + shortName + "\") "
                            : "@Name({\"" + metadcl.cppName + "\", \"." + shortName + "\"}) ";
                    dcl.type.annotations = dcl.type.annotations.replaceAll("@Name\\(.*\\) ", "");
                    javaName = metadcl.javaName + "_" + shortName;
                }
                tokens.index = backIndex;
                Declarator dcl2 = declarator(context, null, -1, false, n, false, false);
                if (dcl2.type.constValue || dcl2.constPointer || dcl2.indirections < 2) {
                    decl.text += "@MemberGetter ";
                }
                decl.text += modifiers + dcl.type.annotations.replace("@ByVal ", "@ByRef ")
                          + dcl.type.javaName + " " + javaName + "(" + indices + ");";
                if (!dcl.type.constValue && !dcl.constPointer && !(dcl2.indirections < 2)) {
                    if (indices.length() > 0) {
                        indices += ", ";
                    }
                    decl.text += " " + modifiers + setterType + javaName + "(" + indices + dcl.type.javaName + " " + javaName + ");";
                }
                decl.text += "\n";
            }
            decl.signature = dcl.signature;
            if (info != null && info.javaText != null) {
                decl.signature = decl.text = info.javaText;
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
        declList.spacing = null;
        return true;
    }

    boolean macro(Context context, DeclarationList declList) throws ParserException {
        int backIndex = tokens.index;
        if (!tokens.get().match('#')) {
            return false;
        }
        tokens.raw = true;
        String spacing = tokens.get().spacing;
        Token keyword = tokens.next();

        // parse all of the macro to find its last token
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
        if (keyword.match(Token.DEFINE) && beginIndex < endIndex) {
            tokens.index = beginIndex;
            String macroName = tokens.get().value;
            Token first = tokens.next();
            boolean hasArgs = first.spacing.length() == 0 && first.match('(');
            List<Info> infoList = infoMap.get(macroName);
            for (Info info : infoList.size() > 0 ? infoList : Arrays.asList(new Info[] { null })) {
                if (info != null && info.skip) {
                    break;
                } else if ((info == null && (hasArgs || beginIndex + 1 == endIndex))
                        || (info != null && info.cppText == null && info.cppTypes != null && info.cppTypes.length == 0)) {
                    // save declaration for expansion
                    info = new Info(macroName).cppText("");
                    tokens.index = backIndex;
                    for (Token token = tokens.get(); tokens.index < endIndex; token = tokens.next()) {
                        info.cppText += token.match("\n") ? token : token.spacing + token;
                    }
                    infoMap.put(info);
                    break;
                } else if (info != null && info.cppText == null &&
                        info.cppTypes != null && info.cppTypes.length > (hasArgs ? 0 : 1)) {
                    // declare as a static native method
                    List<Declarator> prevDcl = new ArrayList<Declarator>();
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
                            decl.signature = dcl.signature;
                        } else if (found && n > 0) {
                            break;
                        }
                        prevDcl.add(dcl);
                    }
                } else if (lastIndex > beginIndex + 1 && (info == null || (info.cppText == null &&
                        (info.cppTypes == null || info.cppTypes.length == 1)))) {
                    // declare as a static final variable
                    String value = "";
                    String cppType = "int";
                    String type = "int";
                    String cat = "";
                    tokens.index = beginIndex + 1;
                    Token prevToken = new Token();
                    boolean translate = true;
                    for (Token token = tokens.get(); tokens.index < lastIndex; token = tokens.next()) {
                        if (token.match(Token.STRING)) {
                            cppType = "const char*"; type = "String"; cat = " + "; break;
                        } else if (token.match(Token.FLOAT)) {
                            cppType = "double"; type = "double"; cat = ""; break;
                        } else if (token.match(Token.INTEGER) && token.value.endsWith("L")) {
                            cppType = "long long"; type = "long"; cat = ""; break;
                        } else if ((prevToken.match(Token.IDENTIFIER, '>') && token.match(Token.IDENTIFIER, '(')) || token.match('{', '}')) {
                            translate = false;
                        } else if (token.match(Token.IDENTIFIER)) {
                            // get types for the values of the macro
                            Info info2 = infoMap.getFirst(token.value);
                            if (info == null && info2 != null && info2.cppTypes != null) {
                                info = info2;
                            }
                        }
                        prevToken = token;
                    }
                    if (info != null) {
                        if (info.cppTypes != null && info.cppTypes.length > 0) {
                            Declarator dcl = new Parser(this, info.cppTypes[0]).declarator(context, null, -1, false, 0, false, true);
                            if (!dcl.type.javaName.equals("int")) {
                                cppType = dcl.type.cppName;
                                type = dcl.type.annotations + (info.pointerTypes != null ? info.pointerTypes[0] : dcl.type.javaName);;
                            }
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
                            value += token.spacing;
                            if (type.equals("String") && token.match("L")) {
                                // strip unnecessary prefixes from strings
                                continue;
                            }
                            value += token + (tokens.index + 1 < lastIndex && token.value.trim().length() > 0 ? cat : "");
                        }
                        value = translate(value);
                        if (type.equals("int")) {
                            if (value.contains("(String)")) {
                                cppType = "const char*"; type = "String";
                            } else if (value.contains("(float)") || value.contains("(double)")) {
                                cppType = "double"; type = "double";
                            } else if (value.contains("(long)")) {
                                cppType = "long long"; type = "long";
                            } else {
                                try {
                                    String trimmedValue = value.trim();
                                    long longValue = Long.parseLong(trimmedValue);
                                    if (longValue > Integer.MAX_VALUE && (longValue >>> 32) == 0) {
                                        // probably some unsigned value, so let's just cast to int
                                        value = value.substring(0, value.length() - trimmedValue.length()) + "(int)" + trimmedValue + "L";
                                    } else if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
                                        cppType = "long long"; type = "long"; value += "L";
                                    }
                                } catch (NumberFormatException e) {
                                    // leave as int?
                                }
                            }
                        }
                    } else {
                        if (info != null && info.annotations != null) {
                            for (String s : info.annotations) {
                                decl.text += s + " ";
                            }
                        }
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
                    decl.signature = macroName;
                    if (info == null || !Arrays.asList(info.cppNames).contains(macroName)) {
                        // save the C++ type (and Java type via pointerTypes) to propagate to other macros referencing this one
                        infoMap.put(new Info(macroName).define(true).cppTypes(cppType).pointerTypes(type).translate(translate));
                    }
                }
                if (info != null && info.javaText != null) {
                    decl.signature = decl.text = info.javaText;
                    break;
                }
            }
        }

        if (decl.text.length() == 0) {
            // output whatever we did not process as comment
            tokens.index = beginIndex;
            int n = spacing.lastIndexOf('\n') + 1;
            decl.text += "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
            for (Token token = tokens.get(); tokens.index < lastIndex; token = tokens.next()) {
                decl.text += token.match("\n") ? "\n// " : token.spacing + token.toString().replace("\n", "\n//");
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

    boolean typedef(Context context, DeclarationList declList) throws ParserException {
        String spacing = tokens.get().spacing;
        // the "using" token can also act as a "typedef"
        String usingDefName = tokens.get().match(Token.USING) && tokens.get(1).match(Token.IDENTIFIER)
                && tokens.get(2).match('=') ? tokens.get(1).value : null;
        if (!tokens.get().match(Token.TYPEDEF) && !tokens.get(1).match(Token.TYPEDEF) && usingDefName == null) {
            return false;
        }
        Declaration decl = new Declaration();
        if (usingDefName != null) {
            tokens.next().expect(Token.IDENTIFIER);
            tokens.next().expect('=');
            tokens.next();
        }
        int backIndex = tokens.index;
        for (int n = 0; n < Integer.MAX_VALUE; n++) {
            decl = new Declaration();
            tokens.index = backIndex;
            Declarator dcl = declarator(context, null, 0, false, n, true, false);
            if (dcl == null) {
                break;
            }
            if (usingDefName != null) {
                dcl.cppName = usingDefName;
            }
            if (attribute() == null) {
                tokens.next();
            }

            String typeName = dcl.type.cppName, defName = dcl.cppName;
            if (defName == null) {
                dcl.cppName = defName = typeName;
            }
            if (dcl.javaName == null) {
                dcl.javaName = dcl.cppName;
            }
            int namespace = defName.lastIndexOf("::");
            if (context.namespace != null && namespace < 0) {
                defName = context.namespace + "::" + defName;
            }
            Info info = infoMap.getFirst(defName);
            if (dcl.definition != null) {
                // a function pointer or something
                decl = dcl.definition;
                if (dcl.javaName.length() > 0 && context.javaName != null) {
                    dcl.javaName = context.javaName + "." + dcl.javaName;
                }
                if (info == null || !info.skip) {
                    info = info != null ? new Info(info).cppNames(defName) : new Info(defName);
                    infoMap.put(info.valueTypes(dcl.javaName)
                            .pointerTypes((dcl.indirections > 0 ? "@ByPtrPtr " : "") + dcl.javaName));
                }
            } else if (typeName.equals("void")) {
                // some opaque data type
                if ((info == null || !info.skip) && !dcl.javaName.equals("Pointer")) {
                    if (dcl.indirections > 0) {
                        decl.text += "@Namespace @Name(\"void\") ";
                        info = info != null ? new Info(info).cppNames(defName) : new Info(defName);
                        infoMap.put(info.valueTypes(dcl.javaName).pointerTypes("@ByPtrPtr " + dcl.javaName));
                    } else if (context.namespace != null && context.javaName == null) {
                        decl.text += "@Namespace(\"" + context.namespace + "\") ";
                    }
                    decl.type = new Type(dcl.javaName);
                    decl.text += "@Opaque public static class " + dcl.javaName + " extends Pointer {\n" +
                                 "    /** Empty constructor. Calls {@code super((Pointer)null)}. */\n" +
                                 "    public " + dcl.javaName + "() { super((Pointer)null); }\n" +
                                 "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n" +
                                 "    public " + dcl.javaName + "(Pointer p) { super(p); }\n" +
                                 "}";
                }
            } else {
                // point back to original type
                info = infoMap.getFirst(typeName);
                if (info == null || !info.skip) {
                    info = info != null ? new Info(info).cppNames(defName) : new Info(defName);
                    if (info.cppTypes == null && info.annotations != null) {
                        // set original C++ type for typedef of types we want to use with adapters
                        String s = typeName;
                        if (dcl.type.constValue && !s.startsWith("const ")) {
                            s = "const " + s;
                        }
                        if (dcl.type.constPointer && !s.endsWith(" const")) {
                            s = s + " const";
                        }
                        if (dcl.type.indirections > 0) {
                            for (int i = 0; i < dcl.type.indirections; i++) {
                                s += "*";
                            }
                        }
                        if (dcl.type.reference) {
                            s += "&";
                        }
                        info.cppNames(defName, s).cppTypes(s);
                    }
                    if (info.valueTypes == null && dcl.indirections > 0) {
                        info.valueTypes(info.pointerTypes != null ? info.pointerTypes : new String[] {typeName});
                        info.pointerTypes("PointerPointer");
                    } else if (info.pointerTypes == null) {
                        info.pointerTypes(typeName);
                    }
                    if (info.annotations == null) {
                        if (dcl.type.annotations != null && dcl.type.annotations.length() > 0
                                && !dcl.type.annotations.startsWith("@ByVal ")
                                && !dcl.type.annotations.startsWith("@Cast(")
                                && !dcl.type.annotations.startsWith("@Const ")) {
                            info.annotations(dcl.type.annotations.trim());
                        } else {
                            info.cast(!dcl.cppName.equals(info.pointerTypes[0]));
                        }
                    }
                    infoMap.put(info);
                }
            }

            if (info != null && info.javaText != null) {
                decl.signature = decl.text = info.javaText;
            }
            String comment = commentAfter();
            decl.text = comment + decl.text;
            declList.spacing = spacing;
            declList.add(decl);
            declList.spacing = null;
        }
        return true;
    }

    boolean using(Context context, DeclarationList declList) throws ParserException {
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
        Info info = infoMap.getFirst(dcl.type.cppName);
        if (!context.inaccessible && info != null && info.javaText != null) {
            // inherit constructors
            decl.signature = decl.text = info.javaText;
            decl.declarator = dcl;
        }
        String comment = commentAfter();
        decl.text = comment + decl.text;
        declList.spacing = spacing;
        declList.add(decl);
        declList.spacing = null;
        return true;
    }

    boolean group(Context context, DeclarationList declList) throws ParserException {
        int backIndex = tokens.index;
        String spacing = tokens.get().spacing;
        boolean typedef = tokens.get().match(Token.TYPEDEF) || tokens.get(1).match(Token.TYPEDEF);
        boolean foundGroup = false, friend = false;
        Context ctx = new Context(context);
        Token[] prefixes = {Token.CLASS, Token.INTERFACE, Token.__INTERFACE, Token.STRUCT, Token.UNION};
        for (Token token = tokens.get(); !token.match(Token.EOF); token = tokens.next()) {
            if (token.match(prefixes)) {
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

        tokens.next().expect(Token.IDENTIFIER, '{', "::");
        if (!tokens.get().match('{') && tokens.get(1).match(Token.IDENTIFIER)
                && !tokens.get(1).match(Token.FINAL)
                && (typedef || !tokens.get(2).match(';'))) {
            tokens.next();
        }
        Type type = type(context, true);
        List<Type> baseClasses = new ArrayList<Type>();
        Declaration decl = new Declaration();
        decl.text = type.annotations;
        String name = type.javaName;
        boolean anonymous = !typedef && type.cppName.length() == 0, derivedClass = false, skipBase = false;
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
                Info info = infoMap.getFirst(t.cppName);
                if (info != null && info.skip) {
                    skipBase = true;
                }
                if (accessible) {
                    baseClasses.add(t);
                }
                if (tokens.get().expect(',', '{').match('{')) {
                    break;
                }
            }
        }
        if (typedef && type.indirections > 0) {
            // skip pointer typedef
            while (!tokens.get().match(';', Token.EOF)) {
                tokens.next();
            }
        }
        if (!tokens.get().match('{', ',', ';')) {
            tokens.index = backIndex;
            return false;
        }
        int startIndex = tokens.index;
        List<Declarator> variables = new ArrayList<Declarator>();
        String originalName = type.cppName;
        String body = body();
        boolean hasBody = body != null && body.length() > 0;
        if (!tokens.get().match(';')) {
            if (typedef) {
                Token token = tokens.get();
                while (!token.match(';', Token.EOF)) {
                    int indirections = 0;
                    while (token.match('*')) {
                        indirections++;
                        token = tokens.next();
                    }
                    if (token.match(Token.IDENTIFIER)) {
                        String name2 = token.value;
                        // use first typedef name, unless it's something weird like a pointer
                        if (indirections > 0) {
                            infoMap.put(new Info(name2).cast().valueTypes(name).pointerTypes("PointerPointer"));
                        } else {
                            if (type.cppName.equals(originalName)) {
                                name = type.javaName = type.cppName = token.value;
                            }
                            infoMap.put(new Info(name2).cast().pointerTypes(name));
                        }
                    }
                    token = tokens.next();
                }
                decl.text += token.spacing;
            } else {
                int index = tokens.index - 1;
                for (int n = 0; n < Integer.MAX_VALUE; n++) {
                    tokens.index = index;
                    Declarator dcl = declarator(context, null, -1, false, n, false, true);
                    if (dcl == null) {
                        break;
                    } else {
                        // declares variable, treat as anonymous
                        anonymous = true;
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
            originalName = context.namespace + "::" + originalName;
        }
        Info info = infoMap.getFirst(type.cppName);
        if (((info == null || info.base == null) && skipBase) || (info != null && info.skip)) {
            decl.text = "";
            declList.add(decl);
            return true;
        } else if (info != null && info.pointerTypes != null && info.pointerTypes.length > 0) {
            type.javaName = info.pointerTypes[0];
            name = context.shorten(type.javaName);
        } else if (info == null) {
            if (type.javaName.length() > 0 && context.javaName != null) {
                type.javaName = context.javaName + "." + type.javaName;
            }
            infoMap.put(info = new Info(type.cppName).pointerTypes(type.javaName));
        }
        Type base = new Type("Pointer");
        Iterator<Type> it = baseClasses.iterator();
        while (it.hasNext()) {
            Type next = it.next();
            Info nextInfo = infoMap.getFirst(next.cppName);
            if (nextInfo == null || !nextInfo.flatten) {
                base = next;
                it.remove();
                break;
            }
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
        if (name.length() > 0 && !hasBody) {
            // incomplete type (forward or friend declaration)
            if (!tokens.get().match(';')) {
                tokens.next();
                tokens.next();
            }
            tokens.next();
            if (friend) {
                decl.text = "";
                declList.add(decl);
                return true;
            } else if (info != null && info.base != null) {
                base.javaName = info.base;
            }
            if (name.equals("Pointer")) {
                return true;
            }
            String fullName = context.namespace != null ? context.namespace + "::" + name : name;
            String cppName = type.cppName;
            for (Token prefix : prefixes) {
                if (info != null && info.cppNames[0].startsWith(prefix.value + " ")) {
                    // make it possible to force resolution with a prefix
                    cppName = prefix.value + " " + cppName;
                    break;
                }
            }
            if (!fullName.equals(cppName)) {
                decl.text += "@Name(\"" + (context.javaName == null || namespace < 0 ? cppName : cppName.substring(namespace + 2)) + "\") ";
            } else if (context.namespace != null && context.javaName == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            decl.type = new Type(name);
            decl.text += "@Opaque public static class " + name + " extends " + base.javaName + " {\n" +
                         "    /** Empty constructor. Calls {@code super((Pointer)null)}. */\n" +
                         "    public " + name + "() { super((Pointer)null); }\n" +
                         "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n" +
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

        if (type.cppName.length() > 0) {
            // name of the typedef type
            ctx.namespace = type.cppName;
            // used to match constructors
            ctx.cppName = originalName;
        }
        if (!anonymous) {
            // used for setter methods
            ctx.javaName = type.javaName;
        }
        if (info != null && info.virtualize) {
            ctx.virtualize = true;
        }

        DeclarationList declList2 = new DeclarationList();
        if (variables.size() == 0) {
            declarations(ctx, declList2);
        } else for (Declarator var : variables) {
            if (context.variable != null) {
                var.cppName = context.variable.cppName + "." + var.cppName;
                var.javaName = context.variable.javaName + "_" + var.javaName;
            }
            ctx.variable = var;
            declarations(ctx, declList2);
        }
        String modifiers = "public static ", constructors = "", explicitConstructors = "";
        boolean implicitConstructor = true, defaultConstructor = false, longConstructor = false,
                pointerConstructor = false, abstractClass = info != null && info.purify && !ctx.virtualize,
                allPureConst = true, haveVariables = false;
        for (Declaration d : declList2) {
            if (d.declarator != null && d.declarator.type != null && d.declarator.type.using && decl.text != null) {
                // inheriting constructors
                defaultConstructor |= d.text.contains("private native void allocate();");
                longConstructor |= d.text.contains("private native void allocate(long");
                pointerConstructor |= d.text.contains("private native void allocate(Pointer");
                implicitConstructor &= !defaultConstructor && !longConstructor && !pointerConstructor;
                String baseType = d.declarator.type.cppName;
                baseType = baseType.substring(0, baseType.lastIndexOf("::"));
                Info info2 = infoMap.getFirst(baseType);
                constructors = d.text.replace(info2.pointerTypes[0], name) + "\n";
                d.text = "";
            } else if (d.declarator != null && d.declarator.type != null && d.declarator.type.constructor) {
                implicitConstructor = false;
                Declarator[] paramDcls = d.declarator.parameters.declarators;
                defaultConstructor |= (paramDcls.length == 0 || (paramDcls.length == 1 && paramDcls[0].type.javaName.equals("void"))) && !d.inaccessible;
                longConstructor |= paramDcls.length == 1 && paramDcls[0].type.javaName.equals("long") && !d.inaccessible;
                pointerConstructor |= paramDcls.length == 1 && paramDcls[0].type.javaName.equals("Pointer") && !d.inaccessible;
            }
            abstractClass |= d.abstractMember;
            allPureConst &= d.constMember && d.abstractMember;
            haveVariables |= d.variable;
        }
        if (allPureConst && ctx.virtualize) {
            modifiers = "@Const " + modifiers;
        }
        if (!anonymous) {
            String fullName = context.namespace != null ? context.namespace + "::" + name : name;
            String cppName = type.cppName;
            for (Token prefix : prefixes) {
                if (info != null && info.cppNames[0].startsWith(prefix.value + " ")) {
                    // make it possible to force resolution with a prefix
                    cppName = prefix.value + " " + cppName;
                    break;
                }
            }
            if (!fullName.equals(cppName)) {
                decl.text += "@Name(\"" + cppName + "\") ";
            } else if (context.namespace != null && context.javaName == null) {
                decl.text += "@Namespace(\"" + context.namespace + "\") ";
            }
            if ((!implicitConstructor || derivedClass) && haveVariables) {
                decl.text += "@NoOffset ";
            }
            if (info != null && info.base != null) {
                base.javaName = info.base;
            }
            String shortName = name.substring(name.lastIndexOf('.') + 1);
            decl.text += modifiers + "class " + shortName + " extends " + base.javaName + " {\n" +
                         "    static { Loader.load(); }\n";

            explicitConstructors = constructors;
            if (implicitConstructor && (info == null || !info.purify) && (!abstractClass || ctx.virtualize)) {
                constructors += "    /** Default native constructor. */\n" +
                             "    public " + shortName + "() { super((Pointer)null); allocate(); }\n" +
                             "    /** Native array allocator. Access with {@link Pointer#position(long)}. */\n" +
                             "    public " + shortName + "(long size) { super((Pointer)null); allocateArray(size); }\n" +
                             "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n" +
                             "    public " + shortName + "(Pointer p) { super(p); }\n" +
                             "    private native void allocate();\n" +
                             "    private native void allocateArray(long size);\n" +
                             "    @Override public " + shortName + " position(long position) {\n" +
                             "        return (" + shortName + ")super.position(position);\n" +
                             "    }\n";
            } else {
                if (!pointerConstructor) {
                    constructors += "    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */\n" +
                                 "    public " + shortName + "(Pointer p) { super(p); }\n";
                }
                if (defaultConstructor && (info == null || !info.purify) && (!abstractClass || ctx.virtualize) && !longConstructor) {
                    constructors += "    /** Native array allocator. Access with {@link Pointer#position(long)}. */\n" +
                                 "    public " + shortName + "(long size) { super((Pointer)null); allocateArray(size); }\n" +
                                 "    private native void allocateArray(long size);\n" +
                                 "    @Override public " + shortName + " position(long position) {\n" +
                                 "        return (" + shortName + ")super.position(position);\n" +
                                 "    }\n";
                }
            }
            decl.text += constructors;
            declList.spacing = spacing;
            decl.text = declList.rescan(decl.text + casts + "\n");
            declList.spacing = null;
        }
        for (Type base2 : baseClasses) {
            Info baseInfo = infoMap.getFirst(base2.cppName);
            if (baseInfo != null && baseInfo.flatten && baseInfo.javaText != null) {
                String text = baseInfo.javaText;
                int start = text.indexOf('{');
                for (int n = 0; n < 2; start++) {
                    int c = text.charAt(start);
                    if (c == '\n') {
                        n++;
                    } else if (!Character.isWhitespace(c)) {
                        n = 0;
                    }
                }
                int end = text.lastIndexOf('}');
                decl.text += text.substring(start, end).replaceAll("(\\s+)" + base2.javaName + "(\\s+)", "$1" + type.javaName + "$2");
            }
        }
        for (Declaration d : declList2) {
            if (!d.inaccessible && (d.declarator == null || d.declarator.type == null || !d.declarator.type.constructor || !abstractClass)) {
                decl.text += d.text;
                if (d.declarator != null && d.declarator.type != null && d.declarator.type.constructor) {
                    explicitConstructors += d.text;
                }
            }
        }
        String constructorName = originalName;
        int template2 = constructorName.lastIndexOf('<');
        if (template2 >= 0) {
            constructorName = constructorName.substring(0, template2);
        }
        int namespace2 = constructorName.lastIndexOf("::");
        if (namespace2 >= 0) {
            constructorName = constructorName.substring(namespace2 + 2);
        }
        Info constructorInfo = infoMap.getFirst(type.cppName + "::" + constructorName);
        if ((context.templateMap == null || context.templateMap.full()) && constructorInfo == null) {
            // save constructors to be able inherit them with C++11 "using" statements
            infoMap.put(new Info(type.cppName + "::" + constructorName).javaText(explicitConstructors));
        }
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
            decl.signature = decl.text = info.javaText;
        } else if (info != null && info.flatten) {
            info.javaText = decl.text;
        }
        declList.add(decl);
        return true;
    }

    boolean enumeration(Context context, DeclarationList declList) throws ParserException {
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
        boolean enumClass = false;
        String enumType = "enum";
        if (tokens.get(1).match(Token.CLASS, Token.STRUCT)) {
            enumClass = true;
            enumType += " " + tokens.next();
        }
        if (typedef && !tokens.get(1).match('{') && tokens.get(2).match(Token.IDENTIFIER)) {
            tokens.next();
        }
        int count = 0;
        boolean longenum = false;
        String cppType = "int";
        String javaType = "int";
        String separator = "";
        String enumPrefix = "public static final " + javaType;
        String countPrefix = "";
        String enumerators = "";
        String enumerators2 = "";
        HashMap<String,String> enumeratorMap = new HashMap<String,String>();
        String extraText = "";
        String name = "";
        Token token = tokens.next().expect(Token.IDENTIFIER, '{', ':', ';');
        if (token.match(Token.IDENTIFIER)) {
            while (tokens.get(1).match(Token.IDENTIFIER)) {
                Attribute attr = attribute(true);
                if (attr != null && attr.annotation) {
                    // XXX: What to do with annotations here?
                } else {
                    break;
                }
                token = tokens.get();
            }
            name = token.value;
            token = tokens.next();
        }
        if (token.match(':')) {
            token = tokens.next();
            Type type = type(context);
            cppType = type.cppName;
            javaType = type.javaName;
            enumPrefix = "public static final " + javaType;
            token = tokens.get();
        }
        if (!typedef && token.match(';')) {
            // skip for loop
        } else if (!token.match('{')) {
            // not an enum
            tokens.index = backIndex;
            return false;
        } else for (token = tokens.next(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            String comment = commentBefore();
            if (macro(context, declList)) {
                Declaration macroDecl = declList.remove(declList.size() - 1);
                extraText += comment + macroDecl.text;
                if (separator.equals(",") && !macroDecl.text.trim().startsWith("//")) {
                    separator = ";";
                    enumPrefix = "public static final " + javaType;
                }
                continue;
            }
            Token enumerator = tokens.get();
            String cppName = enumerator.value;
            if (cppName == null || cppName.length() == 0) {
                // skip over stray commas, etc
                tokens.next().spacing = token.spacing;
                continue;
            }
            String javaName = cppName;
            if (enumClass) {
                cppName = name + "::" + cppName;
            }
            if (context.namespace != null) {
                cppName = context.namespace + "::" + cppName;
            }
            Info info = infoMap.getFirst(cppName);
            if (info != null && info.javaNames != null && info.javaNames.length > 0) {
                javaName = info.javaNames[0];
            } else if (info == null) {
                infoMap.put(info = new Info(cppName).cppText(""));
            }
            String spacing2 = "";
            if (tokens.next().match('=')) {
                spacing2 = tokens.get().spacing;
                if (spacing2.length() > 0 && spacing2.charAt(0) == ' ') {
                    spacing2 = spacing2.substring(1);
                }
                countPrefix = "";
                int count2 = 0;
                Token prevToken = new Token();
                boolean translate = true;
                for (token = tokens.next(); !token.match(Token.EOF, '#', ',', '}') || count2 > 0; token = tokens.next()) {
                    if (token.match(Token.INTEGER) && token.value.endsWith("L")) {
                        longenum = true;
                    }
                    countPrefix += token.spacing + token;
                    if (token.match('(')) {
                        count2++;
                    } else if (token.match(')')) {
                        count2--;
                    }
                    if ((prevToken.match(Token.IDENTIFIER) && token.match('(')) || token.match('{', '}')) {
                        translate = false;
                    }
                    prevToken = token;
                }
                try {
                    count = Integer.parseInt(countPrefix.trim());
                    countPrefix = "";
                } catch (NumberFormatException e) {
                    count = 0;
                    if (translate) {
                        countPrefix = translate(countPrefix);
                        if (countPrefix.length() > 0 && countPrefix.charAt(0) == ' ') {
                            countPrefix = countPrefix.substring(1);
                        }
                    } else {
                        if (separator.equals(",")) {
                            separator = ";";
                        }
                        extraText += "\npublic static native @MemberGetter " + javaType + " " + javaName + "();\n";
                        enumPrefix = "public static final " + javaType;
                        countPrefix = javaName + "()";
                    }
                }
            }
            if (extraText.length() > 0 && !extraText.endsWith("\n") && enumPrefix.length() > 0) {
                extraText += comment + "\n";
                comment = "";
            }
            String text = separator + extraText + enumPrefix + comment;
            String text2 = separator + comment;
            comment = commentAfter();
            if (comment.length() == 0 && tokens.get().match(',')) {
                tokens.next();
                comment = commentAfter();
            }
            String spacing = enumerator.spacing;
            if (comment.length() > 0) {
                text += spacing + comment;
                text2 += spacing + comment;
                int newline = spacing.lastIndexOf('\n');
                if (newline >= 0) {
                    spacing = spacing.substring(newline + 1);
                }
            }
            if (spacing.length() == 0 && !text.endsWith(",")) {
                spacing = " ";
            }
            String cast = javaType.equals("byte") || javaType.equals("short") ? "(" + javaType + ")" : "";
            text += spacing + javaName + spacing2 + " = " + countPrefix;
            text2 += spacing + javaName + spacing2 + "(" + cast + countPrefix;
            if (countPrefix.trim().length() > 0) {
                if (count > 0) {
                    text += " + " + count;
                    if (enumeratorMap.containsKey(countPrefix)) {
                        text2 += ".value";
                    }
                    text2 += " + " + count;
                }
            } else {
                text += count;
                text2 += count;
            }
            count++;

            if (info == null || !info.skip) {
                enumerators += text;
                enumerators2 += text2 + ")";
                enumeratorMap.put(javaName, text);
                separator = ",";
                enumPrefix = "";
                extraText = "";
            }
        }
        if (longenum) {
            enumerators = enumerators.replace(" " + javaType, " long");
            cppType = "long long";
            javaType = "long";
        }
        String comment = commentBefore();
        Declaration decl = new Declaration();
        token = tokens.get();
        while (!token.match(';', Token.EOF)) {
            // deal with typedefs
            int indirections = 0;
            while (token.match('*')) {
                indirections++;
                token = tokens.next();
            }
            if (token.match(Token.IDENTIFIER)) {
                // XXX: If !typedef, this is a variable declaration with anonymous type
                String name2 = token.value;
                if (name == null || name.length() == 0) {
                    name = name2;
                }
                Info info2 = infoMap.getFirst(cppType);
                if (indirections > 0) {
                    infoMap.put(new Info(info2).cast().cppNames(name2).valueTypes(info2.pointerTypes).pointerTypes("PointerPointer"));
                } else {
                    infoMap.put(new Info(info2).cast().cppNames(name2));
                }
            }
            token = tokens.next();
        }
        String cppName = context.namespace != null ? context.namespace + "::" + name : name;
        Info info = infoMap.getFirst(cppName);
        boolean enumerate = info != null ? info.enumerate : false;
        for (Info i : infoMap.get(null)) {
            enumerate |= i.enumerate;
        }
        if (info != null && info.skip) {
            decl.text = enumSpacing;
        } else {
            int newline = enumSpacing.lastIndexOf('\n');
            String enumSpacing2 = newline < 0 ? enumSpacing : enumSpacing.substring(newline + 1);
            if (enumerate) {
                String javaName = info != null && info.valueTypes != null && info.valueTypes.length > 0 ? info.valueTypes[0] : name;
                String fullName = context.namespace != null ? context.namespace + "::" + javaName : javaName;
                String annotations = "";
                if (!fullName.equals(cppName)){
                    annotations += "@Name(\"" + cppName + "\") ";
                } else if (context.namespace != null && context.javaName == null) {
                    annotations += "@Namespace(\"" + context.namespace + "\") ";
                }
                decl.text += enumSpacing + annotations + "public enum " + javaName + " {"
                          +  enumerators2 + token.expect(';').spacing + ";"
                          + (comment.length() > 0 && comment.charAt(0) == ' ' ? comment.substring(1) : comment) + "\n\n"
                          +  enumSpacing2 + "    public final " + javaType + " value;\n"
                          +  enumSpacing2 + "    private " + javaName  + "(" + javaType + " v) { this.value = v; }\n"
                          +  enumSpacing2 + "    private " + javaName  + "(" + javaName + " e) { this.value = e.value; }\n"
                          +  enumSpacing2 + "    public " + javaName + " intern() { for (" + javaName + " e : values()) if (e.value == value) return e; return this; }\n"
                          +  enumSpacing2 + "    @Override public String toString() { return intern().name(); }\n"
                          +  enumSpacing2 + "}";
                Info info2 = new Info(infoMap.getFirst(cppType)).cppNames(cppName);
                info2.valueTypes = Arrays.copyOf(info2.valueTypes, info2.valueTypes.length + 1);
                for (int i = 1; i < info2.valueTypes.length; i++) {
                    info2.valueTypes[i] = "@Cast(\"" + cppName + "\") " + info2.valueTypes[i - 1];
                }
                info2.valueTypes[0] = javaName;
                info2.pointerTypes = Arrays.copyOf(info2.pointerTypes, info2.pointerTypes.length);
                for (int i = 0; i < info2.pointerTypes.length; i++) {
                    info2.pointerTypes[i] = "@Cast(\"" + cppName + "*\") " + info2.pointerTypes[i];
                }
                infoMap.put(info2.cast(false).enumerate());
            } else {
                decl.text += enumSpacing + "/** " + enumType + " " + cppName + " */\n"
                          +  enumSpacing2 + enumerators + token.expect(';').spacing + ";";
                if (cppName.length() > 0) {
                    Info info2 = infoMap.getFirst(cppType);
                    infoMap.put(new Info(info2).cast().cppNames(cppName));
                }
                decl.text += extraText + comment;
            }
        }
        declList.add(decl);
        tokens.next();
        return true;
    }

    boolean namespace(Context context, DeclarationList declList) throws ParserException {
        if (!tokens.get().match(Token.NAMESPACE)) {
            return false;
        }
        Declaration decl = new Declaration();
        String spacing = tokens.get().spacing;
        String name = null;
        tokens.next();
        if (tokens.get().match(Token.IDENTIFIER)) {
            // get the name, unless anonymous
            name = tokens.get().value;
            tokens.next();
        }
        if (tokens.get().match('=')) {
            // deal with namespace aliases
            tokens.next();
            Type type = type(context);
            context.namespaceMap.put(name, type.cppName);
            tokens.get().expect(';');
            tokens.next();
            return true;
        }
        tokens.get().expect('{');
        tokens.next();
        if (tokens.get().spacing.indexOf('\n') < 0) {
            tokens.get().spacing = spacing;
        }

        context = new Context(context);
        context.namespace = name == null ? context.namespace : context.namespace != null ? context.namespace + "::" + name : name;
        declarations(context, declList);
        decl.text += tokens.get().expect('}').spacing;
        tokens.next();
        declList.add(decl);
        return true;
    }

    boolean extern(Context context, DeclarationList declList) throws ParserException {
        if (!tokens.get().match(Token.EXTERN) || !tokens.get(1).match(Token.STRING)) {
            return false;
        }
        String spacing = tokens.get().spacing;
        Declaration decl = new Declaration();
        tokens.next().expect("\"C\"", "\"C++\"");
        if (!tokens.next().match('{')) {
            tokens.get().spacing = spacing;
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

    void declarations(Context context, DeclarationList declList) throws ParserException {
        for (Token token = tokens.get(); !token.match(Token.EOF, '}'); token = tokens.get()) {
            if (token.match(Token.PRIVATE, Token.PROTECTED, Token.PUBLIC) && tokens.get(1).match(':')) {
                context.inaccessible = !token.match(Token.PUBLIC);
                tokens.next();
                tokens.next();
                continue;
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
                decl.comment = true;
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
                    for (Map.Entry<String,Type> e : map.entrySet()) {
                        if (count < type.arguments.length) {
                            Type t = type.arguments[count++];
                            String s = t.cppName;
                            if (t.constValue && !s.startsWith("const ")) {
                                s = "const " + s;
                            }
                            if (t.constPointer && !s.endsWith(" const")) {
                                s = s + " const";
                            }
                            if (t.indirections > 0) {
                                for (int i = 0; i < t.indirections; i++) {
                                    s += "*";
                                }
                            }
                            if (t.reference) {
                                s += "&";
                            }
                            t.cppName = s;
                            e.setValue(t);
                        }
                    }
                    tokens.index = startIndex;
                }

                if (!tokens.get().match(';')
                        && !macro(ctx, declList) && !extern(ctx, declList) && !namespace(ctx, declList)
                        && !enumeration(ctx, declList) && !group(ctx, declList) && !typedef(ctx, declList)
                        && !using(ctx, declList) && !function(ctx, declList) && !variable(ctx, declList)) {
                    spacing = tokens.get().spacing;
                    if (attribute() != null) {
                        tokens.get().spacing = spacing;
                    } else {
                        throw new ParserException(token.file + ":" + token.lineNumber + ":"
                                + (token.text != null ? "\"" + token.text + "\": " : "")
                                + "Could not parse declaration at '" + token + "'");
                    }
                }
                while (tokens.get().match(';') && !tokens.get().match(Token.EOF)) {
                    tokens.next();
                }
            } while (declList.infoIterator != null && declList.infoIterator.hasNext());
        }

        // for comments at the end without declarations
        String comment = commentBefore() + (tokens.get().match(Token.EOF) ? tokens.get().spacing : "");
        Declaration decl = new Declaration();
        if (comment != null && comment.length() > 0) {
            decl.text = comment;
            decl.comment = true;
            declList.add(decl);
        }
    }

    void parse(Context context,
               DeclarationList declList,
               String[] includePath,
               String include,
               boolean isCFile) throws IOException, ParserException {
        List<Token> tokenList = new ArrayList<Token>();
        File file = null;
        String filename = include;
        if (filename.startsWith("<") && filename.endsWith(">")) {
            filename = filename.substring(1, filename.length() - 1);
        } else {
            File f = new File(filename);
            if (f.exists()) {
                file = f;
            }
        }
        if (file == null && includePath != null) {
            for (String path : includePath) {
                File f = new File(path, filename).getCanonicalFile();
                if (f.exists()) {
                    file = f;
                    break;
                }
            }
        }
        if (file == null) {
            file = new File(filename);
        }
        Info info = infoMap.getFirst(file.getName());
        if (info != null && info.skip && info.linePatterns == null) {
            return;
        } else if (!file.exists()) {
            throw new FileNotFoundException("Could not parse \"" + file + "\": File does not exist");
        }
        logger.info("Parsing " + file);
        Token token = new Token();
        token.type = Token.COMMENT;
        token.value = "\n// Parsed from " + include + "\n\n";
        tokenList.add(token);
        Tokenizer tokenizer = new Tokenizer(file, encoding);
        if (info != null && info.linePatterns != null) {
            tokenizer.filterLines(info.linePatterns, info.skip);
        }
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
        token = new Token(Token.EOF);
        token.spacing = "\n";
        token.file = file;
        token.lineNumber = tokenList.get(tokenList.size() - 1).lineNumber;
        tokenList.add(token);
        tokens = new TokenIndexer(infoMap, tokenList.toArray(new Token[tokenList.size()]), isCFile);
        declarations(context, declList);
    }

    public File parse(String outputDirectory, String[] classPath, Class cls) throws IOException, ParserException {
        return parse(new File(outputDirectory), classPath, cls);
    }
    public File parse(File outputDirectory, String[] classPath, Class cls) throws IOException, ParserException {
        ClassProperties allProperties = Loader.loadProperties(cls, properties, true);
        ClassProperties clsProperties = Loader.loadProperties(cls, properties, false);

        // Capture c-includes from "class" and "all" properties
        List<String> cIncludes = new ArrayList<>();
        cIncludes.addAll(clsProperties.get("platform.cinclude"));
        cIncludes.addAll(allProperties.get("platform.cinclude"));

        // Capture class includes
        List<String> clsIncludes = new ArrayList<String>();
        clsIncludes.addAll(clsProperties.get("platform.include"));
        clsIncludes.addAll(clsProperties.get("platform.cinclude"));

        // Capture all includes
        List<String> allIncludes = new ArrayList<String>();
        allIncludes.addAll(allProperties.get("platform.include"));
        allIncludes.addAll(allProperties.get("platform.cinclude"));
        List<String> allTargets = allProperties.get("target");
        List<String> allGlobals = allProperties.get("global");
        List<String> clsTargets = clsProperties.get("target");
        List<String> clsGlobals = clsProperties.get("global");
        List<String> clsHelpers = clsProperties.get("helper");
        // There can only be one target, pick the last one set
        String target = clsTargets.get(clsTargets.size() - 1);
        String global = clsGlobals.get(clsGlobals.size() - 1);
        List<Class> allInherited = allProperties.getInheritedClasses();

        infoMap = new InfoMap();
        for (Class c : allInherited) {
            try {
                InfoMapper infoMapper = ((InfoMapper)c.newInstance());
                if (infoMapper instanceof BuildEnabled) {
                    ((BuildEnabled)infoMapper).init(logger, properties, encoding);
                }
                infoMapper.map(infoMap);
            } catch (ClassCastException |  InstantiationException | IllegalAccessException e) {
                // fail silently as if the interface wasn't implemented
            }
        }
        leafInfoMap = new InfoMap();
        try {
            InfoMapper infoMapper = ((InfoMapper)cls.newInstance());
            if (infoMapper instanceof BuildEnabled) {
                ((BuildEnabled)infoMapper).init(logger, properties, encoding);
            }
            infoMapper.map(leafInfoMap);
        } catch (ClassCastException |  InstantiationException | IllegalAccessException e) {
            // fail silently as if the interface wasn't implemented
        }
        infoMap.putAll(leafInfoMap);

        String version = Parser.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        String text = "// Targeted by JavaCPP version " + version + ": DO NOT EDIT THIS FILE\n\n";
        String targetPackage = "";
        int n = global.lastIndexOf('.');
        if (n >= 0) {
            targetPackage = global.substring(0, n);
            text += "package " + targetPackage + ";\n\n";
        }
        List<Info> infoList = leafInfoMap.get(null);
        for (Info info : infoList) {
            if (info.javaText != null && info.javaText.startsWith("import")) {
                text += info.javaText + "\n";
            }
        }
        text += "import java.nio.*;\n"
             +  "import org.bytedeco.javacpp.*;\n"
             +  "import org.bytedeco.javacpp.annotation.*;\n\n";
        for (int i = 0; i < allTargets.size(); i++) {
            if (!target.equals(allTargets.get(i))) {
                if (allTargets.get(i).equals(allGlobals.get(i))) {
                    text += "import static " + allTargets.get(i) + ".*;\n";
                } else {
                    text += "import " + allTargets.get(i) + ".*;\n"
                         +  "import static " + allGlobals.get(i) + ".*;\n";
                }
            }
        }
        if (allTargets.size() > 1) {
            text += "\n";
        }
        String globalText = text + "public class " + global.substring(n + 1) + " extends "
             + (clsHelpers.size() > 0 && clsIncludes.size() > 0 ? clsHelpers.get(0) : cls.getCanonicalName()) + " {\n"
             + "    static { Loader.load(); }\n";

        String targetPath = global.replace('.', File.separatorChar);
        File targetFile = new File(outputDirectory, targetPath + ".java");
        logger.info("Targeting " + targetFile);
        Context context = new Context();
        context.infoMap = infoMap;
        String[] includePath = classPath;
        n = targetPath.lastIndexOf(File.separatorChar);
        if (n >= 0) {
            includePath = classPath.clone();
            for (int i = 0; i < includePath.length; i++) {
                includePath[i] += File.separator + targetPath.substring(0, n);
            }
        }
        List<String> paths = allProperties.get("platform.includepath");
        for (String s : allProperties.get("platform.includeresource")) {
            for (File f : Loader.cacheResources(s)) {
                paths.add(f.getCanonicalPath());
            }
        }

        if (clsIncludes.size() == 0) {
            logger.info("Nothing targeted for " + targetFile);
            return null;
        }

        String[] includePaths = paths.toArray(new String[paths.size() + includePath.length]);
        System.arraycopy(includePath, 0, includePaths, paths.size(), includePath.length);
        DeclarationList declList = new DeclarationList();
        for (String include : allIncludes) {
            if (!clsIncludes.contains(include)) {
                boolean isCFile = cIncludes.contains(include);
                parse(context, declList, includePaths, include, isCFile);
            }
        }
        declList = new DeclarationList(declList);
        if (clsIncludes.size() > 0) {
            containers(context, declList);
            for (String include : clsIncludes) {
                if (allIncludes.contains(include)) {
                    boolean isCFile = cIncludes.contains(include);
                    parse(context, declList, includePaths, include, isCFile);
                }
            }
        }

        if (declList.size() == 0) {
            logger.info("Nothing targeted for " + targetFile);
            return null;
        }

        File targetDir = targetFile.getParentFile();
        if (targetDir != null) {
            targetDir.mkdirs();
        }
        try (Writer out = encoding != null ? new EncodingFileWriter(targetFile, encoding, lineSeparator)
                                           : new EncodingFileWriter(targetFile, lineSeparator)) {
            out.append(globalText);
            for (Info info : infoList) {
                if (info.javaText != null && !info.javaText.startsWith("import")) {
                    out.append(info.javaText + "\n");
                }
            }
            Declaration prevd = null;
            for (Declaration d : declList) {
                if (!target.equals(global) && d.type != null && d.type.javaName != null && d.type.javaName.length() > 0) {
                    // if the user gave us a class name for "global", we're targeting a package, so output global classes into their own files
                    File javaFile = new File(targetDir, d.type.javaName + ".java");
                    if (prevd != null && !prevd.comment) {
                        out.append(prevd.text);
                    }
                    out.append("\n// Targeting " + d.type.javaName + ".java" + "\n\n");
                    logger.info("Targeting " + javaFile);
                    String javaText = text + "import static " + global + ".*;\n"
                            + (prevd != null && prevd.comment ? prevd.text : "")
                            + d.text.replace("public static class " + d.type.javaName + " ",
                                    "@Properties(inherit = " + cls.getCanonicalName() + ".class)\n"
                                  + "public class " + d.type.javaName + " ");
                    Files.write(javaFile.toPath(), encoding != null ? javaText.getBytes(encoding) : javaText.getBytes());
                    prevd = null;
                } else {
                    if (prevd != null) {
                        out.append(prevd.text);
                    }
                    prevd = d;
                }
            }
            if (prevd != null) {
                out.append(prevd.text);
            }
            out.append("\n}\n").close();
        }

        return targetFile;
    }
}
