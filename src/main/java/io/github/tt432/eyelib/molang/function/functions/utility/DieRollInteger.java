package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.die_roll_integer")
public class DieRollInteger extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return ((int) params.value(0))
                * Math.round(params.value(1) + Math.random() * (params.value(2) - params.value(1)));
    }
}