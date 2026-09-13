/*
 * Copyright (C) 2026 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0 (the "License"),
 * or (at your option) under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2, or
 * (at your option) any later version (subject to the "Classpath" exception),
 * in which case the provisions of the GPL apply. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it here.
 */
package org.bytedeco.javacpp.tools;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Regression coverage for generated resizable-container helpers. */
public class ParserContainerTest {

    @Test public void testMoveOnlyContainerPopBackGeneration() throws Exception {
        Parser parser = new Parser(new Logger(), new java.util.Properties());
        parser.infoMap = new InfoMap();
        parser.leafInfoMap = new InfoMap();
        Info vectorInfo = new Info("std::vector<MoveOnlyData>")
                .pointerTypes("MoveOnlyDataVector")
                .define();
        parser.infoMap.put(vectorInfo);
        parser.leafInfoMap
                .put(new Info("MoveOnlyData").pointerTypes("MoveOnlyData"))
                .put(vectorInfo);

        Context context = new Context();
        context.infoMap = parser.infoMap;
        DeclarationList declarations = new DeclarationList();
        parser.containers(context, declarations);

        String generated = declarations.stream()
                .map(declaration -> declaration.text)
                .filter(java.util.Objects::nonNull)
                .reduce("", String::concat);
        assertTrue(generated.contains("@Name(\"at\") @Index(function = \"at\")"));
        assertTrue(generated.contains("@ByRef(true)") && generated.contains("getByMove"));
        assertTrue(generated.contains("@Name(\"pop_back\") private native void popBackNative()"));
        assertTrue(generated.contains("MoveOnlyData value = getByMove(size - 1)"));
    }
}
