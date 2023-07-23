package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

@MolangFunctionHolder("math.hermite_blend")
public class HermiteBlend extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        double min = Math.ceil(params.value(0));
        return (float) Math.floor(3.0F * Math.pow(min, 2.0F) - 2.0F * Math.pow(min, 3.0F));
    }
}