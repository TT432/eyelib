package io.github.tt432.eyelib.bridge.client.render.adapter;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * 1.20.1 原版栓绳抑制标记。
 *
 * <p>该版本的原版栓绳在 {@code MobRenderer.render} 里绘制，位置在
 * {@code super.render(...)}（即 {@code RenderLivingEvent.Pre} 所在处）返回之后；
 * 事件被 eyelib 取消只会跳过模型，不会跳过随后的 {@code renderLeash}。
 * eyelib 自己接管后会再画一条，因此必须在 {@link MobRendererLeashMixin} 里
 * 把原版这一条抑制掉。
 *
 * <p>只用「本帧 eyelib 实际接管了该实体渲染」作为标记，不用
 * {@code useBuiltInRenderSystem} 之类的静态开关——资源包缺失导致接管失败时
 * 仍应保留原版栓绳。渲染线程单线程访问；同一实体渲染调用内 mark 后立刻 consume。
 *
 * @author TT432
 */
public final class LeashSuppression {
    private static @Nullable LivingEntity handled;

    private LeashSuppression() {
    }

    /** 供渲染适配器在取消事件后调用。只记录 Mob（原版只有 Mob 走 MobRenderer 的栓绳段）。 */
    public static void markHandled(LivingEntity entity) {
        handled = entity instanceof Mob ? entity : null;
    }

    /** 供 1.20.1 混入在 {@code MobRenderer.render} 的 super 调用之后调用。 */
    public static boolean consumeHandled(LivingEntity entity) {
        if (handled == entity) {
            handled = null;
            return true;
        }
        return false;
    }
}
