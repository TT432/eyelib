package io.github.tt432.eyelib.importer.addon;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Bedrock fog 资产注册表（随 BedrockAddonRuntimeBridge 整体替换）：
 * fogs/*.json 解析出的「fog identifier → BrFog」。消费方：FogBridge 每帧把激活 fog 的
 * distance.air 应用到主渲染雾（Bedrock Active Fog Stack 的最小近似——Command/Biome 层
 * 暂不支持，见 FogBridge 注释）。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FogAssetRegistry {
    private static Map<String, BrFog> fogs = Map.of();
    private static @Nullable String activeFogId;

    /**
     * 随 replaceFromResourcePack 一并替换（语义与 SoundAssetRegistry.stageSounds 一致）。
     * 默认激活规则：替换后注册表恰有一个定义时自动激活它，否则不激活
     * （多个定义时由消费方显式 {@link #setActiveFog} 选择，避免任意挑一个的不可预期行为）。
     */
    public static void stageFogs(Map<String, BrFog> fogsById) {
        fogs = Map.copyOf(fogsById);
        activeFogId = fogs.size() == 1 ? fogs.keySet().iterator().next() : null;
    }

    /** 当前激活的 fog 定义（未激活或激活 id 已不在注册表时为空）。 */
    public static Optional<BrFog> activeFog() {
        return activeFogId == null ? Optional.empty() : Optional.ofNullable(fogs.get(activeFogId));
    }

    /** 当前激活 fog 的 identifier。 */
    public static Optional<String> activeFogId() {
        return Optional.ofNullable(activeFogId);
    }

    /** 显式激活某个 fog 定义；id 未注册时抛 {@link IllegalArgumentException}。 */
    public static void setActiveFog(String id) {
        if (!fogs.containsKey(id)) {
            throw new IllegalArgumentException("unknown fog id: " + id);
        }
        activeFogId = id;
    }

    /** 清除激活状态（之后完全不碰 vanilla 雾）。 */
    public static void clearActiveFog() {
        activeFogId = null;
    }
}
