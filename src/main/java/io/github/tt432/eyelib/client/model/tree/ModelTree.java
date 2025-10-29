package io.github.tt432.eyelib.client.model.tree;

/**
 * @author TT432
 */
public interface ModelTree<G extends ModelGroupNode<C>, C extends ModelCubeNode> {
    G getGroup(int groupId);
}
