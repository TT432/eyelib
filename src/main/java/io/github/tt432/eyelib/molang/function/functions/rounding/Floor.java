package io.github.tt432.eyelib.molang.function.functions.rounding;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.floor")
public class Floor extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return (float) Math.floor(params.value(0));
    }
}