package io.github.tt432.eyelib.importer.addon;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 资源包设置的运行时解析服务：molang {@code query.*pack_setting*} 的数据源。
 * <p>
 * 加载侧（资源重载时）把当前启用包（优先级底→顶）的设置目录注册进来；
 * 查询按「顶→底」找第一个声明了该设置名的包取值——用户选择
 * （{@link BedrockPackSettingsStore}）优先，缺省回落 manifest 默认值。
 * 官方语义（Microsoft Learn）：resource pack 设置按玩家本地配置，经
 * {@code query.get_pack_setting}（slider）/ {@code query.is_pack_setting_enabled}（toggle）/
 * {@code query.is_pack_setting_selected}（dropdown）读取。
 * <p>
 * 已知近似：Bedrock 中设置按包隔离、表达式读到的是所属包的设置；这里按设置名
 * 跨包解析（设置名带命名空间，冲突概率低）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public final class BedrockPackSettingsService {

    /** 一个启用包的设置目录快照。 */
    public record ActivePack(String packKey, List<BedrockPackSetting> settings) {
        public ActivePack {
            settings = List.copyOf(settings);
        }
    }

    /** 当前启用包（优先级底→顶）。 */
    private static List<ActivePack> activePacks = List.of();

    private BedrockPackSettingsService() {
    }

    /** 资源重载时刷新启用包集合（优先级底→顶；空列表 = 无 Bedrock 包启用）。 */
    public static synchronized void updateActivePacks(List<ActivePack> packs) {
        activePacks = List.copyOf(packs);
    }

    /** {@code query.is_pack_setting_enabled(name)}：toggle 开 → 1。 */
    public static boolean isPackSettingEnabled(String settingName) {
        Resolved resolved = resolve(settingName, BedrockPackSetting.Type.TOGGLE);
        if (resolved == null) {
            return false;
        }
        return BedrockPackSettingsStore.toggleValue(resolved.packKey(), settingName)
                .orElse(resolved.setting().defaultBoolean());
    }

    /** {@code query.is_pack_setting_selected(name, selection)}：dropdown 当前值等于 selection → 1。 */
    public static boolean isPackSettingSelected(String settingName, String selection) {
        Resolved resolved = resolve(settingName, BedrockPackSetting.Type.DROPDOWN);
        if (resolved == null) {
            return false;
        }
        String current = BedrockPackSettingsStore.dropdownValue(resolved.packKey(), settingName)
                .orElse(resolved.setting().defaultOption());
        return current != null && current.equals(selection);
    }

    /** {@code query.get_pack_setting(name)}：slider 当前值。 */
    public static double packSettingValue(String settingName) {
        Resolved resolved = resolve(settingName, BedrockPackSetting.Type.SLIDER);
        if (resolved == null) {
            return 0;
        }
        return BedrockPackSettingsStore.sliderValue(resolved.packKey(), settingName)
                .map(resolved.setting()::snapToStep)
                .orElse(resolved.setting().clampedDefault());
    }

    private record Resolved(String packKey, BedrockPackSetting setting) {
    }

    /** 顶→底找第一个声明了该设置名且类型匹配的包。 */
    private static synchronized @Nullable Resolved resolve(String settingName,
                                                           BedrockPackSetting.Type expectedType) {
        for (int i = activePacks.size() - 1; i >= 0; i--) {
            for (BedrockPackSetting setting : activePacks.get(i).settings()) {
                if (setting.type() == expectedType && settingName.equals(setting.name())) {
                    return new Resolved(activePacks.get(i).packKey(), setting);
                }
            }
        }
        return null;
    }
}
