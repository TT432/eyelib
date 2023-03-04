package io.github.tt432.eyelib.common.bedrock.model.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MinecraftGeometry implements Serializable {
    /**
     * Bones define the 'skeleton' of the mob: the parts that can be animated, and
     * to which geometry and other bones are attached.
     */
    private BoneFile[] bones;
    private String cape;
    private ModelProperties description;
}
