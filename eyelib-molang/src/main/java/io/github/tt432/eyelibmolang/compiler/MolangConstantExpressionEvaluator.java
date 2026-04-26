package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MolangConstantExpressionEvaluator {
    public static Optional<MolangObject> tryEvaluate(String expression) {
        String normalized = expression == null ? "" : expression.trim();
        if (normalized.isBlank()) {
            return Optional.of(MolangNull.INSTANCE);
        }

        MolangExpressionAnalyzer.ensureMappingsInitialized();

        MolangExpressionAnalysis analysis = MolangExpressionAnalyzer.analyze(normalized);
        if (!analysis.compileTimeEvaluable()) {
            return Optional.empty();
        }

        return Optional.ofNullable(MolangCompileHandler.compile(normalized).apply(new MolangScope()));
    }

    public static Optional<Float> tryEvaluateNumber(String expression) {
        return tryEvaluate(expression)
                .filter(MolangObject::isNumber)
                .map(MolangObject::asFloat);
    }
}
