package io.github.tt432.eyelib.client.model.importer;

import org.joml.Vector3f;

import java.util.List;

record ImportedBoneData(
        int id,
        int parentId,
        Vector3f pivot,
        Vector3f rotation,
        List<ImportedCubeData> cubes,
        List<ImportedLocatorData> locators
) {
}
