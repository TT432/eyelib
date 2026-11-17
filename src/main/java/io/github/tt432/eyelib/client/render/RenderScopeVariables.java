package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.molang.MolangScope;

/**
 * 自定义宿主的额外 {@code variable.*} 注入点（每帧一次，先于模型几何提交）。
 * <p>
 * 注释：只在 {@link RenderHost.ScopedHost} 上执行；方块锚定宿主需要逐作物变量时也应使用
 * {@code scoped(...)} 而不是在调用方手工 {@code scope.set}（手工写会漏掉「每帧一次」的执行时机，
 * 并且与管线 tick 的写入顺序不可预测）。
 *
 * @author TT432
 */
@FunctionalInterface
public interface RenderScopeVariables {
    void apply(MolangScope scope, float partialTick);
}
