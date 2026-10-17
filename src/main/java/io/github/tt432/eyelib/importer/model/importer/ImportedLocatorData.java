package io.github.tt432.eyelib.importer.model.importer;

import org.joml.Vector3f;

/** @author TT432 */
public record ImportedLocatorData(
        String name,
        Vector3f offset,
        Vector3f rotation,
        boolean ignoreInheritedScale,
        boolean isNullObject
) {
}
