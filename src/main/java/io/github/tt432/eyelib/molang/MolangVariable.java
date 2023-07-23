package io.github.tt432.eyelib.molang;

import java.util.function.Function;

/**
 * @author TT432
 */
public class MolangVariable {
    boolean constant;
    float value;
    Function<MolangScope, Float> function;

    public MolangVariable(Function<MolangScope, Float> function) {
        this.function = function;
        constant = false;
    }

    public MolangVariable(float value) {
        this.value = value;
        constant = true;
    }

    public float get(MolangScope scope) {
        if (constant) {
            return value;
        } else {
            return function.apply(scope);
        }
    }
}
