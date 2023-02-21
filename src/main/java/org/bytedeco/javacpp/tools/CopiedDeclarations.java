package org.bytedeco.javacpp.tools;

import java.util.ArrayList;

class CopiedDeclarations extends ArrayList<CopiedDeclarations.CopiedDeclaration> {
    void add(Declaration decl, String fullname, String modifiers, Context context) {
        add(new CopiedDeclaration(decl, fullname, modifiers, context.namespace, context.templateMap));
    }

    static class CopiedDeclaration {
        final Declaration decl;
        final String fullname;
        final String modifiers;
        final String namespace;
        final TemplateMap templateMap;

        CopiedDeclaration(Declaration decl, String fullname, String modifiers, String namespace, TemplateMap templateMap) {
            this.decl = decl;
            this.fullname = fullname;
            this.modifiers = modifiers;
            this.namespace = namespace;
            this.templateMap = templateMap;
        }
    }
}
