package io.github.tt432.eyelib.client.gui.snowstorm;

import io.github.tt432.eyelib.bridge.client.compat.ldlib.LdlibCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snowstorm 粒子编辑器 LDLib 可选前置门控（ADR-0036 D2，照 {@code NodegraphGate} 模式）。
 *
 * <p>本类不引用任何 LDLib 类型：Screen 实现类仅以「类名字符串」持有，
 * 确认前置存在后才经反射加载，保证未安装 LDLib 时主链路零 LDLib 类加载。
 */
public final class SnowstormEditorGate {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowstormEditorGate.class);

    private static final String SCREEN_IMPL_CLASS =
            "io.github.tt432.eyelib.client.gui.snowstorm.SnowstormEditorScreen";

    private SnowstormEditorGate() {
    }

    /** Snowstorm 编辑器是否可用（LDLib 已安装）。 */
    public static boolean isEditorAvailable() {
        return LdlibCompat.isLdlibLoaded();
    }

    /** 打开 Snowstorm 粒子编辑器（LDLib 缺失时告警并 no-op）。 */
    public static void openEditor() {
        if (!isEditorAvailable()) {
            LOGGER.warn("[snowstorm] editor unavailable: optional LDLib dependency is not installed");
            return;
        }
        try {
            Class.forName(SCREEN_IMPL_CLASS)
                    .getMethod("open")
                    .invoke(null);
        } catch (Exception | LinkageError e) {
            LOGGER.error("[snowstorm] failed to open editor via {}", SCREEN_IMPL_CLASS, e);
        }
    }
}
