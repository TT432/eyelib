package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.Value2;

/**
 * @author DustW
 */
@ParticleComponentHolder("minecraft:particle_appearance_billboard")
public class ParticleAppearanceBillboardComponent extends ParticleComponent {
    /**
     * specifies the x/y size of the billboard
     * evaluated every frame
     */
    Value2 size;

    @SerializedName("face_camera_mode")
    FaceMode faceCameraMode;

    /**
     * 只在 faceCameraMode 为
     * DIRECTION_X,
     * DIRECTION_Y,
     * DIRECTION_Z,
     * LOOKAT_DIRECTION
     * 时生效
     */
    ABDirection direction;

    public enum FaceMode {
        /**
         * aligned to the camera, perpendicular to the view axis<p>
         * 与摄像机对齐，垂直于视轴
         */
        @SerializedName("rotate_xyz")
        ROTATE_XYZ,
        /**
         * aligned to camera, but rotating around world y axis<p>
         * 与摄像机对齐，但围绕世界y轴旋转
         */
        @SerializedName("rotate_y")
        ROTATE_Y,
        /**
         * aimed at the camera, biased towards world y up<p>
         * 瞄准摄像机，偏向于世界y轴向上
         */
        @SerializedName("lookat_xyz")
        LOOKAT_XYZ,
        /**
         * aimed at the camera, but rotating around world y axis<p>
         * 瞄准摄像机，但围绕世界Y轴旋转
         */
        @SerializedName("lookat_y")
        LOOKAT_Y,
        @SerializedName("lookat_direction")
        LOOKAT_DIRECTION,
        /**
         * unrotated particle x axis is along the direction vector, unrotated y axis attempts to aim upwards<p>
         * 未旋转的粒子X轴是沿着方向矢量的，未旋转的Y轴试图向上瞄准
         */
        @SerializedName("direction_x")
        DIRECTION_X,
        /**
         * unrotated particle y axis is along the direction vector, unrotated x axis attempts to aim upwards<p>
         * 未旋转的粒子Y轴沿方向矢量，未旋转的X轴试图向上瞄准。
         */
        @SerializedName("direction_y")
        DIRECTION_Y,
        /**
         * billboard face is along the direction vector, unrotated y axis attempts to aim upwards<p>
         * 广告牌面沿方向矢量，未旋转的y轴试图向上瞄准。
         */
        @SerializedName("direction_z")
        DIRECTION_Z,
        /**
         * orient the particles to match the emitter's transform (the billboard plane will match the transform's xy plane).<p>
         * 确定粒子的方向以匹配发射器的变换（广告牌平面将匹配变换的xy平面）。
         */
        @SerializedName("emitter_transform_xy")
        EMITTER_TRANSFORM_XY,
        /**
         * orient the particles to match the emitter's transform (the billboard plane will match the transform's xz plane).<p>
         * 使粒子的方向与发射器的变换相匹配（广告牌平面将与变换的xz平面相匹配）。
         */
        @SerializedName("emitter_transform_xz")
        EMITTER_TRANSFORM_XZ,
        /**
         * orient the particles to match the emitter's transform (the billboard plane will match the transform's yz plane).<p>
         * 使粒子的方向与发射器的变换相匹配（广告牌平面将与变换的yz平面相匹配）。
         */
        @SerializedName("emitter_transform_yz")
        EMITTER_TRANSFORM_YZ
    }
}
