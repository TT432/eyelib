package io.github.tt432.eyelibmolang.compiler;

import java.util.ArrayList;
import java.util.List;

public record MolangExpressionAnalysis(
        boolean compileTimeEvaluable,
        boolean runtimeEnumerable,
        boolean sideEffectFree,
        List<String> blockers
) {
    public MolangExpressionAnalysis {
        blockers = List.copyOf(blockers);
        if (compileTimeEvaluable) {
            runtimeEnumerable = true;
            sideEffectFree = true;
        }
    }

    public static MolangExpressionAnalysis constant() {
        return new MolangExpressionAnalysis(true, true, true, List.of());
    }

    public static MolangExpressionAnalysis enumerable() {
        return new MolangExpressionAnalysis(false, true, true, List.of());
    }

    public static MolangExpressionAnalysis dynamic(String blocker) {
        return new MolangExpressionAnalysis(false, false, true, List.of(blocker));
    }

    public static MolangExpressionAnalysis impure(String blocker) {
        return new MolangExpressionAnalysis(false, false, false, List.of(blocker));
    }

    public MolangExpressionAnalysis withCompileTimeEvaluable(boolean compileTimeEvaluable) {
        return new MolangExpressionAnalysis(compileTimeEvaluable, runtimeEnumerable, sideEffectFree, blockers);
    }

    public MolangExpressionAnalysis addBlocker(String blocker) {
        ArrayList<String> updatedBlockers = new ArrayList<>(blockers);
        updatedBlockers.add(blocker);
        return new MolangExpressionAnalysis(compileTimeEvaluable, runtimeEnumerable, sideEffectFree, updatedBlockers);
    }
}
