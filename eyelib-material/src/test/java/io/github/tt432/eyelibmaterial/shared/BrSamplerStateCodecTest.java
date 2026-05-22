package io.github.tt432.eyelibmaterial.shared;

import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
class BrSamplerStateCodecTest {

    // --- TextureFilter enum roundtrips ---

    static Stream<BrSamplerState.TextureFilter> textureFilterSource() {
        return Stream.of(BrSamplerState.TextureFilter.values());
    }

    @ParameterizedTest
    @MethodSource("textureFilterSource")
    @DisplayName("TextureFilter CODEC roundtrip")
    void textureFilterRoundtrip(BrSamplerState.TextureFilter value) {
        assertRoundtrip(value, BrSamplerState.TextureFilter.CODEC);
    }

    // --- TextureWrap enum roundtrips ---

    static Stream<BrSamplerState.TextureWrap> textureWrapSource() {
        return Stream.of(BrSamplerState.TextureWrap.values());
    }

    @ParameterizedTest
    @MethodSource("textureWrapSource")
    @DisplayName("TextureWrap CODEC roundtrip")
    void textureWrapRoundtrip(BrSamplerState.TextureWrap value) {
        assertRoundtrip(value, BrSamplerState.TextureWrap.CODEC);
    }

    // --- Full record roundtrip ---

    @Test
    @DisplayName("BrSamplerState CODEC roundtrip from JSON")
    void samplerStateRoundtrip() {
        var input = new BrSamplerState(0, BrSamplerState.TextureFilter.Point, BrSamplerState.TextureWrap.Clamp);

        var encoded = BrSamplerState.CODEC.encodeStart(JsonOps.INSTANCE, input)
                .getOrThrow(false, msg -> new AssertionError("Encode failed: " + msg));
        var decoded = BrSamplerState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, msg -> new AssertionError("Decode failed: " + msg));

        assertEquals(input, decoded, "BrSamplerState roundtrip mismatch");
    }

    // --- Generic roundtrip helper ---

    private static <T> void assertRoundtrip(T value, com.mojang.serialization.Codec<T> codec) {
        var encoded = codec.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(false, msg -> new AssertionError("Encode failed for " + value + ": " + msg));
        var decoded = codec.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, msg -> new AssertionError("Decode failed for " + value + ": " + msg));
        assertEquals(value, decoded,
                "Roundtrip mismatch for " + value.getClass().getSimpleName() + "." + value);
    }
}