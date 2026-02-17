package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

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

        @SerializedName("uv_width")
        int uvWidth,

        @SerializedName("uv_height")
        int uvHeight,

        boolean particle,

        @SerializedName("use_as_default")
        boolean useAsDefault,

        @SerializedName("layers_enabled")
        boolean layersEnabled,

        @SerializedName("sync_to_project")
        boolean syncToProject,

        @SerializedName("render_mode")
        String renderMode,

        @SerializedName("render_sides")
        String renderSides,

        @SerializedName("pbr_channel")
        String pbrChannel,

        @SerializedName("frame_time")
        int frameTime,

        @SerializedName("frame_order_type")
        String frameOrderType,

        @SerializedName("frame_order")
        String frameOrder,

        @SerializedName("frame_interpolate")
        boolean frameInterpolate,

        boolean visible,
        boolean internal,
        boolean saved,
        String uuid,
        String source,

        byte[] sourceBytes
) {
    public Texture {
        if (source != null && !source.isEmpty() && sourceBytes == null) {
            try {
                String base64 = source;
                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }
                sourceBytes = Base64.getDecoder().decode(base64);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Nullable
    public NativeImage getNativeImage() throws IOException {
        if (sourceBytes == null) {
            return null;
        }
        return NativeImage.read(new ByteArrayInputStream(sourceBytes));
    }
}
