package io.github.tt432.eyelib.client.gl;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public enum BlendFactor implements StringRepresentable {
    // 缓冲区颜色值
    DestColor(GL11.GL_DST_COLOR),
    // 当前颜色值
    SourceColor(GL11.GL_SRC_COLOR),
    // (0,0,0)
    Zero(GL11.GL_ZERO),
    // (1,1,1)
    One(GL11.GL_ONE),
    // (1,1,1) - 缓冲区颜色值
    OneMinusDestColor(GL11.GL_ONE_MINUS_DST_COLOR),
    // (1,1,1) - 当前颜色值
    OneMinusSrcColor(GL11.GL_ONE_MINUS_SRC_COLOR),
    // 当前颜色中的alpha值
    SourceAlpha(GL11.GL_SRC_ALPHA),
    // 缓冲区颜色中的alpha值
    DestAlpha(GL11.GL_DST_ALPHA),
    // 1 - 当前颜色值中的alpha值
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
