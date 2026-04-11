package io.github.tt432.eyelibimporter.model.importer;

import org.joml.Vector3f;

public record ImportedLocatorData(
        String name,
        Vector3f offset,
        Vector3f rotation,
        boolean ignoreInheritedScale,
        boolean isNullObject
) {
}
