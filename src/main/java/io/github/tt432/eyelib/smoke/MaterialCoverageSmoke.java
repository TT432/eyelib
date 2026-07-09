package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 烟雾测试：验证所有实体引用的材质都在 MaterialManager 中有定义。
 * <p>
 * 此测试可捕获 "材质缺失 → 静默 SOLID 回退" 类问题。
 * 如果某个实体引用了未定义的材质名（如 "spider"），
 * RenderTypeResolver 会走 default 分支输出 SOLID + 警告日志，
 * 导致实体以错误的 RenderType 渲染（texture 缺失/黑块）。
 *
 * @author TT432
 */
@ClientSmoke(
        description = "验证实体引用的材质覆盖率，捕获缺失材质导致的 RenderType 回退",
        priority = 20
)
public class MaterialCoverageSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialCoverageSmoke.class);

    private static void require(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    public MaterialCoverageSmoke() {
        Map<String, BrMaterialEntry> materials = MaterialManager.INSTANCE.all();
        LOGGER.info("[MaterialCoverageSmoke] {} materials loaded", materials.size());
        require(!materials.isEmpty(), "No materials loaded");

        // === M1: 验证核心 base 材质存在 ===
        String[] coreMaterials = {
                "entity", "entity_alphatest", "entity_alphablend",
                "entity_nocull", "entity_static",
                "entity_emissive", "entity_emissive_alpha",
                "entity_change_color", "entity_alphatest_change_color"
        };
        for (String name : coreMaterials) {
            require(BrMaterialResolver.find(materials, name).isPresent(),
                    "Core material '" + name + "' not found in MaterialManager");
        }

        // === M2: 验证 vanilla 实体材质存在（防止 bedrock_entity.material 丢失条目）===
        String[] entityMaterials = {
                "spider", "zombie", "skeleton", "creeper", "bee",
                "ender_dragon", "guardian", "iron_golem", "witch", "phantom"
        };
        for (String name : entityMaterials) {
            require(BrMaterialResolver.find(materials, name).isPresent(),
                    "Entity material '" + name + "' not found — add to bedrock_entity.material");
        }

        // === M3: 验证材质继承链解析正确（spider → entity_alphatest → ALPHA_TEST）===
        {
            BrMaterialEntry spider = BrMaterialResolver.find(materials, "spider").orElse(null);
            require(spider != null, "spider material must exist");
            ResolvedBrMaterial resolved = BrMaterialResolver.resolve(spider, materials);
            require(resolved.hasDefine("ALPHA_TEST"),
                    "spider must resolve ALPHA_TEST via entity_alphatest inheritance; got defines=" + resolved.defines());
        }

        // === M4: 验证 emissive 材质继承链（entity_emissive → USE_EMISSIVE）===
        {
            BrMaterialEntry emissive = BrMaterialResolver.find(materials, "entity_emissive").orElse(null);
            require(emissive != null, "entity_emissive material must exist");
            ResolvedBrMaterial resolved = BrMaterialResolver.resolve(emissive, materials);
            require(resolved.hasDefine("USE_EMISSIVE"),
                    "entity_emissive must resolve USE_EMISSIVE; got defines=" + resolved.defines());
        }

        // === M5: 如果有加载的实体，验证所有引用材质都存在 ===
        Map<String, BrClientEntity> entities = ClientEntityManager.INSTANCE.all();
        if (!entities.isEmpty()) {
            List<String> missing = new ArrayList<>();
            for (BrClientEntity entity : entities.values()) {
                for (var entry : entity.materials().entrySet()) {
                    String matName = entry.getValue();
                    if ("minecraft:null".equals(matName)) continue;
                    if (BrMaterialResolver.find(materials, matName).isEmpty()) {
                        missing.add(matName);
                    }
                }
            }
            require(missing.isEmpty(),
                    "Entities reference " + missing.size() + " undefined materials: " + missing);
            LOGGER.info("[MaterialCoverageSmoke] All {} entities' materials verified", entities.size());
        } else {
            LOGGER.warn("[MaterialCoverageSmoke] ClientEntityManager is empty — addon not loaded, skipping entity material coverage check");
        }

        // === M6: 验证 WARNED_UNKNOWN_RENDER_TYPES 为空（无渲染使用了 SOLID 回退）===
        require(RenderTypeResolver.WARNED_UNKNOWN_RENDER_TYPES.isEmpty(),
                "Unknown render types were encountered during rendering (would cause incorrect SOLID fallback): "
                        + RenderTypeResolver.WARNED_UNKNOWN_RENDER_TYPES);

        LOGGER.info("[MaterialCoverageSmoke] All material coverage checks passed");
    }
}
