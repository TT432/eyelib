package io.github.tt432.eyelib.material.gl;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
@NullMarked
public enum BlendFactor implements PortStringRepresentable {
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

    public static final Codec<BlendFactor> CODEC = PortStringRepresentable.fromEnum(BlendFactor::values);

    public final int factor;

    BlendFactor(int factor) {
        this.factor = factor;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}