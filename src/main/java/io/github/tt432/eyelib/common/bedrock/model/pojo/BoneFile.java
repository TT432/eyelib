package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.util.Value3;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class BoneFile implements Serializable {
    @SerializedName("bind_pose_rotation")
    private double[] bindPoseRotation;
    /**
     * This is the list of cubes associated with this bone.
     */
    private CubeFile[] cubes;
    private Boolean debug;
    /**
     * Grow this box by this additive amount in all directions (in model space units).
     */
    private Double inflate;
    /**
     * This is a list of locators associated with this bone. A locator is a point in
     * model space that tracks a particular bone as the bone animates (by
     * maintaining it's relationship to the bone through the animation).
     */
    private Map<String, Locator> locators;
    /**
     * Mirrors the UV's of the unrotated cubes along the x axis, also causes the
     * east/west faces to get flipped.
     */
    private boolean mirror;
    /**
     * Animation files refer to this bone via this identifier.
     */
    private String name;
    private Boolean neverRender;
    /**
     * Bone that this bone is relative to. If the parent bone moves, this bone will
     * move along with it.
     */
    private String parent;
    /**
     * The bone pivots around this point (in model space units).
     */
    private Value3 pivot;
    /**
     * This is the initial rotation of the bone around the pivot, pre-animation (in
     * degrees, x-then-y-then-z order).
     */
    private float[] rotation = new float[]{0, 0, 0};
}
