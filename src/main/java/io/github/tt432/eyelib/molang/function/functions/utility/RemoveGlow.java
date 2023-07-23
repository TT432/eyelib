package io.github.tt432.eyelib.molang.function.functions.utility;

import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

import java.util.stream.Collectors;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.remove_glow")
public class RemoveGlow extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        if (params.size() > 0) {
            AddGlow.getGlowing(params).putAll(params.svalues().collect(Collectors.toMap(s -> s, s -> false)));
        } else {
            AddGlow.getGlowing(params).put(params.scope().getExtraData("anim.current_bone", ""), false);
        }

        return 0;
    }
}
