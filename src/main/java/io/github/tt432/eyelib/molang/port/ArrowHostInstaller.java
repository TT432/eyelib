package io.github.tt432.eyelib.molang.port;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.Nullable;

/**
 * 箭头访问（{@code ->}）宿主安装器 Port。
 * <p>
 * Bedrock Molang 语义：{@code left->right} 中左侧求值为宿主实体引用，右侧查询在
 * 该实体上下文中求值。domain 层不感知 MC 实体类型，由 bridge 提供安装器：
 * 把左侧宿主值翻译为宿主上下文中的实体并注入，求值结束后恢复原上下文。
 * <p>
 * 未注册安装器时（纯 domain 环境），箭头访问退化为仅求值右式（与旧行为一致）。
 *
 * @author TT432
 */
@FunctionalInterface
public interface ArrowHostInstaller {

    /**
     * 安装箭头宿主并返回恢复 token（供 {@link #restore} 还原原上下文）。
     *
     * @param scope 当前求值作用域
     * @param host  箭头左侧求值结果（宿主引用）
     * @return 恢复 token；宿主不可翻译（非实体引用）时返回 {@code null}，表示未切换
     */
    @Nullable
    Object install(MolangScope scope, MolangObject host);

    /**
     * 恢复箭头切换前的宿主上下文。
     *
     * @param scope     当前求值作用域
     * @param previous  {@link #install} 返回的 token；{@code null} 表示无切换，恢复为无操作
     */
    default void restore(MolangScope scope, @Nullable Object previous) {
    }
}
