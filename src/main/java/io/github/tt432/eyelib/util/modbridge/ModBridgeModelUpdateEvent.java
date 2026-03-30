package io.github.tt432.eyelib.util.modbridge;

import lombok.AllArgsConstructor;
import net.minecraftforge.eventbus.api.Event;

/**
 * fire on client side.
 *
 * @author TT432
 */
@AllArgsConstructor
public class ModBridgeModelUpdateEvent extends Event {
    public final String json;
}
