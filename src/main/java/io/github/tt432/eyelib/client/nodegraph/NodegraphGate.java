package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.bridge.client.compat.ldlib.LdlibCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDLib 可选前置门控（规格 D7）。
 *
 * <p>本类不引用任何 LDLib 类型：编辑器实现类仅以「类名字符串」持有，
 * 确认前置存在后才经反射加载，保证未安装 LDLib 时主链路零 LDLib 类加载。
 *
 * @author TT432
 */
public final class NodegraphGate {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodegraphGate.class);

    //? if <1.20.6 {
    private static final String EDITOR_IMPL_CLASS =
            "io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.Ldlib1NodegraphEditor";
    //?} else {
    private static final String EDITOR_IMPL_CLASS =
            "io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.Ldlib2NodegraphEditor";
    //?}

    private NodegraphGate() {
    }

    /** 节点图编辑器是否可用（LDLib 已安装）。 */
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
            LOGGER.warn("[nodegraph] editor unavailable: optional LDLib dependency is not installed");
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
