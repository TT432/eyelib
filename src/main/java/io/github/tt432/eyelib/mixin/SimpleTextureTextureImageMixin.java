package io.github.tt432.eyelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.util.client.NativeImages;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author TT432
 */
@Mixin(SimpleTexture.TextureImage.class)
public class SimpleTextureTextureImageMixin {
    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;getResourceOrThrow(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/Resource;"))
    private static Resource eyelib$getResourceOrThrow(ResourceManager instance, ResourceLocation resourceLocation, Operation<Resource> original) {
        if (resourceLocation.getPath().endsWith(".png")) {
            return instance.getResource(resourceLocation).orElseGet(() -> {
                try {
                    return instance.getResourceOrThrow(resourceLocation.withPath(resourceLocation.getPath().replace(".png", ".tga")));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return original.call(instance, resourceLocation);
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;read(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;"))
    private static NativeImage eyelib$read(InputStream textureStream, Operation<NativeImage> original) {
        try {
            return NativeImages.loadImage(textureStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
