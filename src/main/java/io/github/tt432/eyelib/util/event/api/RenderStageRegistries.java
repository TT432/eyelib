package io.github.tt432.eyelib.util.event.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 渲染阶段注册表 holder，bridge 层通过此 holder 访问注册表，不直接依赖 Application 层。
 * 默认持有 {@link DefaultRegistry}（no-op），bridge {@code *LifecycleHooks} 调
 * {@link #setupRenderStage} 替换为实际装配结果。
 *
 * @author TT432
 */
public final class RenderStageRegistries {
    private static volatile RenderStageSubscriberRegistry renderStage = new DefaultRegistry();

    private RenderStageRegistries() {
    }

    /**
     * 注册渲染阶段订阅者路由表实例。由 bridge {@code *LifecycleHooks} 在 setup 后调用。
     */
    public static void register(RenderStageSubscriberRegistry registry) {
        renderStage = registry;
    }

    /**
     * @return 当前渲染阶段订阅者路由表
     */
    public static RenderStageSubscriberRegistry renderStage() {
        return renderStage;
    }

    /**
     * 用指定发现器装配默认实现并注册到 holder。bridge {@code *LifecycleHooks} 调用此方法。
     */
    public static void setupRenderStage(RenderStageSubscriberDiscovery discovery) {
        DefaultRegistry impl = new DefaultRegistry();
        impl.setup(discovery);
        renderStage = impl;
    }

    private static final class DefaultRegistry implements RenderStageSubscriberRegistry {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegistry.class);

        private final CopyOnWriteArrayList<MethodHandle> handles = new CopyOnWriteArrayList<>();

        @Override
        public void setup(RenderStageSubscriberDiscovery discovery) {
            handles.clear();
            for (RenderStageSubscriberDiscovery.RenderStageSubscriber subscriber : discovery.discover()) {
                handles.add(subscriber.handle());
            }
            LOGGER.info("[RenderStage] {} subscriber(s) registered", handles.size());
        }

        @Override
        public void dispatch(float partialTick, double camX, double camY, double camZ) {
            for (MethodHandle handle : handles) {
                try {
                    handle.invoke(partialTick, camX, camY, camZ);
                } catch (Throwable e) {
                    LOGGER.error("[RenderStage] dispatch failed for subscriber", e);
                }
            }
        }

        @Override
        public int subscriberCount() {
            return handles.size();
        }
    }
}
