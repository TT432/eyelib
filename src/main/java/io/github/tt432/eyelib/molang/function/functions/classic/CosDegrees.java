package io.github.tt432.eyelib.molang.function.functions.classic;


import io.github.tt432.eyelib.molang.function.MolangFunction;
import io.github.tt432.eyelib.molang.function.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.util.Mth;

@MolangFunctionHolder("math.cos")
public class CosDegrees extends MolangFunction {
    @Override
    public float invoke(MolangFunctionParameters params) {
        return Mth.cos(params.value(0) * EyeMath.DEGREES_TO_RADIANS);
    }
}
