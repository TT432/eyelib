package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * VertexConsumer 版本差异 Port，屏蔽 1.20.6 前后 vertex/addVertex API 差异。
 *
 * @author TT432
 */
public interface VertexConsumerPort {

    org.slf4j.Logger PROBE_LOG = org.slf4j.LoggerFactory.getLogger(VertexConsumerPort.class);

    static void vertex(VertexConsumer consumer,
                       float x, float y, float z,
                       float r, float g, float b, float a,
                       float u, float v, int overlay, int light,
                       float nx, float ny, float nz) {
        //? if <1.20.6 {
        consumer.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
        //?} else {
        if (Boolean.getBoolean("eyelib.ngRenderProbe")) {
            try {
                consumer.addVertex(x, y, z)
                        .setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255))
                        .setUv(u, v)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(nx, ny, nz);
                return;
            } catch (IllegalStateException e) {
                PROBE_LOG.error("[renderProbe] vertex failed on consumer {}@{}: {}",
                        consumer.getClass().getSimpleName(),
                        Integer.toHexString(System.identityHashCode(consumer)), e.toString());
                throw e;
            }
        }
        consumer.addVertex(x, y, z)
                .setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255))
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        //?}
    }
}
