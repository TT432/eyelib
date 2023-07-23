package io.github.tt432.eyelib.molang.function.functions.classic;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.sqrt")
public class Sqrt extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return (float) Math.sqrt(params.value(0));
    }
}