package io.github.tt432.eyelib.util.event.api;

import java.lang.invoke.MethodHandle;
import java.util.List;

/**
 * 平台侧 {@link OnRenderStage} 方法发现接口，由 bridge 层用 (Neo)Forge 反射系统实现。
 *
 * @author TT432
 */
@FunctionalInterface
public interface RenderStageSubscriberDiscovery {
    List<RenderStageSubscriber> discover();

    /**
     * @param declaringClass 声明方法的类
     * @param methodName      方法名
     * @param handle          方法句柄（签名 {@code void(float, double, double, double)}）
     */
    record RenderStageSubscriber(
            Class<?> declaringClass,
            String methodName,
            MethodHandle handle
    ) {
    }
}
