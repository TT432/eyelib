package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.die_roll")
public class DieRoll extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return (float) (((int) params.value(0))
                * (Math.random() * params.value(2) - params.value(2)));
    }
}