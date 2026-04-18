package io.github.tt432.eyelibimporter.model.importer;

import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public record ImportedBoneData(
        int id,
        int parentId,
        Vector3f pivot,
        Vector3f rotation,
        List<ImportedCubeData> cubes,
        List<ImportedLocatorData> locators,
        @Nullable String material,
        boolean reset,
        boolean mirrorUv,
        @Nullable String binding,
        List<ImportedTextureMeshData> textureMeshes
) {
}

