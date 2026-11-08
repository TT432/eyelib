package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * locator 在**模型根空间**的位移解析接口（单位：块），由 bridge 层安装具体实现。
 *
 * <p>与 {@link LocatorPoseProvider} 的区别：不含实体世界位姿、朝向与实体缩放，
 * 只保留模型自身骨骼/locator 的累积变换——即 {@code query.get_root_locator_offset}
 * 需要的「模型根上该 locator 的偏移」。
 *
 * @author TT432
 */
@FunctionalInterface
public interface RootLocatorOffsetProvider {
    /**
     * 1 块 = 16 BE 模型位移单位（Bedrock 骨骼/locator 位移按 1/16 块记）。
     * provider 返回模型空间块；{@code query.get_root_locator_offset} 乘该系数回到 BE 单位。
     */
    float BLOCKS_TO_BEDROCK_UNITS = 16F;

    /** @return 模型根空间位移（块）；无模型或 locator 不存在 → {@code null} */
    @Nullable
    Vector3f resolve(MolangScope scope, String locatorName);
}
