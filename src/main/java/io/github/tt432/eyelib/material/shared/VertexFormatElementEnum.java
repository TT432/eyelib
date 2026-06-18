package io.github.tt432.eyelib.material.shared;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.EnumSet;

/**
 * Bedrock顶点字段常量到Minecraft {@link DefaultVertexFormat}元素的映射。
 *
 * @author TT432
 */
public enum VertexFormatElementEnum implements PortStringRepresentable {
    //? if <1.20.6 {
    Position(DefaultVertexFormat.ELEMENT_POSITION),
    Normal(DefaultVertexFormat.ELEMENT_NORMAL),
    UV0(DefaultVertexFormat.ELEMENT_UV0),
    UV1(DefaultVertexFormat.ELEMENT_UV1),
    Color(DefaultVertexFormat.ELEMENT_COLOR),
    BoneId0(DefaultVertexFormat.ELEMENT_UV2);
    //?} else {
    Position(VertexFormatElement.POSITION),
    Normal(VertexFormatElement.NORMAL),
    UV0(VertexFormatElement.UV0),
    UV1(VertexFormatElement.UV1),
    Color(VertexFormatElement.COLOR),
    BoneId0(VertexFormatElement.UV2);
    //?}

    public static final Codec<VertexFormatElementEnum> CODEC =
            PortStringRepresentable.fromEnum(VertexFormatElementEnum::values);

    public final VertexFormatElement element;

    VertexFormatElementEnum(VertexFormatElement element) {
        this.element = element;
    }

    /**
     * Builds a {@link VertexFormat} from the given set of vertex fields.
     * The order of elements in the resulting format follows iteration order of the enum set,
     * which is the natural declaration order of this enum.
     *
     * @param fields the set of vertex fields to include
     * @return a new VertexFormat containing the mapped elements
     */
    public static VertexFormat fromFields(EnumSet<VertexFormatElementEnum> fields) {
        //? if <1.20.6 {
        ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        //?} else {
        VertexFormat.Builder elements = VertexFormat.builder();
        //?}
        for (VertexFormatElementEnum field : fields) {
            //? if <1.20.6 {
            elements.put(field.getSerializedName(), field.element);
            //?} else {
            elements.add(field.getSerializedName(), field.element);
            //?}
        }
        //? if <1.20.6 {
        return new VertexFormat(elements.buildOrThrow());
        //?} else {
        return elements.build();
        //?}
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
