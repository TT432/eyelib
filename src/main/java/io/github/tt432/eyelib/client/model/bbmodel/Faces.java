package io.github.tt432.eyelib.client.model.bbmodel;

import lombok.With;

@With
public record Faces(
        FaceData north,
        FaceData east,
        FaceData south,
        FaceData west,
        FaceData up,
        FaceData down
) {
}
