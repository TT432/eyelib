package io.github.tt432.eyelib.client.model.locator;

import io.github.tt432.eyelib.client.model.tree.ModelGroupNode;

import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public record GroupLocator(
        Map<String, ? extends ModelGroupNode<LocatorEntry>> children,
        List<LocatorEntry> cubes
) implements ModelGroupNode<LocatorEntry> {
    @Override
    public ModelGroupNode<LocatorEntry> getChild(String groupName) {
        return children.get(groupName);
    }

    @Override
    public LocatorEntry getCubeNode(int index) {
        if (index < 0 || index >= cubes.size()) return null;
        return cubes.get(index);
    }
}
