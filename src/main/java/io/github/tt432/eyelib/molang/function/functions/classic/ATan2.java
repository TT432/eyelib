package io.github.tt432.eyelib.molang.function.functions.classic;

import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import net.minecraft.util.Mth;

@MolangFunctionHolder("math.atan2")
public class ATan2 extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return (float) Mth.atan2(params.value(0), params.value(1));
    }
}