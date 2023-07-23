package io.github.tt432.eyelib.molang.function.functions.utility;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import io.github.tt432.eyelib.util.math.MathE;

@MolangFunctionHolder("math.lerprotate")
public class LerpRotate extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return MathE.lerpYaw(params.value(0), params.value(1), params.value(2));
    }
}