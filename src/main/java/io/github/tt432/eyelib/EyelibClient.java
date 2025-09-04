package io.github.tt432.eyelib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.lwjgl.opengl.GL;

/**
 * @author TT432
 */
@Mod(value = Eyelib.MOD_ID, dist = Dist.CLIENT)
public class EyelibClient {
    public static boolean supportComputeShader() {
        return GL.getCapabilities().GL_ARB_compute_shader;
    }
}
