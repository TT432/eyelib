package io.github.tt432.eyelib.bridge.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.NativeImageIO;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import org.jspecify.annotations.Nullable;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public interface NativeImagePort {
    static void upload(String texture, NativeImage image) {
        NativeImageIO.upload(texture, image);
    }

    //? if <26.1 {
    static void upload(ResourceLocation texture, NativeImage image) {
    //?} else {
    static void upload(Identifier texture, NativeImage image) {
    //?}
        NativeImageIO.upload(texture, image);
    }

    @Nullable
    //? if <26.1 {
    static <R> R download(ResourceLocation texture, Function<NativeImage, R> imageFunction) {
    //?} else {
    static <R> R download(Identifier texture, Function<NativeImage, R> imageFunction) {
    //?}
        return NativeImageIO.download(texture, imageFunction);
    }

    static NativeImage copyImage(NativeImage source) {
        return NativeImageIO.copyImage(source);
    }

    static void clampAlphaToBinary(NativeImage image) {
        NativeImageIO.clampAlphaToBinary(image);
    }

    @Nullable
    //? if <26.1 {
    static ResourceLocation colorMaskTexture(ResourceLocation texture, float[] color) {
    //?} else {
    static Identifier colorMaskTexture(Identifier texture, float[] color) {
    //?}
        return NativeImageIO.colorMaskTexture(texture, color);
    }

    static void uploadFromImportedImageData(String textureKey, ImportedImageData imageData) {
        NativeImageIO.uploadFromImportedImageData(textureKey, imageData);
    }

    static void loadAndUpload(String textureKey, InputStream inputStream) throws IOException {
        NativeImageIO.loadAndUpload(textureKey, inputStream);
    }
}
