package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author DustW
 */
@MolangFunctionHolder("eyelib.remove_glow")
public class RemoveGlow extends MolangFunction {
    public RemoveGlow(MolangValue[] values, String name) throws IllegalArgumentException {
        super(values, name, -1);
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        if (args.length > 0) {
            AddGlow.getGlowing().putAll(Arrays.stream(args).map(v -> v.asString(scope)).collect(Collectors.toMap(s -> s, s -> false)));
        } else {
            String currBone = MolangParser.getCurrentDataSource().getData().getExtraData("anim.current_bone", "");
            AddGlow.getGlowing().put(currBone, false);
        }

        return 0;
    }
}
