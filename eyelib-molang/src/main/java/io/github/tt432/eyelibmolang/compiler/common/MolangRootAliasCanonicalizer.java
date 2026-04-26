package io.github.tt432.eyelibmolang.compiler.common;

import java.util.Locale;
import java.util.Map;

public final class MolangRootAliasCanonicalizer {
    private static final Map<String, String> ROOT_ALIASES = Map.of(
            "q", "query",
            "t", "temp",
            "v", "variable",
            "c", "context"
    );

    private MolangRootAliasCanonicalizer() {
    }

    public static String canonicalizeRoot(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return ROOT_ALIASES.getOrDefault(normalized, normalized);
    }

    public static String canonicalizeQualifiedNameRoot(String qualifiedName) {
        int separator = qualifiedName.indexOf('.');
        if (separator == -1) {
            return canonicalizeRoot(qualifiedName);
        }
        return canonicalizeRoot(qualifiedName.substring(0, separator)) + qualifiedName.substring(separator);
    }
}
