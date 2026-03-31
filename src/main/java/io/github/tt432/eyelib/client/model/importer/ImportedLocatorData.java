package io.github.tt432.eyelib.client.model.importer;

import org.joml.Vector3f;

record ImportedLocatorData(
        String name,
        Vector3f offset,
        Vector3f rotation
) {
}
