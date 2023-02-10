package io.github.tt432.eyelib.common.bedrock.particle.component.particle.pojo;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.util.Value2;
import io.github.tt432.eyelib.util.molang.MolangValue;

/**
 * alternate way via specifying a flipbook animation
 * a flipbook animation uses pieces of the texture to animate, by stepping over time from one
 * "frame" to another
 *
 * @author DustW
 */
public class ABFlipbook {
    /**
     * upper-left corner of starting UV patch
     */
    @SerializedName("base_UV")
    Value2 start;
    /**
     * size of UV patch
     */
    @SerializedName("size_UV")
    Value2 size;
    /**
     * how far to move the UV patch each frame
     */
    @SerializedName("step_UV")
    Value2 step;
    /**
     * default frames per second
     */
    @SerializedName("frames_per_second")
    double fps;
    /**
     * maximum frame number, with first frame being frame 1
     */
    @SerializedName("max_frame")
    MolangValue maxFrame;
    /**
     * optional, adjust fps to match lifetime of particle. default=false
     */
    @SerializedName("stretch_to_lifetime")
    boolean stretchToLifetime;
    /**
     * optional, makes the animation loop when it reaches the end? default=false
     */
    boolean loop;
}
