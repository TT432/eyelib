package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.EntityRenderPorts;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 渲染 Port 实例容器，由 wiring 阶段 {@link #install()} 一次后运行时只读。
 * adapter 子包内的实现类，被 ArchUnit 规则 8 排除。
 *
 * @author TT432
 */
public final class RenderPorts {
    @Nullable
    private static volatile RenderPorts instance;

    public EntityRenderPorts.RenderBufferPort renderBufferPort =
            (p, x, y, z, ps, bs) -> {};
    public EntityRenderPorts.RenderEntityPort renderEntityPort = params -> false;
    public EntityRenderPorts.SetupClientEntityPort setupClientEntityPort = e -> List.of();

    private RenderPorts() {
    }

    public static RenderPorts install() {
        RenderPorts ports = new RenderPorts();
        instance = ports;
        return ports;
    }

    public static RenderPorts get() {
        RenderPorts ports = instance;
        if (ports == null) {
            throw new IllegalStateException("RenderPorts not installed");
        }
        return ports;
    }
}
