package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;

public record Meta(
        @SerializedName("format_version") String formatVersion,
        @SerializedName("model_format") String modelFormat,
        @SerializedName("box_uv") boolean boxUv
) {
}
