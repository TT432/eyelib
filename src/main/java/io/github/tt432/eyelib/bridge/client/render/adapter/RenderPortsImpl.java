package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;

public final class RenderPortsImpl implements RenderPorts {
    private final EntityRenderPorts.RenderEntityPort renderEntityPort;
    private final EntityRenderPorts.SetupClientEntityPort setupClientEntityPort;
    private final EntityRenderPorts.RenderSystemPort renderSystemPort;

    RenderPortsImpl(
            EntityRenderPorts.RenderEntityPort renderEntityPort,
            EntityRenderPorts.SetupClientEntityPort setupClientEntityPort
    ) {
        this.renderEntityPort = renderEntityPort;
        this.setupClientEntityPort = setupClientEntityPort;
        this.renderSystemPort = new EntityRenderSystem();
    }

    @Override
    public EntityRenderPorts.RenderEntityPort renderEntityPort() { return renderEntityPort; }

    @Override
    public EntityRenderPorts.SetupClientEntityPort setupClientEntityPort() { return setupClientEntityPort; }

    @Override
    public EntityRenderPorts.RenderSystemPort renderSystemPort() { return renderSystemPort; }
}
