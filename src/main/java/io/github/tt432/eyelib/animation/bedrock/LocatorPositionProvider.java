package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * locator 世界坐标解析接口，由 bridge 层安装具体实现。
 *
 * @author TT432
 */
@FunctionalInterface
public interface LocatorPositionProvider {
    Vector3f resolve(MolangScope scope, @Nullable String locatorName);
}
