package io.github.tt432.eyelib.mc.impl.modbridge;

import net.minecraftforge.eventbus.api.Event;

/**
 * fire on client side.
 *
 * @author TT432
 */
public class ModBridgeModelUpdateEvent extends Event {
    public final String json;

    public ModBridgeModelUpdateEvent(String json) {
        this.json = json;
    }
}
