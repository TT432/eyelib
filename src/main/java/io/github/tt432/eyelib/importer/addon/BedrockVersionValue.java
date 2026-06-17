package io.github.tt432.eyelib.importer.addon;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** @author TT432 */
@NullMarked
public record BedrockVersionValue(
        List<Integer> numericParts,
        @Nullable String semanticString
) {
    public static final Codec<BedrockVersionValue> CODEC = Codec.either(Codec.INT.listOf(), Codec.STRING).xmap(
            either -> either.map(BedrockVersionValue::numeric, BedrockVersionValue::semantic),
            value -> value.semanticString() != null ? Either.right(value.semanticString()) : Either.left(value.numericParts())
    );

    public BedrockVersionValue {
        numericParts = List.copyOf(numericParts);
    }

    public static BedrockVersionValue numeric(List<Integer> numericParts) {
        return new BedrockVersionValue(numericParts, null);
    }

    public static BedrockVersionValue semantic(String semanticString) {
        return new BedrockVersionValue(List.of(), semanticString);
    }

    public boolean isNumeric() {
        return semanticString == null;
    }

    public boolean isSemanticString() {
        return semanticString != null;
    }
}
