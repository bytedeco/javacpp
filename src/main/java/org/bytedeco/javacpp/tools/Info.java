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

/**
 *
 * @author Samuel Audet
 */
public class Info {
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
    public Info cast() { this.cast = true; return this;  }
    public Info cast(boolean cast) { this.cast = cast; return this;  }
    public Info define() { this.define = true; return this; }
    public Info define(boolean define) { this.define = define; return this; }
    public Info translate() { this.translate = true; return this; }
    public Info translate(boolean translate) { this.translate = translate; return this; }
    public Info skip() { this.skip = true; return this; }
    public Info skip(boolean skip) { this.skip = skip; return this; }
    public Info base(String base) { this.base = base; return this; }
    public Info cppText(String cppText) { this.cppText = cppText; return this; }
    public Info javaText(String javaText) { this.javaText = javaText; return this; }
}
