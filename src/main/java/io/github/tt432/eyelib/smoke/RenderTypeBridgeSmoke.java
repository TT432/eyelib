package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.bridge.material.adapter.RenderPassAdapter;
import io.github.tt432.eyelib.bridge.material.RenderTypeResolver;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.material.port.PortRenderPass.Transparency;
import io.github.tt432.eyelib.material.render.RenderTypeResolver.EntityRenderTypeData;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
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
        Map<String, BrMaterialEntry> materials = MaterialManager.INSTANCE.all();
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
            //?} elif <26.1 {
            ResourceLocation mcTex = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/ghast");
            //?} else {
            Identifier mcTex = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/ghast");

            //?}
            // 26.1.2 下 TRANSLUCENT + cull=true 走 custom pipeline (BrRenderState),
            // RenderPassAdapter 返回 BridgeRenderPass 的 custom RenderType,
            // 不等于标准 RenderTypes.entityTranslucent.
            // PortRenderPass 属性已在上面的 require 中验证.
            //? if <26.1 {
            RenderType expected = RenderType.entityTranslucentCull(mcTex);
            RenderType actual = RenderPassAdapter.toRenderType(pass,
                    PortResourceLocation.of("minecraft", "textures/entity/ghast"));
            require(expected.equals(actual),
                    "entity_alphablend → entityTranslucentCull");
            //?} else {
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

        // === S5: 原版 (MC JE) RenderType 名称回退（材质系统未命中时按名称匹配原版语义）===
        {
            PortResourceLocation tex = PortResourceLocation.of("minecraft", "textures/entity/vanilla");

            // entity_solid -> SOLID + cull, isSolid=true
            EntityRenderTypeData d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_solid"));
            PortRenderPass p = d.factory().apply(tex);
            require(p.transparency() == Transparency.SOLID, "entity_solid -> SOLID: got " + p.transparency());
            require(!p.disableCulling(), "entity_solid cull -> disableCulling=false");
            require(d.isSolid(), "entity_solid isSolid=true");
            require(!RenderTypeResolver.WARNED_UNKNOWN_RENDER_TYPES.contains("minecraft:entity_solid"),
                    "entity_solid is a known vanilla type, must not warn");

            // entity_cutout -> ALPHA_TEST；cull 因版本对齐原版（<26.1 剔除 / >=26.1 不剔除）
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_cutout"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.ALPHA_TEST, "entity_cutout -> ALPHA_TEST: got " + p.transparency());
            //? if <26.1 {
            require(!p.disableCulling(), "entity_cutout <26.1 -> cull (disableCulling=false)");
            //?} else {
            require(p.disableCulling(), "entity_cutout >=26.1 -> no cull (disableCulling=true)");
            //?}

            // entity_cutout_no_cull -> ALPHA_TEST + no cull
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_cutout_no_cull"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.ALPHA_TEST, "entity_cutout_no_cull -> ALPHA_TEST");
            require(p.disableCulling(), "entity_cutout_no_cull -> no cull");

            // entity_translucent -> TRANSLUCENT + no cull
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_translucent"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.TRANSLUCENT, "entity_translucent -> TRANSLUCENT");
            require(p.disableCulling(), "entity_translucent -> no cull");

            // entity_translucent_cull -> TRANSLUCENT + cull
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_translucent_cull"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.TRANSLUCENT, "entity_translucent_cull -> TRANSLUCENT");
            require(!p.disableCulling(), "entity_translucent_cull -> cull");

            // entity_translucent_emissive -> TRANSLUCENT_EMISSIVE
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("entity_translucent_emissive"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.TRANSLUCENT_EMISSIVE,
                    "entity_translucent_emissive -> EMISSIVE: got " + p.transparency());

            // 非原版名仍走 SOLID 回退 + 警告，清理以免污染其它 smoke 的 WARNED 断言
            d = RenderTypeResolver.resolve(PortResourceLocation.parse("eyelib:not_a_vanilla_type"));
            p = d.factory().apply(tex);
            require(p.transparency() == Transparency.SOLID, "unknown -> SOLID fallback: got " + p.transparency());
            require(d.isSolid(), "unknown -> isSolid=true");
            require(RenderTypeResolver.WARNED_UNKNOWN_RENDER_TYPES.contains("eyelib:not_a_vanilla_type"),
                    "unknown type should be warned");
            RenderTypeResolver.WARNED_UNKNOWN_RENDER_TYPES.remove("eyelib:not_a_vanilla_type");
        }

        LOGGER.info("[RenderTypeBridgeSmoke] All bridge adapter + vanilla fallback paths passed");
    }
}

