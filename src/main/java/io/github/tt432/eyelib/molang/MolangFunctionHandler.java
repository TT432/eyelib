package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.function.MolangFunctionParameters;

import java.util.Arrays;

/**
 * @author TT432
 */
public class MolangFunctionHandler {
    public static float tryExecuteFunction(String name, MolangScope scope, Object[] params) {
        if (GlobalMolangFunction.contains(name)) {
            return GlobalMolangFunction.get(name)
                    .invoke(MolangFunctionParameters.upload(scope, Arrays.stream(params).toList()));
        }

        return 0F;
    }
}
