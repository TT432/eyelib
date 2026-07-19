package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.NativeImageIO;
import io.github.tt432.eyelib.importer.model.importer.AddonTextureRegistry;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
//? if <26.1 {
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 将 Bedrock addon 纹理（含 .tga）接入 MC 原版纹理加载机制。
 * <p>
 * 当 {@link TextureManager#getTexture} 查询纹理时，先查询 {@link AddonTextureRegistry}。
 * 命中则创建 {@link DynamicTexture} 并注册到 byPath，使 .tga 纹理与 .png 一样透明加载。
 *
 * @author TT432
 */
@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {

    //? if <26.1 {
    @Shadow
    @SuppressWarnings("NullAway")
    private Map<ResourceLocation, AbstractTexture> byPath;

    @Shadow
    public abstract void register(ResourceLocation path, AbstractTexture texture);

    @Inject(
            method = "getTexture(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/texture/AbstractTexture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eyelib$addonTexture(ResourceLocation path, CallbackInfoReturnable<AbstractTexture> cir) {
        AbstractTexture existing = this.byPath.get(path);
        if (existing != null && existing != MissingTextureAtlasSprite.getTexture()) {
            return;
        }

        ImportedImageData addonData = AddonTextureRegistry.get(path.getPath());
        if (addonData != null) {
        NativeImage image = NativeImageIO.fromImportedImageData(addonData);
        if (image == null) {
            return;
        }
        if (image.getWidth() <= 1 && image.getHeight() <= 1) {
            // 1x1 有两种来源：(a) addon 对 vanilla 路径的占位遮蔽（应让 vanilla SimpleTexture 正常加载，
            // 见 c535b789）；(b) addon 自带的合法 1x1 透明贴图（如 A&S Texture.transparent=bge.png，
            // vanilla 无此资源，放行会得到 FileNotFoundException 并中断整个 attachable 渲染）。
            // 仅当 vanilla 资源管理器确实能解析该路径时才放行。
            boolean vanillaHas = net.minecraft.client.Minecraft.getInstance().getResourceManager()
                    .getResource(path).isPresent();
            if (vanillaHas) {
                image.close();
                return;
            }
        }
        DynamicTexture texture = new DynamicTexture(image);
        this.register(path, texture);
        cir.setReturnValue(texture);
        }
    }
    //?} else {
    @Shadow
    @SuppressWarnings("NullAway")
    private Map<Identifier, AbstractTexture> byPath;

    @Shadow
    public abstract void register(Identifier path, AbstractTexture texture);

    @Inject(
            method = "getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eyelib$addonTexture(Identifier path, CallbackInfoReturnable<AbstractTexture> cir) {
        if (this.byPath.get(path) != null) {
            return;
        }

        ImportedImageData addonData = AddonTextureRegistry.get(path.getPath());
        if (addonData != null) {
        NativeImage image = NativeImageIO.fromImportedImageData(addonData);
        if (image == null) {
            return;
        }
        if (image.getWidth() <= 1 && image.getHeight() <= 1) {
            // 同 <26.1 分支：仅当 vanilla 能解析该路径时才放行 1x1 占位遮蔽。
            boolean vanillaHas = net.minecraft.client.Minecraft.getInstance().getResourceManager()
                    .getResource(path).isPresent();
            if (vanillaHas) {
                image.close();
                return;
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "eyelib addon texture", image);
        this.register(path, texture);
        cir.setReturnValue(texture);
        }
    }
    //?}
}
