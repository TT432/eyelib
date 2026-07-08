//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.compat.ar.adapter;

/**
 * AR 渲染所需的骨骼顶点数据快照。从 BakedBone 提取原始数组，
 * 避免 adapter 直接依赖 application 层的 BakedBone 类型。
 * <p>
 * record equals/hashCode 基于字段引用：同一 BakedBone 的 position/u/v/normal 数组引用不变，
 * 因此跨帧 cache 语义与原 BakedBone-key 等价。
 *
 * @author TT432
 */
public record ARBoneData(float[] positions, float[] u, float[] v, float[] normals, int vertexSize) {
}
//?}
