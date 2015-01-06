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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A {@link Map} containing {@link Info} objects consumed by the {@link Parser}.
 * Also contains a few utility methods to facilitate its use for both the user
 * and the {@link Parser}.
 *
 * @author Samuel Audet
 */
public class InfoMap extends HashMap<String,LinkedList<Info>> {
    public InfoMap() { this.parent = defaults; }
    public InfoMap(InfoMap parent) { this.parent = parent; }

    InfoMap parent = null;
    static final String[] containers = { "std::deque", "std::list", "std::map", "std::queue", "std::set", "std::stack", "std::vector", "std::valarray" };
    static final String[] simpleTypes = { "signed", "unsigned", "char", "short", "int", "long", "bool", "float", "double" };
    static { Arrays.sort(simpleTypes); }
    static final InfoMap defaults = new InfoMap(null)
        .put(new Info(" __attribute__", "__declspec").annotations().skip())
        .put(new Info("void").valueTypes("void").pointerTypes("Pointer"))
        .put(new Info("va_list", "FILE", "std::exception", "std::istream", "std::ostream", "std::iostream",
                "std::ifstream", "std::ofstream", "std::fstream").cast().pointerTypes("Pointer"))

        .put(new Info("int8_t", "__int8", "jbyte", "signed char")
            .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))
        .put(new Info("uint8_t", "unsigned __int8", "char", "unsigned char").cast()
            .valueTypes("byte").pointerTypes("BytePointer", "ByteBuffer", "byte[]"))

        .put(new Info("int16_t", "__int16", "jshort", "short", "signed short", "short int", "signed short int")
            .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))
        .put(new Info("uint16_t", "unsigned __int16", "unsigned short", "unsigned short int").cast()
            .valueTypes("short").pointerTypes("ShortPointer", "ShortBuffer", "short[]"))

        .put(new Info("int32_t", "__int32", "jint", "int", "signed int", "signed")
            .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))
        .put(new Info("uint32_t", "unsigned __int32", "unsigned int", "unsigned").cast()
            .valueTypes("int").pointerTypes("IntPointer", "IntBuffer", "int[]"))

        .put(new Info("int64_t", "__int64", "jlong", "long long", "signed long long", "long long int", "signed long long int")
            .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))
        .put(new Info("uint64_t", "unsigned __int64", "unsigned long long", "unsigned long long int").cast()
            .valueTypes("long").pointerTypes("LongPointer", "LongBuffer", "long[]"))

        .put(new Info("long", "signed long", "long int", "signed long int")
            .valueTypes("long").pointerTypes("CLongPointer"))
        .put(new Info("unsigned long", "unsigned long int").cast()
            .valueTypes("long").pointerTypes("CLongPointer"))

        .put(new Info("size_t", "ptrdiff_t", "intptr_t", "uintptr_t", "off_t").cast().valueTypes("long").pointerTypes("SizeTPointer"))
        .put(new Info("float", "jfloat").valueTypes("float").pointerTypes("FloatPointer", "FloatBuffer", "float[]"))
        .put(new Info("double", "jdouble").valueTypes("double").pointerTypes("DoublePointer", "DoubleBuffer", "double[]"))
        .put(new Info("long double").cast().valueTypes("double").pointerTypes("Pointer"))
        .put(new Info("std::complex<float>").cast().pointerTypes("FloatPointer", "FloatBuffer", "float[]"))
        .put(new Info("std::complex<double>").cast().pointerTypes("DoublePointer", "DoubleBuffer", "double[]"))
        .put(new Info("bool", "jboolean").cast().valueTypes("boolean").pointerTypes("BoolPointer"))
        .put(new Info("wchar_t", "WCHAR").cast().valueTypes("char").pointerTypes("CharPointer"))
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
        .put(new Info("address").javaNames("_address"))
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
