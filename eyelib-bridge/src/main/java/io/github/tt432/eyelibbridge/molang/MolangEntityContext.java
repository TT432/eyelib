package io.github.tt432.eyelibbridge.molang;

import org.jspecify.annotations.NullMarked;

/**
 * Molang 求值上下文，提供实体运行时组件的访问入口。
 *
 * @author TT432
 */
@NullMarked
public final class MolangEntityContext {
    private final ComponentStore componentStore;

    public MolangEntityContext(ComponentStore componentStore) {
        this.componentStore = componentStore;
    }

    /**
     * 返回与此上下文关联的组件存储。
     *
     * @return 组件存储实例
     */
    public ComponentStore componentStore() {
        return componentStore;
    }
}
