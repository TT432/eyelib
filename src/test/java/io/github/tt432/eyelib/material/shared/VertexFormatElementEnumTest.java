package io.github.tt432.eyelib.material.shared;

import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
class VertexFormatElementEnumTest {

    static Stream<VertexFormatElementEnum> enumSource() {
        return Stream.of(VertexFormatElementEnum.values());
    }

    @ParameterizedTest
    @MethodSource("enumSource")
    @DisplayName("CODEC roundtrip for all VertexFormatElementEnum constants")
    void codecRoundtrip(VertexFormatElementEnum value) {
        var encoded = TestCodecUtil.unwrap(VertexFormatElementEnum.CODEC.encodeStart(JsonOps.INSTANCE, value),
                msg -> new AssertionError("Encode failed for " + value + ": " + msg));
        var decoded = TestCodecUtil.unwrap(VertexFormatElementEnum.CODEC.parse(JsonOps.INSTANCE, encoded),
                msg -> new AssertionError("Decode failed for " + value + ": " + msg));
        assertEquals(value, decoded,
                "Roundtrip mismatch for VertexFormatElementEnum." + value);
    }
}
