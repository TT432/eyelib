package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.bridge.client.compat.ldlib.LdlibCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDLib 加载门控（规格 D7；LDLib2 自 ADR-0030 起 jarJar 内嵌，正常恒可开）。
 *
 * <p>本类不引用任何 LDLib 类型：编辑器实现类仅以「类名字符串」持有，
 * 确认前置存在后才经反射加载，保证未安装 LDLib 时主链路零 LDLib 类加载。
 *
 * @author TT432
 */
public final class NodegraphGate {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodegraphGate.class);

    // 全版本统一 LDLib2 编辑器（ldlib1 已于 2026-08-07 退役，见 docs/decisions/0028）
    private static final String EDITOR_IMPL_CLASS =
            "io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.Ldlib2NodegraphEditor";

    private NodegraphGate() {
    }

    /** 节点图编辑器是否可用（LDLib2 已加载——内嵌发布，正常恒 true）。 */
    public static boolean isEditorAvailable() {
        return LdlibCompat.isLdlibLoaded();
    }

    /**
     * 打开节点图编辑器。
     *
     * @param libraryName 要编辑的图文档库名（{@link GraphLibraryManager} 的键）；null = 新建
     */
    public static void openEditor(@org.jspecify.annotations.Nullable String libraryName) {
        if (!isEditorAvailable()) {
            LOGGER.warn("[nodegraph] editor unavailable: LDLib2 (expected to be jarJar-embedded) is not loaded");
            return;
        }
        try {
            Class.forName(EDITOR_IMPL_CLASS)
                    .getMethod("open", String.class)
                    .invoke(null, new Object[]{libraryName});
        } catch (Exception | LinkageError e) {
            LOGGER.error("[nodegraph] failed to open editor via {}", EDITOR_IMPL_CLASS, e);
        }
    }
}
