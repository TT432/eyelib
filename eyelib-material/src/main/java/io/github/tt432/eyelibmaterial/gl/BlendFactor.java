package io.github.tt432.eyelibmaterial.gl;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
@NullMarked
public enum BlendFactor implements StringRepresentable {
    DestColor(GL11.GL_DST_COLOR),
    SourceColor(GL11.GL_SRC_COLOR),
    Zero(GL11.GL_ZERO),
    One(GL11.GL_ONE),
    OneMinusDestColor(GL11.GL_ONE_MINUS_DST_COLOR),
    OneMinusSrcColor(GL11.GL_ONE_MINUS_SRC_COLOR),
    SourceAlpha(GL11.GL_SRC_ALPHA),
    DestAlpha(GL11.GL_DST_ALPHA),
    OneMinusSrcAlpha(GL11.GL_ONE_MINUS_SRC_ALPHA),
    ;

    public static final Codec<BlendFactor> CODEC = StringRepresentable.fromEnum(BlendFactor::values);

    public final int factor;

    BlendFactor(int factor) {
        this.factor = factor;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}