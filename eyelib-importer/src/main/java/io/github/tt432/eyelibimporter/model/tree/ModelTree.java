package io.github.tt432.eyelibimporter.model.tree;

public interface ModelTree<G extends ModelGroupNode<C>, C extends ModelCubeNode> {
    G getGroup(int groupId);
}
