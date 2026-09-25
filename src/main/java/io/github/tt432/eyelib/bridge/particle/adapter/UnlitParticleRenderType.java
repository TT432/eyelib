package io.github.tt432.eyelib.bridge.particle.adapter;

//? if <1.20.6 {
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;

/** Forge 1.20.1 粒子 alpha-test 通道，去掉实体方向光，保留 lightmap、裁剪和深度行为。 */
final class UnlitParticleRenderType extends RenderType {
    // Supplier 在每次绘制时取当前 shader，资源重载后不能继续持有旧 ShaderInstance。
    private static final ShaderStateShard UNLIT_SHADER = new ShaderStateShard(
            ForgeHooksClient.ClientEvents::getEntityTranslucentUnlitShader);

    private UnlitParticleRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                                   int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                   Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    // 调用方按 ParticleDefinition 缓存，不再建立永久的纹理路径缓存。
    static RenderType cutout(ResourceLocation texture, boolean disableCulling) {
        return create("eyelib_particle_unlit_cutout", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, SMALL_BUFFER_SIZE, true, false,
                CompositeState.builder()
                        .setShaderState(UNLIT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(disableCulling ? NO_CULL : CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true));
    }
}
//?}
