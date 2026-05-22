package io.github.tt432.eyelibmaterial.gl;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
@NullMarked
public enum DepthFunc implements StringRepresentable {
    Always(GL11.GL_ALWAYS),
    Equal(GL11.GL_EQUAL),
    NotEqual(GL11.GL_NOTEQUAL),
    Less(GL11.GL_LESS),
    Greater(GL11.GL_GREATER),
    GreaterEqual(GL11.GL_GEQUAL),
    LessEqual(GL11.GL_LEQUAL);

    public static final Codec<DepthFunc> CODEC = StringRepresentable.fromEnum(DepthFunc::values);

    public final int value;

    DepthFunc(int value) {
        this.value = value;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}