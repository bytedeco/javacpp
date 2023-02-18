package org.bytedeco.javacpp.tools;

import java.util.ArrayList;

class CopiedDeclarations extends ArrayList<CopiedDeclarations.CopiedDeclaration> {
    void add(Declaration decl, String fullname, String modifiers, String namespace) {
        add(new CopiedDeclaration(decl, fullname, modifiers, namespace));
    }

    static class CopiedDeclaration {
        final Declaration decl;
        final String fullname;
        final String modifiers;
        final String namespace;

        CopiedDeclaration(Declaration decl, String fullname, String modifiers, String namespace) {
            this.decl = decl;
            this.fullname = fullname;
            this.modifiers = modifiers;
            this.namespace = namespace;
        }
    }
}
