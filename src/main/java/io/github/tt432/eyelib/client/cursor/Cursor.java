package io.github.tt432.eyelib.client.cursor;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.Pointer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author TT432
 */
@Getter
public class Cursor extends Pointer.Default {
    private final GLFWImage image;
    private final int xhot;
    private final int yhot;

    boolean using;

    private static long createCursor(GLFWImage image, int xhot, int yhot) {
        long l = glfwCreateCursor(image, xhot, yhot);
        if (l == 0)
            org.lwjgl.glfw.GLFW.nglfwGetError(0L);
        return l;
    }

    public Cursor(GLFWImage image, int xhot, int yhot) {
        super(createCursor(image, xhot, yhot));
        this.image = image;
        this.xhot = xhot;
        this.yhot = yhot;
    }

    public void set() {
        if (!using) {
            using = true;
            glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), address());
        }
    }

    public void destroy() {
        if (using) {
            using = false;
            glfwDestroyCursor(address());
        }
    }
}
