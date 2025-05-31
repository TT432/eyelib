package io.github.tt432.eyelib.client.gl.stencil;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public enum StencilDepthFailOp implements StringRepresentable {
    /**
     * 保留缓冲区原有值
     */
    Keep(GL11.GL_KEEP),

    /**
     * 使用参考值与掩码的按位与结果替换缓冲区值
     */
    Replace(GL11.GL_REPLACE);

    public static final Codec<StencilDepthFailOp> CODEC = StringRepresentable.fromEnum(StencilDepthFailOp::values);
    public final int value;

    StencilDepthFailOp(int value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
