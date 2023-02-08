package io.github.tt432.eyelib.common.bedrock.particle.pojo.curve;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import io.github.tt432.eyelib.util.EyelibLists;
import io.github.tt432.eyelib.util.molang.MolangValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author DustW
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParticleCurveNode extends EyelibLists.Node<ParticleCurveNode> {
    Timestamp timestamp;

    /**
     * the output of the curve
     */
    MolangValue value;
    /**
     * when curve comes from the left of the node, what point does it use?
     */
    @SerializedName("left_value")
    MolangValue leftValue;
    /**
     * when curve comes from the right side of the node, what point does it use?
     */
    @SerializedName("right_value")
    MolangValue rightValue;

    /**
     * the slope of the node, both sides
     */
    MolangValue slope;
    /**
     * the left slope of the node
     */
    @SerializedName("left_slope")
    MolangValue leftSlope;
    /**
     * the right slope of the node
     */
    @SerializedName("right_slope")
    MolangValue rightSlope;
}
