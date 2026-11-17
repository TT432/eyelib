package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;

/**
 * 一个烘焙预热目标（G8）。
 * <p>
 * 键 = {@code (model, texture, meshTexture)}，**不含** {@code isSolid}：{@code twoSideWarmAsync}
 * 一次烘两个变体（solid / 非 solid），只有查询键带 {@code isSolid}。把 {@code isSolid} 放进枚举会让
 * 同一组合重复出现两次，而飞行中请求会去重，收益为零。
 *
 * @param model       模型
 * @param texture     图层纹理（渲染门控实际使用的那个）
 * @param meshTexture 体素化贴图（无 mesh 时等于 {@code texture}）
 * @author TT432
 */
public record BakeTarget(Model model, PortResourceLocation texture, PortResourceLocation meshTexture) {
}
