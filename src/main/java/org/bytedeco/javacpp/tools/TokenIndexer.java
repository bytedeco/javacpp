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

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Samuel Audet
 */
class TokenIndexer {
    TokenIndexer(InfoMap infoMap, Token[] array) {
        this.infoMap = infoMap;
        this.array = array;
    }

    /** Set to true to disable temporarily the preprocessor. */
    boolean raw = false;
    /** The set of {@link Info} objects to use during preprocessing. */
    InfoMap infoMap = null;
    /** The token of array, modified by the preprocessor as we go. */
    Token[] array = null;
    /** The current index, in the array of tokens. Used by {@link #get(int)} and {@link #next()}. */
    int index = 0;

    void filter(int index) {
        if (index + 1 < array.length && array[index].match('#') &&
                array[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
            // copy the array of tokens up to this point
            ArrayList<Token> tokens = new ArrayList<Token>();
            for (int i = 0; i < index; i++) {
                tokens.add(array[i]);
            }
            int count = 0;
            Info info = null;
            boolean define = true, defined = false;
            while (index < array.length) {
                String spacing = array[index].spacing;
                int n = spacing.lastIndexOf('\n') + 1;
                Token keyword = null;
                // pick up #if, #ifdef, #ifndef, #elif, #else, and #endif directives
                if (array[index].match('#')) {
                    if (array[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF)) {
                        count++;
                    }
                    if (count == 1 && array[index + 1].match(Token.IF, Token.IFDEF, Token.IFNDEF, Token.ELIF, Token.ELSE, Token.ENDIF)) {
                        keyword = array[index + 1];
                    }
                    if (array[index + 1].match(Token.ENDIF)) {
                        count--;
                    }
                }
                // conditionally fill up the new array of tokens
                if (keyword != null) {
                    index += 2;

                    // keep the directive as comment
                    Token comment = new Token();
                    comment.type = Token.COMMENT;
                    comment.spacing = spacing.substring(0, n);
                    comment.value = "// " + spacing.substring(n) + "#" + keyword.spacing + keyword;
                    tokens.add(comment);

                    if (keyword.match(Token.IF, Token.IFDEF, Token.IFNDEF, Token.ELIF)) {
                        String value = "";
                        for ( ; index < array.length; index++) {
                            if (array[index].spacing.indexOf('\n') >= 0) {
                                break;
                            }
                            if (!array[index].match(Token.COMMENT)) {
                                value += array[index].spacing + array[index];
                            }
                            comment.value += array[index].match("\n") ? "\n// " : array[index].spacing + array[index];
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
            // copy the rest of the tokens from this point on
            for ( ; index < array.length; index++) {
                tokens.add(array[index]);
            }
            array = tokens.toArray(new Token[tokens.size()]);
        }
    }

    void expand(int index) {
        if (index < array.length && infoMap.containsKey(array[index].value)) {
            // if we hit a token whose info.cppText starts with #define (a macro), expand it
            int startIndex = index;
            Info info = infoMap.getFirst(array[index].value);
            if (info != null && info.cppText != null) {
                try {
                    Tokenizer tokenizer = new Tokenizer(info.cppText);
                    if (!tokenizer.nextToken().match('#')
                            || !tokenizer.nextToken().match(Token.DEFINE)
                            || !tokenizer.nextToken().match(info.cppNames[0])) {
                        return;
                    }
                    // copy the array of tokens up to this point
                    ArrayList<Token> tokens = new ArrayList<Token>();
                    for (int i = 0; i < index; i++) {
                        tokens.add(array[i]);
                    }
                    ArrayList<String> params = new ArrayList<String>();
                    ArrayList<Token>[] args = null;
                    Token token = tokenizer.nextToken();
                    // pick up the parameters and arguments of the macro if it has any
                    String name = array[index].value;
                    if (token.match('(')) {
                        token = tokenizer.nextToken();
                        while (!token.isEmpty()) {
                            if (token.match(Token.IDENTIFIER)) {
                                params.add(token.value);
                            } else if (token.match(')')) {
                                token = tokenizer.nextToken();
                                break;
                            }
                            token = tokenizer.nextToken();
                        }
                        index++;
                        if (params.size() > 0 && (index >= array.length || !array[index].match('('))) {
                            return;
                        }
                        name += array[index].spacing + array[index];
                        args = new ArrayList[params.size()];
                        int count = 0, count2 = 0;
                        for (index++; index < array.length; index++) {
                            Token token2 = array[index];
                            name += token2.spacing + token2;
                            if (count2 == 0 && token2.match(')')) {
                                break;
                            } else if (count2 == 0 && token2.match(',')) {
                                count++;
                                continue;
                            } else if (token2.match('(','[','{')) {
                                count2++;
                            } else if (token2.match(')',']','}')) {
                                count2--;
                            }
                            if (count < args.length) {
                                if (args[count] == null) {
                                    args[count] = new ArrayList<Token>();
                                }
                                args[count].add(token2);
                            }
                        }
                    }
                    int startToken = tokens.size();
                    // expand the token in question, unless we should skip it
                    info = infoMap.getFirst(name);
                    while ((info == null || !info.skip) && !token.isEmpty()) {
                        boolean foundArg = false;
                        for (int i = 0; i < params.size(); i++) {
                            if (params.get(i).equals(token.value)) {
                                tokens.addAll(args[i]);
                                foundArg = true;
                                break;
                            }
                        }
                        if (!foundArg) {
                            tokens.add(token);
                        }
                        token = tokenizer.nextToken();
                    }
                    // concatenate tokens as required
                    for (int i = startToken; i < tokens.size(); i++) {
                        if (tokens.get(i).match("##") && i > 0 && i + 1 < tokens.size()) {
                            tokens.get(i - 1).value += tokens.get(i + 1).value;
                            tokens.remove(i);
                            tokens.remove(i);
                        }
                    }
                    // copy the rest of the tokens from this point on
                    for (index++; index < array.length; index++) {
                        tokens.add(array[index]);
                    }
                    if ((info == null || !info.skip) && startToken < tokens.size()) {
                        tokens.get(startToken).spacing = array[startIndex].spacing;
                    }
                    array = tokens.toArray(new Token[tokens.size()]);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    int preprocess(int index, int count) {
        for ( ; index < array.length; index++) {
            filter(index); // conditionals
            expand(index); // macros
            // skip comments
            if (!array[index].match(Token.COMMENT) && --count < 0) {
                break;
            }
        }
        filter(index); // conditionals
        expand(index); // macros
        return index;
    }

    /** Returns {@code get(0)}. */
    Token get() {
        return get(0);
    }
    /** Returns {@code array[index + i]}. After preprocessing if {@code raw == false}. */
    Token get(int i) {
        int k = raw ? index + i : preprocess(index, i);
        return k < array.length ? array[k] : Token.EOF;
    }
    /** Increments {@code index} and returns {@code array[index]}. After preprocessing if {@code raw == false}. */
    Token next() {
        index = raw ? index + 1 : preprocess(index, 1);
        return index < array.length ? array[index] : Token.EOF;
    }
}
