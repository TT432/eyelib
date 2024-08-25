package io.github.tt432.eyelib.client.model.tree;

/**
 * @author TT432
 */
public interface ModelGroupNode<C extends ModelCubeNode> {
    ModelGroupNode<C> getChild(String groupName);

    C getCubeNode(int index);
}
