package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.add_glow")
public class AddGlow extends MolangFunction {
    public static Map<String, Boolean> getGlowing(MolangFunctionParameters params) {
        return params.scope().getExtraData("glowing", new HashMap<>());
    }

    @Override
    public float invoke(MolangFunctionParameters params) {
        if (params.size() > 0) {
            getGlowing(params).putAll(params.svalues().collect(Collectors.toMap(s -> s, s -> true)));
        } else {
            getGlowing(params).put(params.scope().getExtraData("anim.current_bone", ""), true);
        }

        return 0;
    }
}
