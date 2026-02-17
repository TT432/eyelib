package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;

public record FaceData(
        double[] uv,
        Integer texture,
        @SerializedName("cullface") String cullFace,
        int rotation,
        int tint
) {
}
