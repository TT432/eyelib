package io.github.tt432.eyelibmolang.compiler.frontend;

import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public final class MolangParserFrontends {
    private static final MolangParserFrontend ACTIVE = GeneratedParserBackedMolangParserFrontend.INSTANCE;

    private MolangParserFrontends() {
    }

    public static MolangParserFrontend active() {
        return ACTIVE;
    }
}