package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * ***EXPERIMENTAL*** A triangle or quad mesh object. Can be used in conjunction
 * with cubes and texture geometry.
 */
@Data
public class PolyMesh {
	/**
	 * If true, UVs are assumed to be [0-1]. If false, UVs are assumed to be
	 * [0-texture_width] and [0-texture_height] respectively.
	 */
	@SerializedName("normalized_uvs")
	private boolean normalizedUvs;
	/**
	 * Vertex normals. Can be either indexed via the "polys" section, or be a
	 * quad-list if mapped 1-to-1 to the positions and UVs sections.
	 */
	private double[] normals;
	private PolysUnion polys;
	/**
	 * Vertex positions for the mesh. Can be either indexed via the "polys" section,
	 * or be a quad-list if mapped 1-to-1 to the normals and UVs sections.
	 */
	private double[] positions;
	/**
	 * Vertex UVs. Can be either indexed via the "polys" section, or be a quad-list
	 * if mapped 1-to-1 to the positions and normals sections.
	 */
	private double[] uvs;
}
