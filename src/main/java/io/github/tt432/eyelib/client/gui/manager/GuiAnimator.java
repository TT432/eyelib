package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.util.math.MathHelper;

/**
 * @author TT432
 */
public final class GuiAnimator {
    public final int animTime;
    private float startStamp, timer, timerStamp;
    private boolean fadeIn;

    public GuiAnimator(int animTime) {
        this.animTime = animTime;
    }

    public final float getTime(int tick, float partialTicks, boolean state) {
        float time = tick + partialTicks;

        if (state) {
            if (!fadeIn) {
                fadeIn = true;
                timerStamp = timer;
                startStamp = time;
            }
        } else if (fadeIn) {
            fadeIn = false;
            timerStamp = timer;
            startStamp = time;
        }

        timer = MathHelper.clamp(timerStamp + (fadeIn ? 1 : -1) * (time - startStamp), 0, animTime);
        return MathHelper.clamp(timer / animTime, 0, 1);
    }
}
