package io.github.tt432.eyelib.client.model.tree;

import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
public interface ModelGroupNode<C extends ModelCubeNode> {
    @Nullable
    ModelGroupNode<C> getChild(int groupName);

    @Nullable
    C getCubeNode(int index);
}
