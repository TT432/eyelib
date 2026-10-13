package io.github.tt432.eyelib.bridge.client.fog;

import io.github.tt432.eyelib.importer.addon.FogAssetRegistry;

import java.util.Optional;

/**
 * Fog 域 Port —— application 侧对 bedrock fog 激活状态的唯一入口，隔离对
 * adapter（FogBridge）具体类的直接依赖。与 AddonSoundPort 不同：fog 三版本均生效
 * （每帧经各版本 ViewportEvent 应用，无 26.1 降级），故 Port 无版本门控。
 *
 * @author TT432
 */
public interface FogPort {
    /** 显式激活某个已加载的 fog 定义（id 未注册抛 IllegalArgumentException）。 */
    static void setActiveFog(String id) {
        FogAssetRegistry.setActiveFog(id);
    }

    /** 清除激活状态（之后完全不碰 vanilla 雾）。 */
    static void clearActiveFog() {
        FogAssetRegistry.clearActiveFog();
    }

    /** 当前激活 fog 的 identifier（默认激活规则见 FogAssetRegistry.stageFogs）。 */
    static Optional<String> activeFogId() {
        return FogAssetRegistry.activeFogId();
    }
}
