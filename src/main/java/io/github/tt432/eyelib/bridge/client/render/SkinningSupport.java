package io.github.tt432.eyelib.bridge.client.render;

/**
 * GPU 蒙皮可用性：{@link RenderSink#skinningSupport} 的返回类型，用于在提交前判断
 * 「这次提交能否走 GPU 蒙皮」，以及不可用时给出原因。
 *
 * <p><b>{@link #AVAILABLE} 只是必要条件</b>：即使返回 AVAILABLE，模型维度仍可能拒绝蒙皮
 * （顶点全零、VS 路径超骨骼上限、几何尚未缓存等），writer 必须继续按
 * {@code GeometryWriter} 契约在会话 {@code begin} 失败时回退 CPU 顶点写入。
 *
 * <p>三节点语义表：
 * <table border="1">
 *   <caption>skinningSupport 各节点返回条件</caption>
 *   <tr><th>节点</th><th>返回</th><th>条件</th></tr>
 *   <tr><td rowspan="6">&lt;26.1（{@code ImmediateRenderSink}）</td>
 *       <td>{@link #DISABLED_BY_BATCH}</td><td>批次策略 {@code allowGpuSkinning=false}</td></tr>
 *   <tr><td>{@link #OFF_BY_FLAG}</td><td>{@code -Deyelib.gpuSkinning=false} 或蒙皮路径尚未选定（OFF）</td></tr>
 *   <tr><td>{@link #SHADER_PACK_ACTIVE}</td><td>Oculus/Iris 光影激活且未强制 {@code -Deyelib.gpuSkinning.mode}</td></tr>
 *   <tr><td>{@link #PASS_NOT_SKINNABLE}</td><td>该 RenderType 未登记蒙皮变体（identity 查询失败）</td></tr>
 *   <tr><td>{@link #NO_ENGINE_PATH}</td><td>着色器未注册（资源重载窗口）或 compute 批次程序缺失</td></tr>
 *   <tr><td>{@link #AVAILABLE}</td><td>以上皆不成立</td></tr>
 *   <tr><td rowspan="4">&gt;=26.1（{@code DeferredRenderSink}）</td>
 *       <td>{@link #DISABLED_BY_BATCH}</td><td>批次策略 {@code allowGpuSkinning=false}</td></tr>
 *   <tr><td>{@link #OFF_BY_FLAG}</td><td>{@code -Deyelib.gpuSkinning=false}</td></tr>
 *   <tr><td>{@link #OUTSIDE_STAGE_WINDOW}</td><td>不在 {@code LevelRenderer} 提交→阶段 flush 窗口内且非 FBO 场景</td></tr>
 *   <tr><td>{@link #SHADER_PACK_ACTIVE}</td><td>Oculus/Iris 光影激活</td></tr>
 * </table>
 *
 * @author TT432
 */
public enum SkinningSupport {
    /** 引擎路径可用（模型维度仍可能拒绝）。 */
    AVAILABLE,
    /** 被 {@code -Deyelib.gpuSkinning=false} 关闭，或蒙皮路径尚未初始化。 */
    OFF_BY_FLAG,
    /** 该节点没有可用的蒙皮引擎路径（着色器未注册 / compute 批次程序缺失）。 */
    NO_ENGINE_PATH,
    /** 光影包（Oculus/Iris）激活，GPU 蒙皮会与光影管线冲突。 */
    SHADER_PACK_ACTIVE,
    /** 26.1：不在实体提交后的阶段 flush 窗口内，记录的绘制不会被刷出。 */
    OUTSIDE_STAGE_WINDOW,
    /** 该 RenderType 没有蒙皮变体（<26.1 按 identity 查询失败）。 */
    PASS_NOT_SKINNABLE,
    /** 当前批次策略禁止 GPU 蒙皮（{@link BatchPolicy#allowGpuSkinning()} 为 false）。 */
    DISABLED_BY_BATCH
}
