package io.github.tt432.eyelib.molang.function.functions.rounding;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.trunc")
public class Trunc extends MolangFunction {

    @Override
    public float invoke(MolangFunctionParameters params) {
        double value = params.value(0);

        return (float) ((value < 0) ? Math.ceil(value) : Math.floor(value));
    }
}