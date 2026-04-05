package io.github.tt432.eyelib.client.model.importer;

import org.joml.Vector3f;

record ImportedTextureMeshData(
        String texture,
        Vector3f position,
        Vector3f rotation,
        Vector3f localPivot,
        Vector3f scale
) {
}
