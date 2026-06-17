package io.github.tt432.eyelib.importer.model.importer;

import org.jspecify.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/** @author TT432 */
public record ImportedFaceData(
        List<Vector3f> positions,
        List<Vector2f> uvs,
        Vector3f normal,
        int textureIndex,
        @Nullable String materialInstance
) {
}

