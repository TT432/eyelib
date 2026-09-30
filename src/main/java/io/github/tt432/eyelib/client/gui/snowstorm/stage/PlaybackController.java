package io.github.tt432.eyelib.client.gui.snowstorm.stage;

import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;

/**
 * Preview.vue {@code View.PlaybackController} as-is 移植（播放控制）。
 * EditorRuntime.Emitter 为非 final 静态（resetForTesting 可重建），每次调用现取。
 */
public final class PlaybackController {

    private PlaybackController() {
    }

    /** JS start()：{@code if (!Emitter.initialized || Emitter.age == 0) Emitter.start(); Emitter.paused = false;}。 */
    public static void start() {
        Emitter emitter = EditorRuntime.Emitter;
        if (!emitter.initialized || emitter.age == 0) {
            emitter.start();
        }
        emitter.paused = false;
    }

    /** JS toggle()。 */
    public static void toggle() {
        Emitter emitter = EditorRuntime.Emitter;
        emitter.paused = !emitter.paused;
        if (!emitter.paused) {
            start();
        }
    }

    /** JS stop()：{@code Emitter.stop(true); Emitter.paused = true;}。 */
    public static void stop() {
        Emitter emitter = EditorRuntime.Emitter;
        emitter.stop(true);
        emitter.paused = true;
    }

    /** Preview.vue startAnimation()：{@code PlaybackController.stop().start()}（播放按钮 / 空格）。 */
    public static void startAnimation() {
        stop();
        start();
    }

    /** Preview.vue togglePause()（暂停按钮 / Ctrl+空格）。 */
    public static void togglePause() {
        toggle();
    }
}
