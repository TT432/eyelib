package io.github.tt432.eyelib.client.gl.stencil;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public enum StencilFunc implements StringRepresentable {
    /**
     * 总是通过模板测试
     */
    Always(GL11.GL_ALWAYS),

    /**
     * 当参考值等于缓冲区值时通过测试
     */
    Equal(GL11.GL_EQUAL),

    /**
     * 当参考值不等于缓冲区值时通过测试
     */
    NotEqual(GL11.GL_NOTEQUAL),

    /**
     * 当参考值小于缓冲区值时通过测试
     */
    Less(GL11.GL_LESS),

    /**
     * 当参考值大于缓冲区值时通过测试
     */
    Greater(GL11.GL_GREATER),

    /**
     * 当参考值大于等于缓冲区值时通过测试
     */
    GreaterEqual(GL11.GL_GEQUAL),

    /**
     * 当参考值小于等于缓冲区值时通过测试
     */
    LessEqual(GL11.GL_LEQUAL);

    public static final Codec<StencilFunc> CODEC = StringRepresentable.fromEnum(StencilFunc::values);
    public final int value;

    StencilFunc(int value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
