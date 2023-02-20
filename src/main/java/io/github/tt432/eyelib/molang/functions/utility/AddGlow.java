package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.add_glow")
public class AddGlow extends MolangFunction {
    public AddGlow(MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        if (args.length > 0) {
            getGlowing().putAll(Arrays.stream(args).map(v -> v.asString(scope)).collect(Collectors.toMap(s -> s, s -> true)));
        } else {
            getGlowing().put(MolangParser.getCurrentDataSource().getData().getExtraData("anim.current_bone", ""), true);
        }

        return 0;
    }

    public static Map<String, Boolean> getGlowing() {
        AnimationData data = MolangParser.getCurrentDataSource().getData();
        return data.getOrCreateExtraData("glowing", new HashMap<>());
    }
}
