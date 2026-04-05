package io.github.tt432.eyelib.client.model.importer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

record ImportedFaceData(
        List<Vector3f> positions,
        List<Vector2f> uvs,
        Vector3f normal,
        int textureIndex,
        @Nullable String materialInstance
) {
}
