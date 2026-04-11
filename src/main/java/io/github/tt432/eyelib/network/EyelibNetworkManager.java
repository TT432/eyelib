package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.EyelibNetworkTransport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibNetworkManager {
    public static void register() {
        EyelibNetworkTransport.register();
    }

    public static void sendToServer(Object packet) {
        EyelibNetworkTransport.sendToServer(packet);
    }
}
