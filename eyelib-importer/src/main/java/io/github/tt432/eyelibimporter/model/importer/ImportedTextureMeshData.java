package io.github.tt432.eyelibimporter.model.importer;

import org.joml.Vector3f;

public record ImportedTextureMeshData(
        String texture,
        Vector3f position,
        Vector3f rotation,
        Vector3f localPivot,
        Vector3f scale
) {
}
