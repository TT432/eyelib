package io.github.tt432.eyelib.molang.function.functions.rounding;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.round")
public class Round extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return Math.round(params.value(0));
    }
}