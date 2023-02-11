package io.github.tt432.eyelib.molang.math.functions.utility;

import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.math.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.add_glowing")
public class AddGlowing extends MolangFunction {
    public AddGlowing(MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        getGlowing().addAll(Arrays.stream(args).map(v -> v.asString(scope)).toList());
        return 0;
    }

    public static List<String> getGlowing() {
        AnimationData data = MolangParser.getInstance().source.getData();
        return (List<String>) data.getExtraData().computeIfAbsent("glowing", s -> new ArrayList<>());
    }
}
