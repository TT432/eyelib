package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.bridge.material.RenderPassAdapter;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.material.port.PortRenderPass.Transparency;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Bridge 层烟雾测试：验证 RenderTypeResolver → RenderPassAdapter 全链路在 MC 运行时正确工作。
 * <p>
 * 使用真实 .mcpack 数据和 MC RenderType 工厂，在 clientsmoke Phase 4 中执行。
 *
 * @author TT432
 */
@ClientSmoke(
        description = "Bridge RenderTypeResolver + RenderPassAdapter 全链路验证",
        priority = 10
)
public class RenderTypeBridgeSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderTypeBridgeSmoke.class);

    private static void require(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    public RenderTypeBridgeSmoke() {
        Map<String, BrMaterialEntry> materials = MaterialManager.INSTANCE.getAllData();
        LOGGER.info("[RenderTypeBridgeSmoke] Loaded {} materials", materials.size());
        require(!materials.isEmpty(), "No materials loaded from .mcpack");

        // === S1: entity → SOLID ===
        {
            BrMaterialEntry entry = java.util.Objects.requireNonNull(materials.get("entity"),
                    "entity not found in MaterialManager");

            PortRenderPass pass = RenderTypeResolver.resolve(
                    PortResourceLocation.of("minecraft", "textures/entity/test"),
                    entry, materials);

            require(pass.transparency() == Transparency.SOLID,
                    "entity → SOLID: got " + pass.transparency());
            require(!pass.disableCulling(),
                    "entity cull=true → disableCulling=false");

            RenderType rt = RenderPassAdapter.toRenderType(pass,
                    PortResourceLocation.of("minecraft", "textures/entity/test"));
            require(rt != null, "entity → MC RenderType should not be null");
        }

        // === S2: entity_nocull → 仅 DisableCulling（SOLID + cull=false）===
        {
            // entity_nocull 在 Bedrock 中只加 DisableCulling，不加 ALPHA_TEST
            // ALPHA_TEST 由 render controller 在运行时选择材质
            BrMaterialEntry entry = java.util.Objects.requireNonNull(materials.get("entity_nocull:entity"),
                    "entity_nocull:entity not found in MaterialManager");

            PortRenderPass pass = RenderTypeResolver.resolve(
                    PortResourceLocation.of("minecraft", "textures/entity/slime"),
                    entry, materials);

            require(pass.transparency() == Transparency.SOLID,
                    "entity_nocull (no ALPHA_TEST) → SOLID: got " + pass.transparency());
            require(pass.disableCulling(),
                    "entity_nocull DisableCulling → disableCulling=true");
        }

        // === S3: entity_alphablend → TRANSLUCENT + cull=true ===
        {
            BrMaterialEntry entry = java.util.Objects.requireNonNull(materials.get("entity_alphablend:entity"),
                    "entity_alphablend:entity not found in MaterialManager");

            PortRenderPass pass = RenderTypeResolver.resolve(
                    PortResourceLocation.of("minecraft", "textures/entity/ghast"),
                    entry, materials);

            require(pass.transparency() == Transparency.TRANSLUCENT,
                    "entity_alphablend → TRANSLUCENT: got " + pass.transparency());
            require(!pass.disableCulling(),
                    "entity_alphablend no DisableCulling → disableCulling=false");

            //? if <1.20.6 {
            ResourceLocation mcTex = new ResourceLocation("minecraft", "textures/entity/ghast");
            //?} else {
            ResourceLocation mcTex = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/ghast");
            //?}
            //? if <26.1 {
            RenderType expected = RenderType.entityTranslucentCull(mcTex);
            RenderType actual = RenderPassAdapter.toRenderType(pass,
                    PortResourceLocation.of("minecraft", "textures/entity/ghast"));
            require(expected.equals(actual),
                    "entity_alphablend → entityTranslucentCull");
            //?} else {
            RenderType expected = RenderType.entityTranslucentCullItemTarget(mcTex);
            RenderType actual = RenderPassAdapter.toRenderType(pass,
                    PortResourceLocation.of("minecraft", "textures/entity/ghast"));
            require(expected.equals(actual),
                    "entity_alphablend → entityTranslucentCullItemTarget (26.1)");
            //?}
        }

        // === S4: entity_alphatest → ALPHA_TEST + cull=true ===
        {
            BrMaterialEntry entry = java.util.Objects.requireNonNull(materials.get("entity_alphatest:entity"),
                    "entity_alphatest:entity not found in MaterialManager");

            PortRenderPass pass = RenderTypeResolver.resolve(
                    PortResourceLocation.of("minecraft", "textures/entity/test_alphatest"),
                    entry, materials);

            require(pass.transparency() == Transparency.ALPHA_TEST,
                    "entity_alphatest → ALPHA_TEST: got " + pass.transparency());
            require(!pass.disableCulling(),
                    "entity_alphatest no DisableCulling → disableCulling=false");
        }

        LOGGER.info("[RenderTypeBridgeSmoke] All 4 bridge adapter paths passed");
    }
}
