package io.github.tt432.eyelib.material.gl.stencil;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public enum StencilPassOp implements PortStringRepresentable {
    /**
     * 保留缓冲区原有值
     */
    Keep(GL11.GL_KEEP),

    /**
     * 使用参考值与掩码的按位与结果替换缓冲区值
     */
    Replace(GL11.GL_REPLACE);

    public static final Codec<StencilPassOp> CODEC = PortStringRepresentable.fromEnum(StencilPassOp::values);
    public final int value;

    StencilPassOp(int value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}