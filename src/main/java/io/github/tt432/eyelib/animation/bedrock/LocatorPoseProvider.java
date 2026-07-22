package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * locator 世界位姿解析接口，由 bridge 层安装具体实现。
 *
 * @author TT432
 */
@FunctionalInterface
public interface LocatorPoseProvider {
    Matrix4f resolve(MolangScope scope, @Nullable String locatorName);
}
