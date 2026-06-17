package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
class EnumCodecTest {

    // --- GLStates ---

    static Stream<GLStates> glStatesSource() {
        return Stream.of(GLStates.values());
    }

    @ParameterizedTest
    @MethodSource("glStatesSource")
    @DisplayName("GLStates CODEC roundtrip")
    void glStatesRoundtrip(GLStates value) {
        assertRoundtrip(value, GLStates.CODEC);
    }

    // --- DepthFunc ---

    static Stream<DepthFunc> depthFuncSource() {
        return Stream.of(DepthFunc.values());
    }

    @ParameterizedTest
    @MethodSource("depthFuncSource")
    @DisplayName("DepthFunc CODEC roundtrip")
    void depthFuncRoundtrip(DepthFunc value) {
        assertRoundtrip(value, DepthFunc.CODEC);
    }

    // --- BlendFactor ---

    static Stream<BlendFactor> blendFactorSource() {
        return Stream.of(BlendFactor.values());
    }

    @ParameterizedTest
    @MethodSource("blendFactorSource")
    @DisplayName("BlendFactor CODEC roundtrip")
    void blendFactorRoundtrip(BlendFactor value) {
        assertRoundtrip(value, BlendFactor.CODEC);
    }

    // --- StencilFunc ---

    static Stream<StencilFunc> stencilFuncSource() {
        return Stream.of(StencilFunc.values());
    }

    @ParameterizedTest
    @MethodSource("stencilFuncSource")
    @DisplayName("StencilFunc CODEC roundtrip")
    void stencilFuncRoundtrip(StencilFunc value) {
        assertRoundtrip(value, StencilFunc.CODEC);
    }

    // --- StencilFailOp ---

    static Stream<StencilFailOp> stencilFailOpSource() {
        return Stream.of(StencilFailOp.values());
    }

    @ParameterizedTest
    @MethodSource("stencilFailOpSource")
    @DisplayName("StencilFailOp CODEC roundtrip")
    void stencilFailOpRoundtrip(StencilFailOp value) {
        assertRoundtrip(value, StencilFailOp.CODEC);
    }

    // --- StencilPassOp ---

    static Stream<StencilPassOp> stencilPassOpSource() {
        return Stream.of(StencilPassOp.values());
    }

    @ParameterizedTest
    @MethodSource("stencilPassOpSource")
    @DisplayName("StencilPassOp CODEC roundtrip")
    void stencilPassOpRoundtrip(StencilPassOp value) {
        assertRoundtrip(value, StencilPassOp.CODEC);
    }

    // --- StencilDepthFailOp ---

    static Stream<StencilDepthFailOp> stencilDepthFailOpSource() {
        return Stream.of(StencilDepthFailOp.values());
    }

    @ParameterizedTest
    @MethodSource("stencilDepthFailOpSource")
    @DisplayName("StencilDepthFailOp CODEC roundtrip")
    void stencilDepthFailOpRoundtrip(StencilDepthFailOp value) {
        assertRoundtrip(value, StencilDepthFailOp.CODEC);
    }

    // --- MsaaSupport ---

    static Stream<MsaaSupport> msaaSupportSource() {
        return Stream.of(MsaaSupport.values());
    }

    @ParameterizedTest
    @MethodSource("msaaSupportSource")
    @DisplayName("MsaaSupport CODEC roundtrip")
    void msaaSupportRoundtrip(MsaaSupport value) {
        assertRoundtrip(value, MsaaSupport.CODEC);
    }

    // --- PrimitiveMode ---

    static Stream<PrimitiveMode> primitiveModeSource() {
        return Stream.of(PrimitiveMode.values());
    }

    @ParameterizedTest
    @MethodSource("primitiveModeSource")
    @DisplayName("PrimitiveMode CODEC roundtrip")
    void primitiveModeRoundtrip(PrimitiveMode value) {
        assertRoundtrip(value, PrimitiveMode.CODEC);
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