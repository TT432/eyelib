package io.github.tt432.eyelib.client.nodegraph;

import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.nodegraph.preview.NodeAssetPreview;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.importer.addon.SoundAssetRegistry;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.nodegraph.RefExistenceChecker;
import io.github.tt432.eyelib.particle.loading.ParticleDefinitionRegistry;

/**
 * 注册表驱动的引用存在性检查（规格 nodegraph-missing-refs §2）：
 * 编辑器节点红高亮与 GraphValidator MISSING_REFERENCE 共用同一判定。
 * 实体声明表作为各通道兜底（几何/材质/动画/AC 的短名值即标识符）。
 *
 * @author TT432
 */
public final class MissingRefCheck implements RefExistenceChecker {
    public static final MissingRefCheck INSTANCE = new MissingRefCheck();

    private MissingRefCheck() {
    }

    @Override
    public boolean exists(String refNodeTypeId, String identifier) {
        if (identifier.isBlank()) {
            return true; // 占位 ref：UNKNOWN_REFERENCE/未连线惯例先行覆盖
        }
        return switch (refNodeTypeId) {
            case "ref.geometry" -> ModelManager.INSTANCE.get(identifier) != null
                    || declaredInAnyEntity(EntityChannel.GEOMETRY, identifier);
            case "ref.texture" -> NodeAssetPreview.hasUsableTexture(identifier);
            case "ref.material" -> MaterialManager.INSTANCE.get(identifier) != null
                    || declaredInAnyEntity(EntityChannel.MATERIAL, identifier);
            case "ref.animation" -> AnimationAssetRegistry.animationSchemaDocument(identifier).isPresent()
                    || declaredInAnyEntity(EntityChannel.ANIMATION, identifier);
            case "ref.ac" -> AnimationAssetRegistry.controllerSchemaDocument(identifier).isPresent()
                    || declaredInAnyEntity(EntityChannel.AC, identifier);
            case "ref.particle" -> ParticleDefinitionRegistry.store().get(identifier) != null;
            case "ref.sound" -> SoundAssetRegistry.knownSoundIds().contains(identifier);
            case "ref.rc" -> RenderControllerManager.INSTANCE.get(identifier) != null;
            default -> true;
        };
    }

    private enum EntityChannel {GEOMETRY, MATERIAL, ANIMATION, AC}

    /** 已注册 ClientEntity 声明表值集兜底（短名 → 标识符的值列）。 */
    private static boolean declaredInAnyEntity(EntityChannel channel, String identifier) {
        for (BrClientEntity entity : ClientEntityManager.INSTANCE.all().values()) {
            boolean hit = switch (channel) {
                case GEOMETRY -> entity.geometry().containsValue(identifier);
                case MATERIAL -> entity.materials().containsValue(identifier);
                case ANIMATION -> entity.animations().containsValue(identifier);
                case AC -> entity.animation_controllers().stream()
                        .anyMatch(m -> m.containsValue(identifier));
            };
            if (hit) {
                return true;
            }
        }
        return false;
    }
}
