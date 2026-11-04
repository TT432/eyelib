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
 *
 * @author TT432
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnRenderStage {
}
