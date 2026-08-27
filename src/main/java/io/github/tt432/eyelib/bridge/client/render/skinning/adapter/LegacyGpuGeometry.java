//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

/**
 * Legacy（≤26.1）蒙皮几何的公共视图：VS 蒙皮（{@link LegacySkinnedGeometry}）与
 * compute 蒙皮（{@link ComputeSkinnedGeometry}）两种实现对 {@link LegacySkinningSession} 透明。
 */
interface LegacyGpuGeometry extends AutoCloseable {
    /** 骨骼 id → palette slot；无几何骨骼返回 -1。 */
    int slotOf(int boneId);

    int slotCount();

    @Override
    void close();
}
//?}
