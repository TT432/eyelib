package io.github.tt432.eyelibmolang.compiler.frontend;

public final class MolangParserFrontends {
    private static final MolangParserFrontend ACTIVE = GeneratedParserBackedMolangParserFrontend.INSTANCE;

    private MolangParserFrontends() {
    }

    public static MolangParserFrontend active() {
        return ACTIVE;
    }
}
