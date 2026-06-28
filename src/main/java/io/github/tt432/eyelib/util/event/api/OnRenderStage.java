package io.github.tt432.eyelib.util.event.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法处理渲染阶段事件（对应 Forge {@code RenderLevelStageEvent}）。
 * 方法签名必须是 {@code void(float partialTick, double camX, double camY, double camZ)}，
 * 参数类型仅 Application 级，不引用 Forge / MC 事件类。
 *
 * @author TT432
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnRenderStage {
}
