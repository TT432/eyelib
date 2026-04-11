package io.github.tt432.eyelibimporter.model.tree;

import org.jetbrains.annotations.Nullable;

public interface ModelGroupNode<C extends ModelCubeNode> {
    @Nullable
    ModelGroupNode<C> getChild(int groupName);

    @Nullable
    C getCubeNode(int index);
}
