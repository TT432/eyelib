package io.github.tt432.eyelib.common.bedrock.model.pojo;

import lombok.Data;

@Data
public class CubeFile {
    /**
     * Grow this box by this additive amount in all directions (in model space
     * units), this field overrides the bone's inflate field for this cube only.
     */
    private Double inflate;
	/**
	 * Mirrors this cube about the unrotated x axis (effectively flipping the east /
	 * west faces), overriding the bone's 'mirror' setting for this cube.
	 */
    private Boolean mirror;
	/**
	 * This point declares the unrotated lower corner of cube (smallest x/y/z value
	 * in model space units).
	 */
    private double[] origin = new double[]{0, 0, 0};
	/**
	 * If this field is specified, rotation of this cube occurs around this point,
	 * otherwise its rotation is around the center of the box.
	 */
    private double[] pivot = new double[]{0, 0, 0};
	/**
	 * The cube is rotated by this amount (in degrees, x-then-y-then-z order) around
	 * the pivot.
	 */
    private double[] rotation = new double[]{0, 0, 0};
	/**
	 * The cube extends this amount relative to its origin (in model space units).
	 */
    private double[] size = new double[]{1, 1, 1};
    private UvUnion uv;
}
