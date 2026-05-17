package io.github.tt432.eyelibmodel.tree;

public interface ModelTree<G extends ModelGroupNode<C>, C extends ModelCubeNode> {
    G getGroup(int groupId);
}
