package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.entity.BrClientEntityScripts;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * a&s.mcpack 实体定义完整性测试。
 * 取 a&s 加载的实体定义与当前 MC 版本注册表的交集，逐个验证：
 * 定义加载、render_controllers 解析、geometry/textures/materials 引用、Molang 编译求值。
 *
 * @author TT432
 */
@ClientSmoke(
        description = "a&s.mcpack 实体定义完整性：交集实体 × 定义加载 × 资源引用 × Molang 编译",
        priority = 30
)
public class AnsEntitySmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnsEntitySmoke.class);

    public AnsEntitySmoke() {
        var clientEntities = ClientEntityManager.INSTANCE.getAllData();

        Set<String> mcEntityIds = new TreeSet<>();
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            mcEntityIds.add(rl.toString());
        }

        Set<String> targets = new TreeSet<>(clientEntities.keySet());
        targets.retainAll(mcEntityIds);

        LOGGER.info("[AnsEntitySmoke] a&s 定义 {} 个实体, MC 注册 {} 个, 交集 {} 个",
                clientEntities.size(), mcEntityIds.size(), targets.size());
        LOGGER.info("[AnsEntitySmoke] a&s 实体: {}", clientEntities.keySet());
        LOGGER.info("[AnsEntitySmoke] MC 实体: {}", mcEntityIds);

        if (targets.isEmpty()) {
            throw new AssertionError("交集为空：a&s 未加载或 MC 版本无匹配实体");
        }

        List<String> errors = new ArrayList<>();
        int checked = 0;

        for (String entityId : targets) {
            checked++;
            var entity = clientEntities.get(entityId);
            try {
                validateEntity(entityId, entity, errors);
            } catch (Exception e) {
                errors.add(entityId + ": 未预期异常 " + e);
                LOGGER.error("[AnsEntitySmoke] {} 验证抛出异常", entityId, e);
            }
        }

        int passCount = checked - errors.size();
        LOGGER.info("[AnsEntitySmoke] 完成: {}/{} 通过, {} 失败", passCount, checked, errors.size());
        for (String err : errors) {
            LOGGER.error("[AnsEntitySmoke] FAIL {}", err);
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(errors.size()).append("/").append(checked).append(" 实体验证失败:\n");
            for (String err : errors) {
                sb.append("  ").append(err).append("\n");
            }
            throw new AssertionError(sb.toString());
        }
    }

    private void validateEntity(String entityId, BrClientEntity entity, List<String> errors) {
        MolangScope scope = new MolangScope();

        validateGeometryRefs(entityId, entity, errors);
        validateTextureRefs(entityId, entity, errors);
        validateRenderControllers(entityId, entity, scope, errors);
        validateScripts(entityId, entity, scope, errors);
    }

    /**
     * geometry map 的 value（geometry identifier）必须在 ModelManager 中存在。
     */
    private void validateGeometryRefs(String entityId, BrClientEntity entity, List<String> errors) {
        var geometryMap = entity.geometry();
        if (geometryMap.isEmpty()) {
            return;
        }
        var models = ModelManager.INSTANCE.getAllData();
        for (var entry : geometryMap.entrySet()) {
            String geoId = entry.getValue();
            if (!models.containsKey(geoId)) {
                errors.add(entityId + ": geometry[" + entry.getKey() + "]=\"" + geoId + "\" 在 ModelManager 中不存在");
            }
        }
    }

    /**
     * textures map 的 value（纹理路径）必须可被解析为 ResourceLocation。
     */
    private void validateTextureRefs(String entityId, BrClientEntity entity, List<String> errors) {
        var textureMap = entity.textures();
        if (textureMap.isEmpty()) {
            return;
        }
        for (var entry : textureMap.entrySet()) {
            String texPath = entry.getValue();
            if (ResourceLocation.tryParse(texPath) == null) {
                errors.add(entityId + ": texture[" + entry.getKey() + "]=\"" + texPath + "\" 无法解析为 ResourceLocation");
            }
        }
    }

    /**
     * render_controllers 中的每个 RC 名必须在 RenderControllerManager 中存在；
     * renderControllerConditions 中的 MolangValue 必须能 evalAsBool；
     * 每个 RC 的 geometry/textures/materials/part_visibility MolangValue 必须 initArrays 后能 getObject 不崩溃。
     */
    private void validateRenderControllers(String entityId, BrClientEntity entity,
                                            MolangScope scope, List<String> errors) {
        var rcList = entity.render_controllers();
        var rcConditions = entity.renderControllerConditions();

        for (String rcName : rcList) {
            var condition = rcConditions.get(rcName);
            if (condition != null) {
                try {
                    condition.evalAsBool(scope);
                } catch (Exception e) {
                    errors.add(entityId + ": RC[" + rcName + "] condition 求值异常: " + e.getMessage());
                }
            }

            RenderControllerEntry rcEntry = RenderControllerManager.INSTANCE.get(rcName);
            if (rcEntry == null) {
                errors.add(entityId + ": render_controller \"" + rcName + "\" 在 RenderControllerManager 中不存在");
                continue;
            }

            MolangScope rcScope = new MolangScope();
            rcEntry.initArrays(rcScope, entity);

            tryEval(errors, entityId, rcName + ".geometry", () -> rcEntry.geometry().getObject(rcScope));

            for (int i = 0; i < rcEntry.textures().size(); i++) {
                MolangValue texMv = rcEntry.textures().get(i);
                int idx = i;
                tryEval(errors, entityId, rcName + ".texture[" + idx + "]", () -> texMv.getObject(rcScope));
            }

            for (int i = 0; i < rcEntry.materials().size(); i++) {
                var matEntry = rcEntry.materials().get(i);
                int idx = i;
                tryEval(errors, entityId, rcName + ".material[" + idx + "]", () -> matEntry.value().getObject(rcScope));
            }

            rcEntry.part_visibility().forEach((bone, mv) ->
                    tryEval(errors, entityId, rcName + ".part_visibility[" + bone + "]", () -> mv.getObject(rcScope))
            );
        }
    }

    /**
     * scripts 中的 MolangValue 必须能 eval（initialize, pre_animation, scale, scaleX/Y/Z），
     * animate map 中的每个 MolangValue 也必须能 eval。
     */
    private void validateScripts(String entityId, BrClientEntity entity,
                                  MolangScope scope, List<String> errors) {
        Optional<BrClientEntityScripts> scriptsOpt = entity.scripts();
        if (scriptsOpt.isEmpty()) {
            return;
        }
        BrClientEntityScripts scripts = scriptsOpt.get();

        tryEval(errors, entityId, "scripts.initialize", () -> scripts.initialize().getObject(scope));
        tryEval(errors, entityId, "scripts.pre_animation", () -> scripts.pre_animation().getObject(scope));
        tryEval(errors, entityId, "scripts.parent_setup", () -> scripts.parent_setup().getObject(scope));
        tryEval(errors, entityId, "scripts.scale", () -> scripts.scale().getObject(scope));

        scripts.scaleX().ifPresent(mv ->
                tryEval(errors, entityId, "scripts.scaleX", () -> mv.getObject(scope)));
        scripts.scaleY().ifPresent(mv ->
                tryEval(errors, entityId, "scripts.scaleY", () -> mv.getObject(scope)));
        scripts.scaleZ().ifPresent(mv ->
                tryEval(errors, entityId, "scripts.scaleZ", () -> mv.getObject(scope)));

        scripts.animate().forEach((name, mv) ->
                tryEval(errors, entityId, "scripts.animate[" + name + "]", () -> mv.getObject(scope))
        );
    }

    @FunctionalInterface
    private interface EvalAction {
        MolangObject eval();
    }

    private void tryEval(List<String> errors, String entityId, String field, EvalAction action) {
        try {
            action.eval();
        } catch (Exception e) {
            errors.add(entityId + ": " + field + " 求值异常: " + e.getMessage());
        }
    }
}
