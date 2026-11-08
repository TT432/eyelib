package io.github.tt432.eyelib.bridge.client.render.adapter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
//? if <1.20.6 {
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.client.ForgeHooksClient;
//?} else {
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?}
//?} else {
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
//?}

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * JE 人形盔甲的几何与纹理解析。
 * <p>
 * 基岩版的盔甲是 attachable：几何由引擎提供（{@code geometry.humanoid.armor.*}），骨骼按名称绑定到持有者
 * 同名 body-part 骨骼。JE 的对应素材是 {@code HumanoidModel} 的盔甲部件（每个部件的 {@code PartPose} 就在关节上，
 * 与基岩版骨骼 pivot 同义）加上物品自带的盔甲纹理。本类把两者解析成「部件名 + 纹理 + 顶点色」的扁平列表，
 * 骨骼绑定留给应用层（它持有持有者模型的骨骼位姿）。
 * <p>
 * 部件名沿用 vanilla 命名（{@code head}/{@code hat}/{@code body}/{@code right_arm}/…），应用层负责映射到
 * 基岩版骨骼名。几何按槽位烘焙一次并缓存：每槽位一份 {@code ModelPart} 实例，避免槽位之间部件集合互相污染。
 *
 * @author TT432
 */
final class ArmorRenderAdapter {
    private ArmorRenderAdapter() {
    }

    /** 各槽位要绘制的 vanilla 部件（与 vanilla {@code HumanoidArmorLayer.setPartVisibility} 等价）。 */
    private static final EnumMap<EquipmentSlot, String[]> PARTS = new EnumMap<>(EquipmentSlot.class);

    static {
        //? if <26.1 {
        PARTS.put(EquipmentSlot.HEAD, new String[]{"head", "hat"});
        //?} else {
        // 26.1 的盔甲网格按槽位 retain 部件，且 hat 是 head 的子骨骼（渲染 head 会一并绘制）
        PARTS.put(EquipmentSlot.HEAD, new String[]{"head"});
        //?}
        PARTS.put(EquipmentSlot.CHEST, new String[]{"body", "right_arm", "left_arm"});
        PARTS.put(EquipmentSlot.LEGS, new String[]{"body", "right_leg", "left_leg"});
        PARTS.put(EquipmentSlot.FEET, new String[]{"right_leg", "left_leg"});
    }

    private static final EnumMap<EquipmentSlot, ModelPart> ROOTS = new EnumMap<>(EquipmentSlot.class);

    /**
     * 该槽位当前物品的盔甲部件列表。空列表 = 不绘制（物品不是该槽位的人形盔甲，或该槽位没有对应几何）。
     */
    static List<EntityRenderPorts.RenderSystemPort.ArmorRenderPiece> pieces(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return List.of();
        }

        List<ArmorLayer> layers = textureLayers(entity, slot, stack);
        if (layers.isEmpty()) {
            return List.of();
        }

        ModelPart root = root(slot);
        if (root == null) {
            return List.of();
        }

        String[] partNames = PARTS.get(slot);
        if (partNames == null) {
            return List.of();
        }
        List<EntityRenderPorts.RenderSystemPort.ArmorRenderPiece> pieces =
                new ArrayList<>(partNames.length * layers.size());
        for (ArmorLayer layer : layers) {
            for (String partName : partNames) {
                if (!root.hasChild(partName)) {
                    continue;
                }
                pieces.add(new ArmorPiece(partName, layer.texture(), layer.tint(), root.getChild(partName)));
            }
        }
        return pieces;
    }

    /** 纹理层：同一部件的几何可能叠加多层（如染色皮革的底色层 + 覆盖层）。 */
    private record ArmorLayer(PortResourceLocation texture, int tint) {
    }

    private static @Nullable ModelPart root(EquipmentSlot slot) {
        ModelPart cached = ROOTS.get(slot);
        if (cached != null) {
            return cached;
        }
        //? if <26.1 {
        ModelLayerLocation location = slot == EquipmentSlot.LEGS
                ? ModelLayers.PLAYER_INNER_ARMOR
                : ModelLayers.PLAYER_OUTER_ARMOR;
        ModelPart baked = Minecraft.getInstance().getEntityModels().bakeLayer(location);
        //?} else {
        ArmorModelSet<ModelLayerLocation> locations = ModelLayers.PLAYER_ARMOR;
        ModelPart baked = Minecraft.getInstance().getEntityModels().bakeLayer(locations.get(slot));
        //?}
        ROOTS.put(slot, baked);
        return baked;
    }

    private static List<ArmorLayer> textureLayers(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        //? if <1.20.6 {
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
            return List.of();
        }
        boolean inner = slot == EquipmentSlot.LEGS;
        String base = "textures/models/armor/" + armor.getMaterial().getName() + "_layer_" + (inner ? 2 : 1);
        if (!(armor instanceof DyeableLeatherItem dyeable)) {
            return List.of(new ArmorLayer(location(ForgeHooksClient.getArmorTexture(entity, stack, base + ".png", slot, null)), WHITE));
        }
        // 染色皮革：底色层带染料色，覆盖层（描边）不染色
        int rgb = dyeable.getColor(stack) & 0xFFFFFF;
        return List.of(
                new ArmorLayer(location(ForgeHooksClient.getArmorTexture(entity, stack, base + ".png", slot, null)), 0xFF000000 | rgb),
                new ArmorLayer(location(ForgeHooksClient.getArmorTexture(entity, stack, base + "_overlay.png", slot, "overlay")), WHITE));
        //?} elif <26.1 {
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
            return List.of();
        }
        ArmorMaterial material = armor.getMaterial().value();
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        int fallback = extensions.getDefaultDyeColor(stack);
        boolean inner = slot == EquipmentSlot.LEGS;
        List<ArmorLayer> layers = new ArrayList<>(material.layers().size());
        for (int i = 0; i < material.layers().size(); i++) {
            ArmorMaterial.Layer layer = material.layers().get(i);
            int tint = extensions.getArmorLayerTintColor(stack, entity, layer, i, fallback);
            if (tint == 0) {
                continue;
            }
            layers.add(new ArmorLayer(location(ClientHooks.getArmorTexture(entity, stack, layer, inner, slot)), tint));
        }
        return layers;
        //?} else {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot) {
            return List.of();
        }
        ResourceKey<EquipmentAsset> assetId = equippable.assetId().orElse(null);
        if (assetId == null) {
            return List.of();
        }
        EquipmentClientInfo.LayerType layerType = slot == EquipmentSlot.LEGS
                ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                : EquipmentClientInfo.LayerType.HUMANOID;
        List<EquipmentClientInfo.Layer> source = equipmentInfo(assetId).getLayers(layerType);
        if (source.isEmpty()) {
            return List.of();
        }
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        int fallback = extensions.getDefaultDyeColor(stack);
        List<ArmorLayer> layers = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            EquipmentClientInfo.Layer layer = source.get(i);
            int tint = extensions.getArmorLayerTintColor(stack, layer, i, fallback);
            if (tint == 0) {
                continue;
            }
            layers.add(new ArmorLayer(location(layer.getTextureLocation(layerType)), tint));
        }
        return layers;
        //?}
    }

    private static final int WHITE = 0xFFFFFFFF;

    //? if >=26.1 {
    private static final EquipmentClientInfo EMPTY_INFO = new EquipmentClientInfo(Map.of());
    private static final Map<Identifier, EquipmentClientInfo> EQUIPMENT_INFO = new HashMap<>();

    /**
     * 解析 {@code equipment/<id>.json}。宿主的 {@code EquipmentAssetManager} 实例在 {@code Minecraft} 构造函数里是
     * 局部变量、外部不可达，因此按同一份官方 codec 直接读资源；解析失败降级为「无盔甲层」，不影响实体本体渲染。
     */
    private static EquipmentClientInfo equipmentInfo(ResourceKey<EquipmentAsset> assetId) {
        Identifier id = assetId.identifier();
        EquipmentClientInfo cached = EQUIPMENT_INFO.get(id);
        if (cached != null) {
            return cached;
        }
        EquipmentClientInfo parsed = EMPTY_INFO;
        var resource = Minecraft.getInstance().getResourceManager()
                .getResource(Identifier.fromNamespaceAndPath(id.getNamespace(), "equipment/" + id.getPath() + ".json"));
        if (resource.isPresent()) {
            try (BufferedReader reader = resource.get().openAsReader()) {
                parsed = EquipmentClientInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .result().orElse(EMPTY_INFO);
            } catch (Exception ignored) {
                // 资源损坏：按无盔甲层降级
            }
        }
        EQUIPMENT_INFO.put(id, parsed);
        return parsed;
    }

    /** 资源包切换：装备素材缓存与烘焙几何一起失效。 */
    static void invalidate() {
        EQUIPMENT_INFO.clear();
        ROOTS.clear();
    }
    //?} else {
    /** 资源包切换：烘焙几何失效。 */
    static void invalidate() {
        ROOTS.clear();
    }
    //?}

    private static PortResourceLocation location(String path) {
        return PortResourceLocation.parse(path);
    }

    //? if <26.1 {
    private static PortResourceLocation location(ResourceLocation location) {
        return PortResourceLocation.of(location.getNamespace(), location.getPath());
    }
    //?} else {
    private static PortResourceLocation location(Identifier location) {
        return PortResourceLocation.of(location.getNamespace(), location.getPath());
    }
    //?}

    /**
     * 单个盔甲部件：几何处在 vanilla 模型空间（关节为原点）。{@code emit} 抵消部件自身的 {@code PartPose}
     * 偏移，使调用方给出的 pose 就是该部件的关节帧。
     */
    private record ArmorPiece(String part, PortResourceLocation texture, int tint, ModelPart modelPart)
            implements EntityRenderPorts.RenderSystemPort.ArmorRenderPiece {

        @Override
        public void emit(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
            Matrix4f local = new Matrix4f(pose.pose())
                    .translate(-modelPart.x / 16F, -modelPart.y / 16F, -modelPart.z / 16F);
            PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(local);
            //? if <1.20.6 {
            modelPart.render(poseStack, consumer, light, overlay,
                    ((tint >> 16) & 0xFF) / 255F, ((tint >> 8) & 0xFF) / 255F, (tint & 0xFF) / 255F,
                    ((tint >>> 24) & 0xFF) / 255F);
            //?} else {
            modelPart.render(poseStack, consumer, light, overlay, tint);
            //?}
        }
    }
}
