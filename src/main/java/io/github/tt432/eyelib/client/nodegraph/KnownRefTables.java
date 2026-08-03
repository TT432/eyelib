package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import io.github.tt432.eyelib.nodegraph.ShortNames;
import java.util.Map;

/**
 * 已知短名表收集（规格 nodegraph-shortname-elimination D4 跨文档关联）：
 * RC/AC 导入时，{@code geometry.x} 等裸短名 ref 需要一张「短名 → 标识符」表来回填标识符。
 *
 * <p>来源（先注册表后图库，先登记者胜）：
 * <ol>
 *   <li>{@link ClientEntityManager} 全部已注册 ClientEntity 的声明表
 *       （geometry/textures/materials/animations/animation_controllers——Bedrock 原始键）；</li>
 *   <li>{@link GraphLibraryManager} 已导入实体图库中 ref 节点的有效短名
 *       （显式或派生）→ 标识符。</li>
 * </ol>
 */
public final class KnownRefTables {
    private KnownRefTables() {
    }

    public static ShortNameOps.KnownTables collect() {
        return collectForRc(null);
    }

    /**
     * 面向某个 RC 导入的收集（D4）：优先收录「render_controllers 列出该 RC」的实体表
     * （RC↔实体的真实关联方），其余实体表作为低优先级兜底（先登记者胜）。
     * A\&S 类包共享 RC 时，非关联实体的短名可能是「设计性 miss」（miss→default 回退），
     * 优先关联实体可避免预览错配。
     */
    public static ShortNameOps.KnownTables collectForRc(@org.jspecify.annotations.Nullable String rcId) {
        ShortNameOps.KnownTables.Builder b = new ShortNameOps.KnownTables.Builder();
        java.util.List<BrClientEntity> all = new java.util.ArrayList<>(
                ClientEntityManager.INSTANCE.all().values());
        // 关联实体优先
        all.sort((a, c) -> Boolean.compare(!a.render_controllers().contains(rcId),
                !c.render_controllers().contains(rcId)));
        for (BrClientEntity entity : all) {
            putEntity(b, entity);
        }
        collectGraphLibraries(b);
        return b.build();
    }

    private static void putEntity(ShortNameOps.KnownTables.Builder b, BrClientEntity entity) {
        entity.geometry().forEach((k, v) -> b.put("ref.geometry", k, v));
        // 运行时纹理值带 .png（CODEC 层补）；图内 path 选项不带（组装器注释同约）——剥掉
        entity.textures().forEach((k, v) -> b.put("ref.texture", k,
                v.endsWith(".png") ? v.substring(0, v.length() - 4) : v));
        entity.materials().forEach((k, v) -> b.put("ref.material", k, v));
        entity.animations().forEach((k, v) -> b.put("ref.animation", k, v));
        entity.animation_controllers().forEach(map -> map.forEach((k, v) -> b.put("ref.ac", k, v)));
    }

    private static void collectGraphLibraries(ShortNameOps.KnownTables.Builder b) {
        for (GraphLibrary library : GraphLibraryManager.INSTANCE.all().values()) {
            if (library.kind() != GraphKind.CLIENT_ENTITY) {
                continue;
            }
            for (GraphData graph : library.graphs().values()) {
                for (NodeInstance node : graph.nodes()) {
                    String valueOption = ShortNames.valueOptionOf(node.type());
                    if (valueOption == null) {
                        continue;
                    }
                    String identifier = node.option(valueOption, NodeTypes.require(node.type()))
                            .map(com.google.gson.JsonElement::getAsString).orElse("");
                    if (identifier.isEmpty()) {
                        continue;
                    }
                    String shortName = ShortNames.effective(node, NodeTypes.require(node.type()));
                    if (!shortName.isEmpty()) {
                        b.put(node.type(), shortName, identifier);
                    }
                }
            }
        }
    }
}
