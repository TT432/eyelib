package io.github.tt432.eyelib.material.shared;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
//? if >=1.20.6 {
import com.mojang.blaze3d.vertex.VertexFormatElement;
//}
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
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
class VertexFormatElementEnumTest {

    //? if <1.20.6 {
    private static final VertexFormatElement EXPECTED_POSITION = DefaultVertexFormat.ELEMENT_POSITION;
    private static final VertexFormatElement EXPECTED_NORMAL = DefaultVertexFormat.ELEMENT_NORMAL;
    private static final VertexFormatElement EXPECTED_UV0 = DefaultVertexFormat.ELEMENT_UV0;
    private static final VertexFormatElement EXPECTED_UV1 = DefaultVertexFormat.ELEMENT_UV1;
    private static final VertexFormatElement EXPECTED_COLOR = DefaultVertexFormat.ELEMENT_COLOR;
    private static final VertexFormatElement EXPECTED_UV2 = DefaultVertexFormat.ELEMENT_UV2;
    //?} else {
    private static final VertexFormatElement EXPECTED_POSITION = VertexFormatElement.POSITION;
    private static final VertexFormatElement EXPECTED_NORMAL = VertexFormatElement.NORMAL;
    private static final VertexFormatElement EXPECTED_UV0 = VertexFormatElement.UV0;
    private static final VertexFormatElement EXPECTED_UV1 = VertexFormatElement.UV1;
    private static final VertexFormatElement EXPECTED_COLOR = VertexFormatElement.COLOR;
    private static final VertexFormatElement EXPECTED_UV2 = VertexFormatElement.UV2;
    //}

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

    @Test
    @DisplayName("Position maps to expected element")
    void positionMapping() {
        assertSame(EXPECTED_POSITION, VertexFormatElementEnum.Position.element);
    }

    @Test
    @DisplayName("Normal maps to expected element")
    void normalMapping() {
        assertSame(EXPECTED_NORMAL, VertexFormatElementEnum.Normal.element);
    }

    @Test
    @DisplayName("UV0 maps to expected element")
    void uv0Mapping() {
        assertSame(EXPECTED_UV0, VertexFormatElementEnum.UV0.element);
    }

    @Test
    @DisplayName("UV1 maps to expected element")
    void uv1Mapping() {
        assertSame(EXPECTED_UV1, VertexFormatElementEnum.UV1.element);
    }

    @Test
    @DisplayName("Color maps to expected element")
    void colorMapping() {
        assertSame(EXPECTED_COLOR, VertexFormatElementEnum.Color.element);
    }

    @Test
    @DisplayName("BoneId0 maps to expected UV2 element (temporary)")
    void boneId0Mapping() {
        assertSame(EXPECTED_UV2, VertexFormatElementEnum.BoneId0.element);
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
        assertSame(EXPECTED_POSITION, format.getElements().get(0));
        assertSame(EXPECTED_NORMAL, format.getElements().get(1));
        assertSame(EXPECTED_UV0, format.getElements().get(2));
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
