package io.github.tt432.eyelib.client.render;

import java.util.List;

/**
 * 挂载声明源（G3）：每帧对本宿主的模型声明若干挂载。
 * <p>
 * 调用点固定为「模型几何提交之前、每实体每帧一次」（嵌套绘制不触发，见
 * {@link SimpleRenderAction.Builder#hostBody(boolean)}）。实现必须：
 * <ul>
 *   <li>不缓存 {@link RenderFrame}（跨帧持有会持有位姿与 sink，26.1 下 sink 的生命周期只有一次提交窗口）；</li>
 *   <li>不依赖调用顺序（同一帧内多个源的顺序由注册序决定，不是契约）；</li>
 *   <li>用 {@link RenderFrame#handledSlots()} 判断「库是否已经画了这个槽位」，不要按骨骼名反推。</li>
 * </ul>
 * 异常会被隔离：一个源抛错只跳过该源本帧的声明并计入诊断，不影响其它源与本体渲染。
 *
 * @author TT432
 */
@FunctionalInterface
public interface AttachmentSource {
    List<AttachmentDeclaration> declare(RenderFrame frame);
}
