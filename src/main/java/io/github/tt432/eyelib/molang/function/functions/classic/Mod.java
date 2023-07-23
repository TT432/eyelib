package io.github.tt432.eyelib.molang.function.functions.classic;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.mod")
public class Mod extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return params.value(0) % params.value(1);
    }
}