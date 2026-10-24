package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;

/**
 * 单步图文档版本迁移：实现类只负责 fromVersion → fromVersion + 1 的一步，
 * 由 {@code GraphMigrations} 按版本升序链式调用。
 */
public interface VersionMigration {
    /** 源版本号（迁移后文档版本为 fromVersion + 1）。 */
    int fromVersion();

    /** 应用本步迁移，返回新文档（纯函数，不修改入参）。 */
    GraphLibrary apply(GraphLibrary library);
}
