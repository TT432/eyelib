package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public interface RenderPorts {
    AtomicReference<@Nullable RenderPorts> HOLDER = new AtomicReference<>();

    EntityRenderPorts.RenderBufferPort renderBufferPort();
    EntityRenderPorts.RenderEntityPort renderEntityPort();
    EntityRenderPorts.SetupClientEntityPort setupClientEntityPort();
    EntityRenderPorts.RenderSystemPort renderSystemPort();

    static RenderPorts install(
            EntityRenderPorts.RenderBufferPort renderBufferPort,
            EntityRenderPorts.RenderEntityPort renderEntityPort,
            EntityRenderPorts.SetupClientEntityPort setupClientEntityPort
    ) {
        RenderPortsImpl ports = new RenderPortsImpl(renderBufferPort, renderEntityPort, setupClientEntityPort);
        HOLDER.set(ports);
        return ports;
    }

    static RenderPorts get() {
        RenderPorts ports = HOLDER.get();
        if (ports == null) {
            throw new IllegalStateException("RenderPorts not installed");
        }
        return ports;
    }
}

