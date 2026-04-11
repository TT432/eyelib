package io.github.tt432.eyelib.mc.impl.util.time;

import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.core.util.time.FixedStepTimerState;
import net.minecraft.client.Minecraft;

/**
 * Minecraft-backed fixed-step timer adapter.
 */
public class FixedTimer {
    private final FixedStepTimerState state = new FixedStepTimerState(30);

    public int getRate() {
        return state.getRate();
    }

    public int getLastFixed() {
        return state.getLastFixed();
    }

    public boolean canNextStep() {
        return state.canNextStep(ClientTickHandler.getTick(), Minecraft.getInstance().timer.partialTick);
    }

    public void start() {
        state.start(ClientTickHandler.getTick(), Minecraft.getInstance().timer.partialTick);
    }

    public float realSec() {
        return state.realSeconds(ClientTickHandler.getTick(), Minecraft.getInstance().timer.partialTick);
    }

    public float seconds() {
        return state.seconds();
    }
}
