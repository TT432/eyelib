//? if >=1.20.1 {
package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraph;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
//?}
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 变量表面板操作的 undo 接线（规格 nodegraph-variable-table §2.7）：
 * 图模型 NBT 快照 + {@link EvmGraph.LibraryContext#variableScopes} 侧表快照
 * 双份留底，undo/redo 同时恢复（LDLib2 内建 UndoableGraphCommand 只覆盖图 NBT，
 * 侧表是 eyelib 域概念，不在其序列化范围）。
 *
 * <p>连续编辑（逐键改名/改默认值）靠 {@code source} 合并：同一 {@code source}+同名
 * 的连续 push 会被 HistoryStack 合并为一条历史（快照语义下合并后端点仍正确——
 * redo 终态 = 最后一次 after，undo 终态 = 第一次 before）。
 */
final class EvmUndo {
    private EvmUndo() {
    }

    /**
     * 执行变更并入栈。
     *
     * @param source 合并键（同一字段的连续编辑传同一个稳定对象，如 "name:&lt;uid&gt;"；
     *               离散操作传 null）
     */
    static void push(GraphView view, GraphModel model, EvmGraph.@Nullable LibraryContext ctx,
                     String name, @Nullable Object source, Runnable mutation) {
        CompoundTag before = serializeGraph(model);
        Map<UUID, VariableDecl.Scope> scopesBefore = snapshotScopes(ctx);

        mutation.run();

        CompoundTag after = serializeGraph(model);
        Map<UUID, VariableDecl.Scope> scopesAfter = snapshotScopes(ctx);
        if (before.equals(after) && scopesBefore.equals(scopesAfter)) {
            return;
        }
        view.getHistoryStack().pushHistory(Component.literal(name), EditAction.of(
                () -> restore(view, model, ctx, after, scopesAfter),
                () -> restore(view, model, ctx, before, scopesBefore)), source, false);
    }

    /** 图模型快照（26.1 的序列化 API 改版为 ValueOutput/ValueInput，低版本是 *NBT(RegistryAccess)）。 */
    private static CompoundTag serializeGraph(GraphModel model) {
        //? if <26.1 {
        return model.serializeNBT(Platform.getFrozenRegistry());
        //?} else {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING,
                Platform.getFrozenRegistry());
        model.serialize(output);
        return output.buildResult();
        //?}
    }

    private static void deserializeGraph(GraphModel model, CompoundTag tag) {
        //? if <26.1 {
        model.deserializeNBT(Platform.getFrozenRegistry(), tag);
        //?} else {
        model.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING,
                Platform.getFrozenRegistry(), tag));
        //?}
    }

    private static void restore(GraphView view, GraphModel model,
                                EvmGraph.@Nullable LibraryContext ctx,
                                CompoundTag tag, Map<UUID, VariableDecl.Scope> scopes) {
        deserializeGraph(model, tag);
        if (ctx != null) {
            ctx.variableScopes.clear();
            ctx.variableScopes.putAll(scopes);
        }
        view.rebuildGraphUI();
    }

    private static Map<UUID, VariableDecl.Scope> snapshotScopes(EvmGraph.@Nullable LibraryContext ctx) {
        return ctx == null ? Map.of() : new HashMap<>(ctx.variableScopes);
    }
}
//?}
