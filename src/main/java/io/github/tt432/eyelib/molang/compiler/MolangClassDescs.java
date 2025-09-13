package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;

import java.lang.constant.ClassDesc;

/**
 * @author TT432
 */
public final class MolangClassDescs {
    public static ClassDesc CD_MolangObject = ClassDesc.of(MolangObject.class.getName());
    public static ClassDesc CD_MolangScope = ClassDesc.of(MolangScope.class.getName());

    private MolangClassDescs() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
