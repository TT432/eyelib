package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import java.util.Optional;
import java.util.function.Function;

/**
 * Bedrock JSON 资产 → 图库 导入器（规格 nodegraph-workbench §W2）：
 * 结构镜像 {@code ClientEntityAssembler} / {@code RenderControllerAssembler} /
 * {@code AnimationControllerAssembler} 的逆。
 *
 * <p>字段映射：
 * <ul>
 *   <li>ClientEntity：description.identifier → entity.root 选项；geometry/textures/materials/
 *       animations/animation_controllers 声明表 → ref.* 节点，建节点即接线到 entity.root
 *       对应声明端口（规格 D1：声明 = 连线）；
 *       scripts.initialize/pre_animation/parent_setup（string 或 string[]）→ event.* 源节点
 *       + exec 链（v13 事件模型，规格 nodegraph-event-nodes）；
 *       scale/scaleX/scaleY/scaleZ → 表达式槽（数值/布尔 → 内联常量，字符串 → molang 反编译连线）；
 *       animate → animate.entry（短名解析到声明表 ref.animation/ref.ac，未声明 → 补独立
 *       ref.animation + UNKNOWN_REFERENCE）；render_controllers → rc.condition_entry + ref.rc；</li>
 *   <li>RenderController：geometry → 表达式槽（典型 {@code geometry.x} → ref.geometry 连线）；
 *       textures → list.entry；materials/part_visibility（object 或 single-key object 数组）
 *       → material.entry / part_visibility.entry；arrays → rc.root TEXT 选项（原文）；
 *       ignore_lighting → 布尔选项；color/is_hurt_color/on_fire_color/overlay_color
 *       → const.color（全数值且 8bit 精确）或 color.compose 接 COLOR 端口；</li>
 *   <li>AnimationController：initial_state → ac.root 选项；states → ac.state
 *       （on_entry/on_exit → exec 链；animations → animate.entry；transitions → ac.transition；
 *       blend_transition/blend_via_shortest_path → 数值/布尔选项）。</li>
 * </ul>
 *
 * <p>未知/多余 JSON 字段不丢：StickyNote 记录原文 + UNKNOWN_FIELD 诊断；
 * 已知字段值类型非法 → 跳过 + INVALID_FIELD 诊断 + 便签。
 *
 * <p>实现分布：三类文档的导入体分别在 {@link ClientEntityImporter} /
 * {@link RenderControllerImporter} / {@link AnimationControllerImporter}，
 * 通用 JSON 值助手在 {@link ImportValueOps}。
 */
public final class JsonGraphImporters {
    private JsonGraphImporters() {
    }

    // ---------- ClientEntity ----------

    /** client_entity 文件 JSON（含 minecraft:client_entity 包装）→ CLIENT_ENTITY 库（RC 全部为外部引用）。 */
    public static ImportResult importClientEntity(JsonObject fileJson) {
        return ClientEntityImporter.importClientEntity(fileJson);
    }

    /**
     * v4 内联导入（规格 nodegraph-inline-render-controller §6）：{@code rcResolver}
     * 按 RC id 解析 render_controllers 文件形态文档；命中 → rc.root 内联反编译进同一主图
     * （controller/condition 接线齐全）；未命中 → ref.rc 外部引用 + {@link DecompileDiagnostics#RC_INLINE_MISS}。
     * geo/tex/mat 声明表不再建 entity 级 ref：先入 KnownTables 供 RC 表达式裸短名回填，
     * 表内未被任何 RC 字段实体化的条目凾底挂第一个 RC 锚点的声明端口。
     */
    public static ImportResult importClientEntity(JsonObject fileJson,
                                                  Function<String, Optional<JsonObject>> rcResolver) {
        return ClientEntityImporter.importClientEntity(fileJson, rcResolver);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联（规格 nodegraph-import-inline-variables）。 */
    public static ImportResult importClientEntity(JsonObject fileJson,
                                                  Function<String, Optional<JsonObject>> rcResolver,
                                                  boolean inlineVariables) {
        return ClientEntityImporter.importClientEntity(fileJson, rcResolver, inlineVariables);
    }

    // ---------- RenderController ----------

    /** render_controllers 文件 JSON + 目标 RC 名 → RENDER_CONTROLLER 库。 */
    public static ImportResult importRenderController(JsonObject fileJson, String rcName) {
        return RenderControllerImporter.importRenderController(fileJson, rcName);
    }

    /**
     * 同上传入已知短名表（规格 D4 跨文档关联）：{@code geometry.x} 等裸短名 ref
     * 命中表项时回填标识符（显式短名保留）。
     */
    public static ImportResult importRenderController(JsonObject fileJson, String rcName,
                                                      ShortNameOps.KnownTables known) {
        return RenderControllerImporter.importRenderController(fileJson, rcName, known);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联。 */
    public static ImportResult importRenderController(JsonObject fileJson, String rcName,
                                                      ShortNameOps.KnownTables known,
                                                      boolean inlineVariables) {
        return RenderControllerImporter.importRenderController(fileJson, rcName, known, inlineVariables);
    }

    // ---------- AnimationController ----------

    /** animation controllers 文件 JSON + 目标 AC 名 → ANIMATION_CONTROLLER 库。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName) {
        return AnimationControllerImporter.importAnimationControllers(fileJson, acName);
    }

    /** 同上传入已知短名表（规格 D4）：state animations 裸短名 ref 命中表项时回填标识符。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName,
                                                          ShortNameOps.KnownTables known) {
        return AnimationControllerImporter.importAnimationControllers(fileJson, acName, known);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName,
                                                          ShortNameOps.KnownTables known,
                                                          boolean inlineVariables) {
        return AnimationControllerImporter.importAnimationControllers(fileJson, acName, known, inlineVariables);
    }
}
