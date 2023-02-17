package io.github.tt432.eyelib.common.bedrock.particle.component.particle.pojo;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.util.Value2;
import io.github.tt432.eyelib.util.math.Vec2d;
import io.github.tt432.eyelib.util.math.Vec4d;

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

    public Vec4d getUv(MolangVariableScope scope, int width, int height) {
        Vec2d startVec = start.evaluate(scope);
        Vec2d sizeVec = size.evaluate(scope);

        int maxFrameNum = (int) Math.floor(maxFrame.evaluate(scope));
        double age = scope.getValue("variable.particle_age");
        double lifetime = scope.getValue("variable.particle_lifetime");
        int aFps = (int) Math.floor(stretchToLifetime ? maxFrameNum / lifetime : fps);

        int currFrame = (int) Math.floor(age * aFps);

        if (currFrame > maxFrameNum && loop)
            currFrame = currFrame % maxFrameNum;

        Vec2d stepValue = step.evaluate(scope);

        double xOffset = currFrame * stepValue.getX();
        double yOffset = currFrame * stepValue.getY();

        return new Vec4d(
                (xOffset + startVec.getX()) / width,
                (xOffset + startVec.getX() + sizeVec.getX()) / width,
                (yOffset + startVec.getY()) / height,
                (yOffset + startVec.getY() + sizeVec.getY()) / height
        );
    }
}
