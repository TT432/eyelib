package io.github.tt432.eyelib.common.bedrock.particle.pojo.curve;

import com.google.gson.annotations.SerializedName;

/**
 * @author DustW
 */
public enum CurveType {
    /**
     * linear is a series of nodes, equally spaced between 0 and 1 after applying input/horizontal_range
     */
    @SerializedName("linear")
    LINEAR,
    /**
     * bezier is a 4-node bezier spline, with the first and last point being the values at 0 and 1
     * and the middle two points forming the slope lines at 0.33 for the first point and 0.66 for the second
     */
    @SerializedName("bezier")
    BEZIER,
    /**
     * catmull_rom is a series of curves which pass through all but the last/first node.
     * The first/last nodes are used to form the slope of the second/second-last points respectively.
     * All points are evenly spaced.
     */
    @SerializedName("catmull_rom")
    CATMULL_ROM,
    /**
     * bezier_chain is a chain of bezier splines, which will be explained in a later section.
     * A series of points are specified, along with their corresponding slopes,
     * and each segment will use its pair of points and slopes to form a bezier spline.
     * Each point other than first/last is shared between its pair of spline segments.
     */
    @SerializedName("bezier_chain")
    BEZIER_CHAIN
}