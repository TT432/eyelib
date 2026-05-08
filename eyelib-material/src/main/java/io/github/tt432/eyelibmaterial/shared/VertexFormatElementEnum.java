package io.github.tt432.eyelibmaterial.shared;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.EnumSet;

/**
 * Bedrock vertex field constants mapped to Minecraft {@link DefaultVertexFormat} elements.
 * <p>
 * This enum replaces the currently empty {@code VertexFormatElementEnum} in
 * {@code BrMaterialEntry.java} with 6 Bedrock vertex field mappings.
 *
 * @author TT432
 */
public enum VertexFormatElementEnum implements StringRepresentable {
    Position(DefaultVertexFormat.ELEMENT_POSITION),
    Normal(DefaultVertexFormat.ELEMENT_NORMAL),
    UV0(DefaultVertexFormat.ELEMENT_UV0),
    UV1(DefaultVertexFormat.ELEMENT_UV1),
    Color(DefaultVertexFormat.ELEMENT_COLOR),
    BoneId0(DefaultVertexFormat.ELEMENT_UV2);

    public static final Codec<VertexFormatElementEnum> CODEC =
            StringRepresentable.fromEnum(VertexFormatElementEnum::values);

    public final VertexFormatElement element;

    VertexFormatElementEnum(VertexFormatElement element) {
        this.element = element;
    }

    /**
     * Builds a {@link VertexFormat} from the given set of vertex fields.
     * <p>
     * The order of elements in the resulting format follows iteration order of the enum set,
     * which is the natural declaration order of this enum.
     *
     * @param fields the set of vertex fields to include
     * @return a new VertexFormat containing the mapped elements
     */
    public static VertexFormat fromFields(EnumSet<VertexFormatElementEnum> fields) {
        ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        for (VertexFormatElementEnum field : fields) {
            elements.put(field.getSerializedName(), field.element);
        }
        return new VertexFormat(elements.buildOrThrow());
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
