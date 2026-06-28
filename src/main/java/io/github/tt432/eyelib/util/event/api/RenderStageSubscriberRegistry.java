package io.github.tt432.eyelib.util.event.api;

/**
 * 渲染阶段订阅者路由表接口，由 domain 层定义、bridge 层通过 {@link RenderStageRegistries} 访问。
 * bridge adapter 订阅 Forge 游戏事件，翻译参数后调 {@link #dispatch} 回调 Application 注解方法。
 *
 * @author TT432
 */
public interface RenderStageSubscriberRegistry {
    /**
     * 装配订阅者路由表。由 bridge {@code *LifecycleHooks} 在 {@code FMLCommonSetupEvent} 中调用一次。
     *
     * @param discovery 平台侧发现实现
     */
    void setup(RenderStageSubscriberDiscovery discovery);

    /**
     * 分发渲染阶段事件到所有订阅者。单个订阅者抛出异常不影响其他订阅者。
     */
    void dispatch(float partialTick, double camX, double camY, double camZ);

    /**
     * @return 已注册的订阅者数量
     */
    int subscriberCount();
}
