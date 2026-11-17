package io.github.tt432.eyelib.util.event.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法处理逐帧渲染阶段（每帧一次，在实体渲染之前、且早于 Iris/Oculus 的 shadow pass）。
 * 方法签名必须是 {@code void(float partialTick, double camX, double camY, double camZ)}，
 * 参数类型仅 Application 级，不引用 Forge / MC 事件类。
 * <p>
 * 时机说明见 {@code RenderStageEventAdapter}：必须早于 shadow pass，否则 shadow map 使用上一帧
 * 的实体姿态，主渲染面会被自身上一帧轮廓遮蔽而整片发黑。
 * <p>
 * {@link #priority()} 只重排<b>同一次 dispatch 内</b>的顺序；「必须在 X 之后」这类语义依赖唯一可靠的
 * 表达是与 X 取不同档（见 {@link RenderStagePriority}），跨 mod 依赖请选相距 ≥100 的锚点。
 * 同一优先级内按（声明类全名，方法名）排序，与 mod 装载顺序无关。该注解的整体时机契约不变：
 * 仍然每帧一次、早于 shadow pass（ADR-0038）。
 *
 * @author TT432
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnRenderStage {
    /**
     * 同类 dispatch 内的调度优先级，数值小的先执行。默认 {@link RenderStagePriority#CORE}（eyelib 主管道档）。
     *
     * @return 调度优先级
     */
    int priority() default RenderStagePriority.CORE;
}
