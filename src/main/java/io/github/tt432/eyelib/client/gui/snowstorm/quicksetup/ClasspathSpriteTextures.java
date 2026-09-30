package io.github.tt432.eyelib.client.gui.snowstorm.quicksetup;
//? if >=1.20.1 {
import com.mojang.blaze3d.platform.NativeImage;
//? if <26.1 {
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Snowstorm 内置 PNG（classpath {@code /snowstorm/...}，非 assets/&lt;ns&gt;/ 结构，
 * ResourceManager 够不到）→ DynamicTexture 注册接缝（stage minecraft_block 加载同款模式）。
 *
 * <p>返回 id 字符串（"eyelib:snowstorm_sprites/&lt;id&gt;"），UI 侧经
 * {@code SpriteTexture.of(String)} 消费（三版本 LDLib 均有 String 重载，规避
 * 26.1 ResourceLocation→Identifier 类型漂移）。</p>
 *
 * <p>偏离：26.1 无 GlStateManager，NEAREST 过滤设置跳过（默认过滤）。</p>
 */
public final class ClasspathSpriteTextures {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathSpriteTextures.class);
    private static final Map<String, Boolean> LOADED = new HashMap<>();

    private ClasspathSpriteTextures() {
    }

    /** 注册并返回 sprite 纹理 id 字符串（幂等；失败返回 null 并告警一次）。 */
    public static @Nullable String sprite(String id) {
        Boolean loaded = LOADED.get(id);
        if (loaded == null) {
            loaded = load(id);
            LOADED.put(id, loaded);
        }
        return loaded ? "eyelib:snowstorm_sprites/" + id : null;
    }

    private static boolean load(String id) {
        try (InputStream in = ClasspathSpriteTextures.class.getResourceAsStream("/snowstorm/sprites/" + id + ".png")) {
            if (in == null) {
                LOGGER.warn("[snowstorm] sprite {}.png not found on classpath", id);
                return false;
            }
            NativeImage image = NativeImage.read(in);
            //? if <26.1 {
            ResourceLocation rl = ResourceLocation.tryParse("eyelib:snowstorm_sprites/" + id);
            if (rl == null) {
                return false;
            }
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(rl, texture);
            // NearestFilter as-is（JS Texture magFilter/minFilter=NearestFilter）
            RenderSystem.bindTexture(texture.getId());
            int glTexture2D = 0x0DE1; // GL_TEXTURE_2D
            GlStateManager._texParameter(glTexture2D, 0x2800 /* GL_TEXTURE_MAG_FILTER */, 0x2600 /* GL_NEAREST */);
            GlStateManager._texParameter(glTexture2D, 0x2801 /* GL_TEXTURE_MIN_FILTER */, 0x2600);
            //?} else {
            Identifier rl = Identifier.parse("eyelib:snowstorm_sprites/" + id);
            DynamicTexture texture = new DynamicTexture(() -> "snowstorm sprite " + id, image);
            Minecraft.getInstance().getTextureManager().register(rl, texture);
            // 26.1：GlStateManager 不存在，NEAREST 过滤设置跳过（默认过滤，偏离已记录）
            //?}
            return true;
        } catch (Exception e) {
            LOGGER.warn("[snowstorm] failed to load sprite {}.png", id, e);
            return false;
        }
    }
}
//?}
