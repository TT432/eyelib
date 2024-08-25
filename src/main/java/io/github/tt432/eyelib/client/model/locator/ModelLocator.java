package io.github.tt432.eyelib.client.model.locator;

import io.github.tt432.eyelib.client.model.tree.ModelTree;

import java.util.Map;

/**
 * @author TT432
 */
public record ModelLocator(
        Map<String, GroupLocator> groupLocatorMap
) implements ModelTree<GroupLocator, LocatorEntry> {
    @Override
    public GroupLocator getGroup(String groupName) {
        return groupLocatorMap.get(groupName);
    }
}
