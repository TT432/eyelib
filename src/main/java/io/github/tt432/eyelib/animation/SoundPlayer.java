package io.github.tt432.eyelib.animation;

import org.jspecify.annotations.NullMarked;

/**
 * 声音播放端口。Animation 模块通过此接口播放声音，具体实现由 MC 侧提供。
 *
 * @author TT432
 */
@NullMarked
@FunctionalInterface
public interface SoundPlayer {
    void playSound(String soundId, double x, double y, double z, float volume, float pitch);
}
