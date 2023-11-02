package io.github.tt432.eyelib.molang.function.functions.limit;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import io.github.tt432.eyelib.util.math.MathE;

@MolangFunctionHolder("math.clamp")
public class Clamp extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return MathE.clamp(params.value(0), params.value(1), params.value(2));
    }
}