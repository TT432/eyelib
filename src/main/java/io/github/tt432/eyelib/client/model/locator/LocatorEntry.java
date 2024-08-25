package io.github.tt432.eyelib.client.model.locator;

import io.github.tt432.eyelib.client.model.tree.ModelCubeNode;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record LocatorEntry(
        String name,
        Vector3f offset,
        Vector3f rotation
) implements ModelCubeNode {
}
