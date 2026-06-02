package io.github.tt432.eyelibmodel.tree;

/**
 * 模型树结构接口，提供按组 ID 获取子节点的方法。
 *
 * @author TT432
 */
public interface ModelTree<G extends ModelGroupNode<C>, C extends ModelCubeNode> {
    G getGroup(int groupId);
}