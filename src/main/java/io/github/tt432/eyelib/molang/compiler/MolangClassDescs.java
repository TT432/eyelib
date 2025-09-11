package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import lombok.experimental.UtilityClass;

import java.lang.constant.ClassDesc;

/**
 * @author TT432
 */
@UtilityClass
public class MolangClassDescs {
    public static final ClassDesc CD_MolangObject = ClassDesc.of(MolangObject.class.getName());
    public static final ClassDesc CD_MolangScope =ClassDesc.of(MolangScope .class.getName());
}
