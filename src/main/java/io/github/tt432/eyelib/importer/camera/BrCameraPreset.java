package io.github.tt432.eyelib.importer.camera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

/** bedrock camera_preset 定义的 import 层表示。
 * @author TT432 */
@NullMarked
public record BrCameraPreset(
        String formatVersion,
        String identifier
) {
    public static final Codec<BrCameraPreset> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrCameraPreset::formatVersion),
            RecordCodecBuilder.<String>create(ins2 -> ins2.group(
                    Codec.STRING.fieldOf("identifier").forGetter(id -> id)
            ).apply(ins2, id -> id)).fieldOf("minecraft:camera_preset").forGetter(BrCameraPreset::identifier)
    ).apply(ins, BrCameraPreset::new));
}
