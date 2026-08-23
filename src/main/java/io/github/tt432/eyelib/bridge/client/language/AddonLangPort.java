package io.github.tt432.eyelib.bridge.client.language;

//? if <26.1
import io.github.tt432.eyelib.bridge.client.language.adapter.AddonLangBridge;

/**
 * AddonLangBridge Port —— 隔离 application 对 adapter 具体类的直接依赖。
 * 26.1 降级为 no-op（AddonLangPack 仅 &lt;26.1 实现）。
 */
public interface AddonLangPort {
    /** addon 语言文件替换后重载 LanguageManager，使新合成的 lang JSON 生效。 */
    static void triggerLangReload() {
        //? if <26.1
        AddonLangBridge.triggerLangReload();
    }
}
