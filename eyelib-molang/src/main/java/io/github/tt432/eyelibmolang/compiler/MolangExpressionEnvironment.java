package io.github.tt432.eyelibmolang.compiler;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record MolangExpressionEnvironment(
        Set<String> runtimeEnumerableVariables
) {
    public static final MolangExpressionEnvironment DEFAULT = new MolangExpressionEnvironment(Set.of());

    public MolangExpressionEnvironment {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        runtimeEnumerableVariables.forEach(variable -> normalized.add(normalize(variable)));
        runtimeEnumerableVariables = Set.copyOf(normalized);
    }

    public boolean isRuntimeEnumerableVariable(String variableName) {
        return runtimeEnumerableVariables.contains(normalize(variableName));
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String normalize(String variableName) {
        return variableName.toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final LinkedHashSet<String> runtimeEnumerableVariables = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder runtimeEnumerableVariable(String variableName) {
            runtimeEnumerableVariables.add(normalize(variableName));
            return this;
        }

        public Builder runtimeEnumerableVariables(String... variableNames) {
            for (String variableName : variableNames) {
                runtimeEnumerableVariable(variableName);
            }
            return this;
        }

        public MolangExpressionEnvironment build() {
            return new MolangExpressionEnvironment(runtimeEnumerableVariables);
        }
    }
}
