package io.github.tt432.eyelibmodel.tree;

import org.jspecify.annotations.Nullable;

public interface ModelGroupNode<C extends ModelCubeNode> {
    @Nullable
    ModelGroupNode<C> getChild(int groupName);

    @Nullable
    C getCubeNode(int index);
}
