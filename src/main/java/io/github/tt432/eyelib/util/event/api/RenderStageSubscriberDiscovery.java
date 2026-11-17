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
     * @param priority        调度优先级，数值小的先执行（见 {@link RenderStagePriority}）
     */
    record RenderStageSubscriber(
            Class<?> declaringClass,
            String methodName,
            MethodHandle handle,
            int priority
    ) {
        /**
         * 兼容构造：未指定优先级时按 {@link RenderStagePriority#CORE}（主管道档）处理。
         *
         * @param declaringClass 声明方法的类
         * @param methodName      方法名
         * @param handle          方法句柄（签名 {@code void(float, double, double, double)}）
         */
        public RenderStageSubscriber(Class<?> declaringClass, String methodName, MethodHandle handle) {
            this(declaringClass, methodName, handle, RenderStagePriority.CORE);
        }
    }
}
