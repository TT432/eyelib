package io.github.tt432.eyelib.bridge.client.sound;

//? if <26.1
import io.github.tt432.eyelib.bridge.client.sound.adapter.AddonSoundBridge;

/**
 * AddonSoundBridge Port —— 隔离 application 对 adapter 具体类的直接依赖。
 * 26.1 降级为 no-op（AddonSoundPack 预览播放仅 <26.1 实现）。
 */
public interface AddonSoundPort {
    /** 预览播放 bedrock 音效 id（addon 音效包 sounds.json 现场合成；未定义 id 静默忽略）。 */
    static void playPreview(String soundId) {
        //? if <26.1
        AddonSoundBridge.playPreview(soundId);
    }

    /** addon 资产替换后局部重建 SoundManager，使新合成的 sounds.json 生效。 */
    static void triggerSoundReload() {
        //? if <26.1
        AddonSoundBridge.triggerSoundReload();
    }
}
