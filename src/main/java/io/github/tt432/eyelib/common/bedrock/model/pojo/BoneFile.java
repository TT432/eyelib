package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class BoneFile {
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
	private Boolean mirror;
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
	private double[] pivot = new double[] { 0, 0, 0 };
	/**
	 * ***EXPERIMENTAL*** A triangle or quad mesh object. Can be used in conjunction
	 * with cubes and texture geometry.
	 */
	@SerializedName("poly_mesh")
	private PolyMesh polyMesh;
	@SerializedName("render_group_id")
	private Long renderGroupID;
	private Boolean reset;
	/**
	 * This is the initial rotation of the bone around the pivot, pre-animation (in
	 * degrees, x-then-y-then-z order).
	 */
	private double[] rotation = new double[] { 0, 0, 0 };
	/**
	 * ***EXPERIMENTAL*** Adds a mesh to the bone's geometry by converting texels in
	 * a texture into boxes.
	 */
	@SerializedName("texture_meshes")
	private TextureMesh[] textureMeshes;
}
