package io.github.tt432.eyelib.client.molangdebug;

import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.capability.AttachableDataTypes;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.compiler.CompileContext;
import io.github.tt432.eyelib.molang.compiler.MolangCompilerImpl;
import io.github.tt432.eyelib.molang.compiler.MolangConstantExpressionEvaluator;
import io.github.tt432.eyelib.molang.compiler.cache.MolangCompileCache;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Molang 运行时调试服务：聚合调试器对 MC 世界与 molang 运行时的全部访问，
 * 使 {@link MolangDebugScreen} 保持 MC 无关。
 *
 * <p>scope 获取链路（与渲染链 {@code EntityRenderOrchestrator} 一致但全程只读）：
 * 实体 → {@link DataAttachmentHelper#getOrNull}（不创建附件）→ {@link RenderData}
 * → owner 校验 → {@link RenderData#getScope()}。scope 由渲染链 lazy-init
 * （{@code ensureOwner} → {@code init}）创建，调试器不主动初始化；没有 scope
 * 的目标由界面标注「无 molang 上下文」。
 *
 * <p>所有方法都必须在客户端（渲染）线程调用；屏幕 render 即在渲染线程，安全。
 *
 * @author TT432
 */
public final class MolangDebugService {
    private MolangDebugService() {}

    /**
     * 调试目标实体。
     *
     * @param hasScope 列出目标时是否已存在可用的 molang scope（仅用于列表标注，快照可能过期）
     */
    public record DebugTarget(Entity entity, String typeId, String displayName, boolean hasScope) {}

    /**
     * scope 变量快照条目。
     *
     * @param depth 来源层：0 = 当前 scope 本层，n &gt; 0 = parent 链第 n 层（同名变量逐层遮蔽）
     */
    public record ScopeVar(String name, MolangObject value, int depth) {}

    /**
     * watch 表达式求值结果。成功时 {@link #value} 非空；失败时 {@link #error} 非空。
     */
    public record EvalResult(@Nullable MolangObject value, @Nullable String error) {
        public boolean success() {
            return error == null;
        }
    }

    /**
     * 调试器自用的 L1 编译缓存（与 {@code MolangValue} 的缓存同构）：
     * 带映射树版本引用做过期检测，避免 watch 表达式每帧重复编译。
     */
    private static final MolangCompileCache COMPILE_CACHE =
            new MolangCompileCache(MolangMappingRegistries.mappingTree(), null);

    /** 打开 molang 调试屏幕。 */
    public static void openDebugScreen() {
        Minecraft.getInstance().setScreen(UiPort.wrap(new MolangDebugScreen()));
    }

    /**
     * 列出当前 client level 中可被调试的实体目标；不在世界中时返回空表。
     * 实体遍历走 {@link ClientLevel#entitiesForRendering()}（全版本同签名，
     * 渲染链 {@code EntityRenderOrchestrator} 同款路径，无需版本条件）。
     */
    public static List<DebugTarget> listTargets() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        List<DebugTarget> result = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            String typeId = resolveTypeId(entity);
            // 显示名 = 实体类型 id + UUID 短码（区分同类型多个体）
            String uuidShort = entity.getUUID().toString().substring(0, 8);
            result.add(new DebugTarget(entity, typeId, typeId + " #" + uuidShort, resolveScope(entity) != null));
        }
        return result;
    }

    /**
     * 解析实体类型 id。优先走 bridge 渲染端口（与渲染链 {@code setupClientEntity} 同一来源，
     * 内部消化 Forge/NeoForge 注册表差异）；端口未安装时回退实体自身编码 id。
     */
    private static String resolveTypeId(Entity entity) {
        @Nullable RenderPorts ports = RenderPorts.HOLDER.get();
        if (ports != null) {
            try {
                return ports.renderSystemPort().getEntityTypeId(entity);
            } catch (Throwable ignored) {
                // 渲染端口异常不阻断调试器，走回退路径
            }
        }
        String encodeId = entity.getEncodeId();
        return encodeId != null ? encodeId : entity.getType().toString();
    }

    /**
     * 只读解析实体的 molang scope。
     * 实体无 {@link RenderData} 附件、附件 owner 易主或 scope 尚未由渲染链初始化时返回 {@code null}。
     */
    public static @Nullable MolangScope resolveScope(Entity entity) {
        @Nullable RenderData<?> cap = DataAttachmentHelper.getOrNull(AttachableDataTypes.RENDER_DATA.get(), entity);
        if (cap == null) {
            return null;
        }
        // owner 守卫收敛在 RenderData 内（IQF Q-4）；附件易主或未初始化时返回 null。
        return cap.scopeIfOwnedBy(entity).orElse(null);
    }

    /** 目标实体是否仍然存活（未被移除出世界）。 */
    public static boolean isAlive(Entity entity) {
        return !entity.isRemoved();
    }

    /**
     * 沿 scope 链做变量快照（{@code variable.*}、{@code temp.*}、{@code context.*} 等所有缓存键）。
     * 按层序（本层在前）+ 层内键名排序返回；同名变量在各层都保留条目，
     * 由 {@link ScopeVar#depth()} 标注来源层供界面区分遮蔽关系。
     */
    public static List<ScopeVar> snapshotScope(MolangScope scope) {
        List<ScopeVar> result = new ArrayList<>();
        int depth = 0;
        for (@Nullable MolangScope s = scope; s != null; s = s.getParent(), depth++) {
            int d = depth;
            s.localEntries().entrySet().stream()
             .sorted(Comparator.comparing(java.util.Map.Entry<String, MolangObject>::getKey))
             .forEach(entry -> result.add(new ScopeVar(entry.getKey(), entry.getValue(), d)));
        }
        return result;
    }

    /**
     * 编译并求值 molang 表达式。路径与 {@code MolangValue#resolveFunction} 一致：
     * 常量折叠（{@link MolangConstantExpressionEvaluator}）→ L1 编译缓存
     * （{@link MolangCompileCache#getOrCompile} + {@link MolangCompilerImpl}）→ evaluate(scope)。
     * 任何失败都收敛为 {@link EvalResult#error} 文本，不向调用方抛出。
     */
    public static EvalResult eval(MolangScope scope, String expression) {
        try {
            Optional<MolangObject> constant = MolangConstantExpressionEvaluator.tryEvaluate(expression);
            MolangObject value = constant.isPresent()
                    ? constant.get()
                    : COMPILE_CACHE.getOrCompile(expression,
                            () -> new MolangCompilerImpl().compile(expression, CompileContext.defaults()))
                    .evaluate(scope);
            return new EvalResult(value != null ? value : MolangNull.INSTANCE, null);
        } catch (Throwable t) {
            String message = t.getMessage();
            return new EvalResult(null, message != null && !message.isEmpty()
                    ? message : t.getClass().getSimpleName());
        }
    }
}
