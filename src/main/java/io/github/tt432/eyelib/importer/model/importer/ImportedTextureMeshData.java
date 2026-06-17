package io.github.tt432.eyelib.importer.model.importer;

import org.joml.Vector3f;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record ImportedTextureMeshData(
        String texture,
        Vector3f position,
        Vector3f rotation,
        Vector3f localPivot,
        Vector3f scale
) {
}
