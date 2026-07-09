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
            DynamicTexture texture = new DynamicTexture(image);
            this.register(path, texture);
            cir.setReturnValue(texture);
        }
    }
    //?}
}
