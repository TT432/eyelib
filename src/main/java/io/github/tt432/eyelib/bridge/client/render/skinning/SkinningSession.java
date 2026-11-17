package io.github.tt432.eyelib.bridge.client.render.skinning;

import io.github.tt432.eyelib.bridge.client.render.bake.BakedModel;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * GPU 蒙皮会话（C1，ADR-0032）：一次实体×组件提交的骨骼调色板采集通道。
 *
 * <p>调用序列（渲染线程，{@code HighSpeedRenderModelVisitor} 驱动）：
 * <ol>
 *   <li>{@link #begin}（visitPreModel）：决定是否蒙皮；false → 调用方回退经典 CPU 路径，
 *       本对象后续调用不再发生。</li>
 *   <li>{@link #appendBone}（visitPreBone，每个有几何的骨骼）：写入该骨骼的世界矩阵与法线阵。</li>
 *   <li>{@link #markVisible}（visitPreBone，骨骼可见时）：该骨骼参与本次绘制。</li>
 * </ol>
 * 会话的 palette 上传与绘制由实现方在 writer 回调结束后完成，不属于本契约。
 *
 * <p>26.1.2 实现见 {@code adapter/NgSkinningSession}；&le;26.1（1.20.1/1.21.1）实现见
 * {@code adapter/LegacySkinningSession}（P2，DESIGN-P2）。
 */
public interface SkinningSession {

    /**
     * 开始一个实体×组件的蒙皮采集。
     *
     * @param model      烘焙模型（骨骼→palette slot 映射、骨骼容量判定的依据）
     * @param tintColor  实体色调（null = 白色），逐分量 clamp [0,1] 由实现方完成
     * @param overlay    packed overlay 坐标（vanilla UV1 语义）
     * @param light      packed light 坐标（vanilla UV2 语义）
     * @return true = 走 GPU 蒙皮；false = 回退经典 CPU 顶点变换路径
     */
    boolean begin(BakedModel model, float @Nullable [] tintColor, int overlay, int light);

    /**
     * 写入一根有几何的骨骼的当帧世界矩阵与法线阵（CPU 合成的同一对象，含实体姿态）。
     * 无论骨骼可见与否都会调用（palette slice 需连续覆盖全部 slot）。
     */
    void appendBone(int boneId, Matrix4f pose, Matrix3f normal);

    /** 标记骨骼本次可见（其索引区间进入绘制集）。 */
    void markVisible(int boneId);

    /**
     * 静态实例通道入口（ADR-0048）：本次提交的模型没有任何动画姿态，几何为 bind 姿态常量。
     * <p>
     * 实现返回 true 时调用方<b>不再写顶点</b>、也不再调用 {@link #appendBone}/{@link #markVisible}：
     * 绘制数据只有实例级 root 位姿 + tint/overlay/light + 可见性分段，由实现方在 writer 结束后
     * 以实例化（≤26.1）或逐 draw uniform（26.1.2）接管。
     * <p>
     * 默认实现返回 false（该实现没有静态通道），调用方按 {@link #begin} 走常规蒙皮。
     *
     * @param bakedModel     烘焙几何（摊平后即为静态网格）
     * @param model          bind 骨骼树（摊平需要骨骼链 bind 变换；与 bakedModel 同源）
     * @param rootPose       提交时的实体根位姿（与蒙皮 palette 中的根姿态同空间）
     * @param rootNormal     与 rootPose 同源的法线阵
     * @param partVisibility 骨骼 id → 是否可见（缺省可见）
     * @param tintColor      实体色调（null = 白色）
     * @param overlay        packed overlay 坐标
     * @param light          packed light 坐标
     * @return true = 本次走静态实例通道；false = 回退常规路径
     */
    default boolean beginStatic(BakedModel bakedModel, io.github.tt432.eyelib.model.Model model,
                                org.joml.Matrix4fc rootPose, org.joml.Matrix3fc rootNormal,
                                it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap partVisibility,
                                float @Nullable [] tintColor, int overlay, int light) {
        return false;
    }

    /** 本次会话是否已进入静态实例通道（{@link #beginStatic} 返回 true）。 */
    default boolean isStatic() {
        return false;
    }
}
