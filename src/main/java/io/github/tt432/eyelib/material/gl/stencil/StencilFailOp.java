package io.github.tt432.eyelibmaterial.gl.stencil;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
@NullMarked
public enum StencilFailOp implements PortStringRepresentable {
    /**
     * 保留缓冲区原有值
     */
    Keep(GL11.GL_KEEP),

    /**
     * 使用参考值与掩码的按位与结果替换缓冲区值
     */
    Replace(GL11.GL_REPLACE);

    public static final Codec<StencilFailOp> CODEC = PortStringRepresentable.fromEnum(StencilFailOp::values);
    public final int value;

    StencilFailOp(int value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}