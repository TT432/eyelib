package io.github.tt432.eyelib.molang.mapping.api;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.Nullable;

/**
 * 动态 Molang 命名空间：把一段点分名字根交给运行期注册的提供者解析。
 *
 * <p>与 {@link MolangMappingTree} 的静态映射（{@code @MolangMapping} 类扫描）互补：
 * 静态树按「首段根 + 余下成员名」组织，表达不了 {@code query.owner.health} 这类整根
 * 由运行期提供的成员；动态命名空间按点分最长前缀匹配补齐这一层。
 *
 * <p>解析顺序契约（由 {@code MolangRuntimeSupport} 保证）：{@link MolangScope} 覆盖 →
 * 动态命名空间 → 静态映射零参绑定。提供者不得假设自己优先于 scope 覆盖，也不要在
 * 成员访问之外的调用形态（{@code query.owner.health()}）上依赖本接口。
 *
 * @author TT432
 */
@FunctionalInterface
public interface MolangNamespace {
    /**
     * 解析一次成员访问。
     *
     * @param scope 当前求值作用域，供提供者按宿主上下文取值
     * @param path  相对命名空间的剩余成员路径（如 {@code "health"}、{@code "pos.x"}）；
     *              被访问的名字与命名空间完全相同时为空串
     * @return 本次访问的值；本次不可用返回 {@code null}（继续回落到静态映射）
     */
    @Nullable
    MolangObject resolve(MolangScope scope, String path);
}
