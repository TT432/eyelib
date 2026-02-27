package io.github.tt432.eyelib.client.model.bbmodel;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public record Texture(
        String name,
        String path,
        String folder,
        String namespace,
        String id,
        String group,
        int width,
        int height,
        int uvWidth,
        int uvHeight,
        boolean particle,
        boolean useAsDefault,
        boolean layersEnabled,
        boolean syncToProject,
        String renderMode,
        String renderSides,
        String pbrChannel,
        int frameTime,
        String frameOrderType,
        String frameOrder,
        boolean frameInterpolate,
        boolean visible,
        boolean internal,
        boolean saved,
        String uuid,
        String source,

        NativeImage nativeImage
) {
    private record Part1(
            String name,
            String path,
            String folder,
            String namespace,
            String id,
            String group,
            int width,
            int height,
            int uvWidth,
            int uvHeight,
            boolean particle,
            boolean useAsDefault,
            boolean layersEnabled,
            boolean syncToProject,
            String renderMode
    ) {
        public static final MapCodec<Part1> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(Part1::name),
                Codec.STRING.optionalFieldOf("path", "").forGetter(Part1::path),
                Codec.STRING.optionalFieldOf("folder", "").forGetter(Part1::folder),
                Codec.STRING.optionalFieldOf("namespace", "").forGetter(Part1::namespace),
                Codec.STRING.optionalFieldOf("id", "").forGetter(Part1::id),
                Codec.STRING.optionalFieldOf("group", "").forGetter(Part1::group),
                Codec.INT.optionalFieldOf("width", 0).forGetter(Part1::width),
                Codec.INT.optionalFieldOf("height", 0).forGetter(Part1::height),
                Codec.INT.optionalFieldOf("uv_width", 0).forGetter(Part1::uvWidth),
                Codec.INT.optionalFieldOf("uv_height", 0).forGetter(Part1::uvHeight),
                Codec.BOOL.optionalFieldOf("particle", false).forGetter(Part1::particle),
                Codec.BOOL.optionalFieldOf("use_as_default", false).forGetter(Part1::useAsDefault),
                Codec.BOOL.optionalFieldOf("layers_enabled", false).forGetter(Part1::layersEnabled),
                EyelibCodec.withAlternative(
                        Codec.BOOL,
                        Codec.STRING.xmap(s -> s.equals("true"), Object::toString)
                ).optionalFieldOf("sync_to_project", false).forGetter(Part1::syncToProject),
                Codec.STRING.optionalFieldOf("render_mode", "").forGetter(Part1::renderMode)
        ).apply(ins, Part1::new));
    }

    private record Part2(
            String renderSides,
            String pbrChannel,
            int frameTime,
            String frameOrderType,
            String frameOrder,
            boolean frameInterpolate,
            boolean visible,
            boolean internal,
            boolean saved,
            String uuid,
            String source
    ) {
        public static final MapCodec<Part2> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.optionalFieldOf("render_sides", "").forGetter(Part2::renderSides),
                Codec.STRING.optionalFieldOf("pbr_channel", "").forGetter(Part2::pbrChannel),
                Codec.INT.optionalFieldOf("frame_time", 0).forGetter(Part2::frameTime),
                Codec.STRING.optionalFieldOf("frame_order_type", "").forGetter(Part2::frameOrderType),
                Codec.STRING.optionalFieldOf("frame_order", "").forGetter(Part2::frameOrder),
                Codec.BOOL.optionalFieldOf("frame_interpolate", false).forGetter(Part2::frameInterpolate),
                Codec.BOOL.optionalFieldOf("visible", false).forGetter(Part2::visible),
                Codec.BOOL.optionalFieldOf("internal", false).forGetter(Part2::internal),
                Codec.BOOL.optionalFieldOf("saved", false).forGetter(Part2::saved),
                Codec.STRING.optionalFieldOf("uuid", "").forGetter(Part2::uuid),
                Codec.STRING.optionalFieldOf("source", "").forGetter(Part2::source)
        ).apply(ins, Part2::new));
    }

    public static final Codec<Texture> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Part1.MAP_CODEC.forGetter(tex -> new Part1(tex.name, tex.path, tex.folder, tex.namespace, tex.id, tex.group, tex.width, tex.height, tex.uvWidth, tex.uvHeight, tex.particle, tex.useAsDefault, tex.layersEnabled, tex.syncToProject, tex.renderMode)),
            Part2.MAP_CODEC.forGetter(tex -> new Part2(tex.renderSides, tex.pbrChannel, tex.frameTime, tex.frameOrderType, tex.frameOrder, tex.frameInterpolate, tex.visible, tex.internal, tex.saved, tex.uuid, tex.source))
    ).apply(ins, (p1, p2) -> {
        try {
            return new Texture(p1.name, p1.path, p1.folder, p1.namespace, p1.id, p1.group, p1.width, p1.height, p1.uvWidth, p1.uvHeight, p1.particle, p1.useAsDefault, p1.layersEnabled, p1.syncToProject, p1.renderMode, p2.renderSides, p2.pbrChannel, p2.frameTime, p2.frameOrderType, p2.frameOrder, p2.frameInterpolate, p2.visible, p2.internal, p2.saved, p2.uuid, p2.source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }));

    public Texture(String name, String path, String folder, String namespace, String id, String group, int width, int height, int uvWidth, int uvHeight, boolean particle, boolean useAsDefault, boolean layersEnabled, boolean syncToProject, String renderMode, String renderSides, String pbrChannel, int frameTime, String frameOrderType, String frameOrder, boolean frameInterpolate, boolean visible, boolean internal, boolean saved, String uuid, String source) throws IOException {
        this(name, path, folder, namespace, id, group, width, height, uvWidth, uvHeight, particle, useAsDefault, layersEnabled, syncToProject, renderMode, renderSides, pbrChannel, frameTime, frameOrderType, frameOrder, frameInterpolate, visible, internal, saved, uuid, source, NativeImage.read(new ByteArrayInputStream(getBytes(source))));
    }

    private static byte[] getBytes(String source) {
        if (source != null && !source.isEmpty()) {
            try {
                String base64 = source;
                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }
                return Base64.getDecoder().decode(base64);
            } catch (Exception e) {
                // Ignore
            }
        }

        return null;
    }

    public int imageWidth() {
        return nativeImage.getWidth();
    }

    public int imageHeight() {
        return nativeImage.getHeight();
    }
}
