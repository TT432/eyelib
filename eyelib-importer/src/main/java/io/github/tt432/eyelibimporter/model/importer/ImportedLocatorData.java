package io.github.tt432.eyelibimporter.model.importer;

import org.joml.Vector3f;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record ImportedLocatorData(
        String name,
        Vector3f offset,
        Vector3f rotation,
        boolean ignoreInheritedScale,
        boolean isNullObject
) {
}
