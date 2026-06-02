package io.github.tt432.eyelibmodel.tree;

import org.jspecify.annotations.Nullable;

/**
 * 模型组节点接口，提供获取子组和立方体节点的方法。
 *
 * @author TT432
 */
public interface ModelGroupNode<C extends ModelCubeNode> {
    @Nullable
    ModelGroupNode<C> getChild(int groupName);

    @Nullable
    C getCubeNode(int index);
}