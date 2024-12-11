package io.github.tt432.eyelib.client.gl;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public enum DepthFunc implements StringRepresentable {
    Always(GL11.GL_ALWAYS),// 总是通过
    Equal(GL11.GL_EQUAL),// 深度值与缓冲区值相等时通过
    NotEqual(GL11.GL_NOTEQUAL),// 深度值与缓冲区值不相等时通过
    Less(GL11.GL_LESS),// 深度值小于缓冲区值时通过
    Greater(GL11.GL_GREATER),// 深度值大于缓冲区值时通过
    GreaterEqual(GL11.GL_GEQUAL),// 深度值大于等于缓冲区值时通过
    LessEqual(GL11.GL_LEQUAL);// 深度值小于等于缓冲区值时通过

    public static final Codec<DepthFunc> CODEC = StringRepresentable.fromEnum(DepthFunc::values);

    public final int value;

    DepthFunc(int value) {
        this.value = value;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name();
    }
}
