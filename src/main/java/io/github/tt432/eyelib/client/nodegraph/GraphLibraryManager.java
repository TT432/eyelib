package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/**
 * 图文档库注册表：键 = 资源文件名（不含目录与后缀）。
 *
 * @author TT432
 */
public final class GraphLibraryManager {
    public static final Registry<GraphLibrary> INSTANCE =
            new Registry<>("GraphLibraryManager", ManagerEventPublishBridge.publisher());

    private GraphLibraryManager() {
    }
}
