package io.github.tt432.eyelib.client.material;


/**
 * @author TT432
 */
public record SingleModelMaterial(
        MaterialEntry material
) implements ModelMaterial {

    @Override
    public GroupMaterial getGroup(String groupName) {
        return new GroupMaterial() {
            @Override
            public GroupMaterial getChild(String boneName) {
                return this;
            }

            @Override
            public MaterialEntry getCubeNode(int index) {
                return material;
            }
        };
    }
}
