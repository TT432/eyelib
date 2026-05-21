package io.github.tt432.eyelibmaterial.shared;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
class VertexFormatElementEnumTest {

    static Stream<VertexFormatElementEnum> enumSource() {
        return Stream.of(VertexFormatElementEnum.values());
    }

    @ParameterizedTest
    @MethodSource("enumSource")
    @DisplayName("CODEC roundtrip for all VertexFormatElementEnum constants")
    void codecRoundtrip(VertexFormatElementEnum value) {
        var encoded = VertexFormatElementEnum.CODEC.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(false, msg -> new AssertionError("Encode failed for " + value + ": " + msg));
        var decoded = VertexFormatElementEnum.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, msg -> new AssertionError("Decode failed for " + value + ": " + msg));
        assertEquals(value, decoded,
                "Roundtrip mismatch for VertexFormatElementEnum." + value);
    }

    @Test
    @DisplayName("Position maps to DefaultVertexFormat.ELEMENT_POSITION")
    void positionMapping() {
        assertSame(DefaultVertexFormat.ELEMENT_POSITION, VertexFormatElementEnum.Position.element);
    }

    @Test
    @DisplayName("Normal maps to DefaultVertexFormat.ELEMENT_NORMAL")
    void normalMapping() {
        assertSame(DefaultVertexFormat.ELEMENT_NORMAL, VertexFormatElementEnum.Normal.element);
    }

    @Test
    @DisplayName("UV0 maps to DefaultVertexFormat.ELEMENT_UV0")
    void uv0Mapping() {
        assertSame(DefaultVertexFormat.ELEMENT_UV0, VertexFormatElementEnum.UV0.element);
    }

    @Test
    @DisplayName("UV1 maps to DefaultVertexFormat.ELEMENT_UV1")
    void uv1Mapping() {
        assertSame(DefaultVertexFormat.ELEMENT_UV1, VertexFormatElementEnum.UV1.element);
    }

    @Test
    @DisplayName("Color maps to DefaultVertexFormat.ELEMENT_COLOR")
    void colorMapping() {
        assertSame(DefaultVertexFormat.ELEMENT_COLOR, VertexFormatElementEnum.Color.element);
    }

    @Test
    @DisplayName("BoneId0 maps to DefaultVertexFormat.ELEMENT_UV2 (temporary)")
    void boneId0Mapping() {
        assertSame(DefaultVertexFormat.ELEMENT_UV2, VertexFormatElementEnum.BoneId0.element);
    }

    @Test
    @DisplayName("fromFields with Position, Normal, UV0 produces VertexFormat with 3 elements")
    void fromFieldsThreeElements() {
        EnumSet<VertexFormatElementEnum> fields = EnumSet.of(
                VertexFormatElementEnum.Position,
                VertexFormatElementEnum.Normal,
                VertexFormatElementEnum.UV0
        );
        VertexFormat format = VertexFormatElementEnum.fromFields(fields);
        assertEquals(3, format.getElements().size());
        assertSame(DefaultVertexFormat.ELEMENT_POSITION, format.getElements().get(0));
        assertSame(DefaultVertexFormat.ELEMENT_NORMAL, format.getElements().get(1));
        assertSame(DefaultVertexFormat.ELEMENT_UV0, format.getElements().get(2));
    }

    @Test
    @DisplayName("fromFields with all 6 constants produces VertexFormat with 6 elements")
    void fromFieldsAllElements() {
        EnumSet<VertexFormatElementEnum> fields = EnumSet.allOf(VertexFormatElementEnum.class);
        VertexFormat format = VertexFormatElementEnum.fromFields(fields);
        assertEquals(6, format.getElements().size());
    }

    @Test
    @DisplayName("fromFields with empty set produces VertexFormat with 0 elements")
    void fromFieldsEmpty() {
        EnumSet<VertexFormatElementEnum> fields = EnumSet.noneOf(VertexFormatElementEnum.class);
        VertexFormat format = VertexFormatElementEnum.fromFields(fields);
        assertEquals(0, format.getElements().size());
    }
}