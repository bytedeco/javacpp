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

import java.io.File;

/**
 *
 * @author Samuel Audet
 */
class Token implements Comparable<Token> {
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

    Token expect(Object ... tokens) throws ParserException {
        if (!match(tokens)) {
            throw new ParserException(file + ":" + lineNumber + ": Unexpected token '" + toString() + "'");
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
