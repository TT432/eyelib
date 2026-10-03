//? if <26.1 {
package io.github.tt432.eyelib.bridge.material.adapter;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.material.port.PortRenderPass.Transparency;
import net.minecraft.client.renderer.ShaderInstance;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
//?}

/**
 * 粒子无方向光 shader（ADR-0033）：{@code eyelib:particle_unlit} 的注册与获取。
 *
 * <p>Bedrock 粒子光照只有环境光 tint、无方向项，vanilla entity shader 的
 * {@code minecraft_mix_light} 对 billboard 法线产生视角相关明暗；1.21.1 vanilla/neoforge
 * 均无现成 unlit entity shader，1.20.1 虽有 Forge unlit，但两版本统一用自带 shader
 * 保持语义一致（fsh 复用 vanilla {@code rendertype_entity_cutout}，仅 vsh 自定义）。
 *
 * <p>注册失败/资源重载窗口期 {@link #unlitShader()} 为 null，调用方回退 vanilla
 * entity shader（退化回方向光近似，不崩）。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class ParticleUnlitShaders {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleUnlitShaders.class);

    // 1.20.1 与 1.21.1 的 vanilla 母版不同（1.21.1 fog_distance 2 参签名、cutout.fsh 不声明 normal 输入），
    // 资产按版本分开（同 LegacySkinningManager 先例）
    //? if <1.20.6 {
    private static final String SHADER_NAME = "particle_unlit";
    //?} else {
    private static final String SHADER_NAME = "particle_unlit_121";
    //?}

    private static @Nullable ShaderInstance unlit;
    //? if <1.20.6 {
    private static final Map<Transparency, ShaderInstance> TEXTURE_COLOR = new EnumMap<>(Transparency.class);
    //?}

    private ParticleUnlitShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        //? if <1.20.6 {
        TEXTURE_COLOR.clear();
        io.github.tt432.eyelib.bridge.client.render.adapter.TextureColorWorldPass.clear();
        //?}
        // 重载清理：vanilla 负责关闭旧 ShaderInstance
        unlit = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            ResourceLocationBridge.fromParts("eyelib", SHADER_NAME),
                            DefaultVertexFormat.NEW_ENTITY),
                    shader -> unlit = shader);
        } catch (IOException e) {
            // 绝不能把异常抛出事件总线——RegisterShadersEvent 中断会让 vanilla 着色器全部缺失，
            // 客户端必崩（2026-08-27 实证）；回退 vanilla entity shader（方向光近似）
            LOGGER.error("eyelib:{} 注册失败，粒子回退 vanilla 方向光着色", SHADER_NAME, e);
        }
        //? if <1.20.6 {
        try {
            for (var transparency : List.of(Transparency.TRANSLUCENT, Transparency.ADDITIVE,
                    Transparency.ALPHA_TEST, Transparency.SOLID)) {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                                ResourceLocationBridge.fromParts("eyelib", "texture_color_" + transparency.name().toLowerCase(Locale.ROOT)),
                                DefaultVertexFormat.NEW_ENTITY), shader -> TEXTURE_COLOR.put(transparency, shader));
            }
        } catch (IOException e) {
            LOGGER.error("eyelib 原色 shader 注册失败，缺失的变体回退 vanilla emissive shader，无法保证原色", e);
        }
        //?}
    }

    /** 无方向光粒子 shader；重载窗口期或注册失败为 null，调用方须回退 vanilla shader。 */
    public static @Nullable ShaderInstance unlitShader() {
        return unlit;
    }

    //? if <1.20.6 {
    /** 原色 shader；注册失败回退 vanilla emissive（不保证原色，注册期已记录 error），永不返回 null。 */
    public static ShaderInstance textureColorShader(Transparency transparency) {
        ShaderInstance shader = TEXTURE_COLOR.get(transparency);
        return shader != null ? shader : net.minecraft.client.renderer.GameRenderer.getRendertypeEntityTranslucentEmissiveShader();
    }
    //?}
}
//?}
