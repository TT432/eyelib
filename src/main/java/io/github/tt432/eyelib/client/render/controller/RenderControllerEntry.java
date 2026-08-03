package io.github.tt432.eyelib.client.render.controller;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort;
import io.github.tt432.eyelib.bridge.client.render.texture.TexturePresencePort;
import io.github.tt432.eyelib.util.entitydata.ModelComponentInfo;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.importer.render.controller.BrRcColor;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangMapEntry;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.type.*;
import io.github.tt432.eyelib.util.PortResourceLocation;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.util.*;

/**
 * 渲染控制器条目。
 *
 * @author TT432
 */
public record RenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        List<MolangMapEntry> materials,
        Map<String, MolangValue> part_visibility,
        boolean ignoreLighting,
        Optional<BrRcColor> color,
        Optional<BrRcColor> isHurtColor,
        Optional<BrRcColor> onFireColor,
        Optional<BrRcColor> overlayColor
) {
    public static final Codec<RenderControllerEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("geometry", MolangValue.ZERO).forGetter(RenderControllerEntry::geometry),
            MolangValue.CODEC.listOf()
                             .optionalFieldOf("textures", List.of())
                             .forGetter(RenderControllerEntry::textures),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()))
                  .optionalFieldOf("arrays", Map.of())
                  .forGetter(RenderControllerEntry::arrays),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    RenderControllerEntry::toMolangMapEntries,
                    RenderControllerEntry::fromMolangMapEntries
            ).optionalFieldOf("materials", List.of()).forGetter(RenderControllerEntry::materials),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    l -> {
                        Map<String, MolangValue> result = new LinkedHashMap<>();
                        for (Map<String, MolangValue> map : l) {
                            result.putAll(map);
                        }
                        return result;
                    },
                    List::of
            ).optionalFieldOf("part_visibility", Map.of()).forGetter(RenderControllerEntry::part_visibility),
            Codec.BOOL.optionalFieldOf("ignore_lighting", false).forGetter(RenderControllerEntry::ignoreLighting),
            BrRcColor.CODEC.optionalFieldOf("color").forGetter(RenderControllerEntry::color),
            BrRcColor.CODEC.optionalFieldOf("is_hurt_color").forGetter(RenderControllerEntry::isHurtColor),
            BrRcColor.CODEC.optionalFieldOf("on_fire_color").forGetter(RenderControllerEntry::onFireColor),
            BrRcColor.CODEC.optionalFieldOf("overlay_color").forGetter(RenderControllerEntry::overlayColor)
    ).apply(ins, RenderControllerEntry::new));

    private static List<MolangMapEntry> toMolangMapEntries(List<Map<String, MolangValue>> list) {
        List<MolangMapEntry> result = new ArrayList<>();
        for (Map<String, MolangValue> map : list) {
            for (var entry : map.entrySet()) {
                result.add(new MolangMapEntry(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private static List<Map<String, MolangValue>> fromMolangMapEntries(List<MolangMapEntry> entries) {
        return entries.stream()
                      .map(e -> Map.of(e.key(), e.value()))
                      .toList();
    }

    public void initArrays(MolangScope scope, BrClientEntity clientEntity) {
        for (Map<String, List<String>> value : arrays.values()) {
            value.forEach((name, list) -> {
                List<MolangDynamicObject> result = new ArrayList<>();
                for (String s : list) {
                    result.add(new MolangDynamicObject(() -> scope.get(s.toLowerCase(Locale.ROOT))));
                }
                scope.set(name.toLowerCase(Locale.ROOT), new MolangArray<>(result));
            });
        }

        clientEntity.textures().forEach((name, value) -> {
            scope.set("texture." + name.toLowerCase(Locale.ROOT), new MolangString(value));
        });

        clientEntity.geometry().forEach((name, value) -> {
            scope.set("geometry." + name.toLowerCase(Locale.ROOT), new MolangString(value));
        });

        clientEntity.materials().forEach((name, value) -> {
            scope.set("material." + name.toLowerCase(Locale.ROOT), new MolangString(value));
        });
    }

    public List<ModelComponent> setupModel(MolangScope scope, BrClientEntity entity,
                                           Collection<Model> models,
                                           RenderControllerComponent.Slot renderControllerSlot,
                                           List<Runnable> syncedActions) {
        initArrays(scope, entity);

        var geometryResult = get(scope, geometry, "geometry", entity.geometry());
        if ("minecraft:null".equals(geometryResult)) {
            var defaultGeo = entity.geometry().get("default");
            geometryResult = defaultGeo != null ? defaultGeo
                    : entity.geometry().values().stream().findFirst().orElse("minecraft:null");
        }

        List<ModelComponent> components = new ArrayList<>();

        // 收集所有骨骼
        Set<Integer> allBoneIds = new HashSet<>();
        for (Model model : models) {
            if (model != null) {
                for (int id : model.allBones().keySet()) {
                    if (id >= 0) allBoneIds.add(id);
                }
            }
        }

        // 按 materials 数组顺序处理所有槽位，后面覆盖前面（Bedrock "Saddle will override Mane" 语义）
        // boneId → materialName
        Map<Integer, String> boneMaterialMap = new LinkedHashMap<>();

        for (var entry : materials) {
            String pattern = entry.key();
            String materialName = get(scope, entry.value(), "material", entity.materials());
            Set<Integer> matchedBones = matchBonePattern(pattern, models);

            for (int boneId : matchedBones) {
                boneMaterialMap.put(boneId, materialName);
            }
        }

        if (boneMaterialMap.isEmpty()) {
            return components;
        }

        // 按材质名分组骨骼
        Map<String, Set<Integer>> materialBoneGroups = new LinkedHashMap<>();
        for (var entry : boneMaterialMap.entrySet()) {
            materialBoneGroups.computeIfAbsent(entry.getValue(), k -> new LinkedHashSet<>()).add(entry.getKey());
        }

        // 预计算全局 part_visibility（只构造一次，所有组件复用）
        renderControllerSlot.runtime().setup(models, this);

        boolean needReloadTexture = renderControllerSlot.needsTextureReload();

        float[] rcColor = evalRcColor(scope);

        // texture_meshes 体素化贴图（BE 语义：形状由 mesh 短名指定的贴图决定，与图层无关）
        PortResourceLocation meshTexture = resolveMeshTexture(models, entity, geometryResult);

        // 每组骨骼先解析各自的图层纹理列表（texture.material 注入依赖组材质）
        // BE 语义：textures 数组 = 多图层，按数组顺序逐层渲染同一几何（先底层后顶层），不做贴图合并
        Map<String, List<PortResourceLocation>> texturesByGroup = new LinkedHashMap<>();
        int maxLayers = 1;
        for (var groupEntry : materialBoneGroups.entrySet()) {
            List<PortResourceLocation> layers = resolveSlotTextures(scope, entity, groupEntry.getKey(),
                                                                    needReloadTexture, syncedActions);
            // BE 语义：多图层纹理需要 multitexture/masked 材质（多采样器）；
            // 单采样材质只渲染第 0 层（bedrock-wiki 分层教程：需 villager_v2_masked 类材质）
            if (!isMultitextureMaterial(groupEntry.getKey()) && layers.size() > 1) {
                layers = layers.subList(0, 1);
            }
            texturesByGroup.put(groupEntry.getKey(), layers);
            maxLayers = Math.max(maxLayers, layers.size());
        }

        // 层在外、组在内：第 i 层渲染全模型的所有材质组，再渲染第 i+1 层
        for (int layer = 0; layer < maxLayers; layer++) {
            for (var groupEntry : materialBoneGroups.entrySet()) {
                String materialName = groupEntry.getKey();
                Set<Integer> visibleBones = groupEntry.getValue();

                // 不变量：texturesByGroup 与 materialBoneGroups 同键（上方同一循环构建）
                List<PortResourceLocation> layers = java.util.Objects.requireNonNull(texturesByGroup.get(materialName));
                PortResourceLocation matTexture = layers.get(Math.min(layer, layers.size() - 1));

                ModelComponent comp = new ModelComponent();
                comp.setInfo(new ModelComponentInfo(geometryResult, matTexture,
                        PortResourceLocation.parse(materialName)
                ));
                comp.setIgnoreLighting(ignoreLighting);
                comp.setRcColor(rcColor);
                comp.setMeshTexture(meshTexture);

                Int2BooleanOpenHashMap vis = buildVisibility(allBoneIds, visibleBones);
                renderControllerSlot.runtime().evalPartVisibility(vis, scope);
                comp.getPartVisibility().putAll(vis);

                components.add(comp);
            }
        }

        if (needReloadTexture) {
            renderControllerSlot.markTextureUploaded();
        }

        return components;
    }

    /**
     * 解析 texture_meshes 的体素化贴图：仅在当前 RC 选中的几何上收集 texture_mesh 短名，经实体纹理表解析。
     * 仅当所有 mesh 解析到同一路径时返回该路径；无 mesh 或多路径时返回 null（回退组件图层贴图）。
     */
    private static @org.jspecify.annotations.Nullable PortResourceLocation resolveMeshTexture(Collection<Model> models, BrClientEntity entity, String geometryName) {
        String resolved = null;
        for (Model model : models) {
            if (model == null || !model.name().equals(geometryName)) continue;
            for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
                for (Model.TextureMesh tm : boneEntry.getValue().textureMeshes()) {
                    String path = entity.textures().get(tm.texture());
                    if (path == null) {
                        path = entity.textures().get("default");
                    }
                    if (path == null) continue;
                    if (resolved == null) {
                        resolved = path;
                    } else if (!resolved.equals(path)) {
                        return null;
                    }
                }
            }
        }
        return resolved == null ? null : PortResourceLocation.parse(resolved);
    }

    /**
     * 判断材质是否支持多图层纹理（BE multitexture/masked 材质族，多采样器）。
     * 命中途径：USE_COLOR_MASK / MASKED_MULTITEXTURE define，或名称含 multitexture/masked。
     */
    private static boolean isMultitextureMaterial(String materialName) {
        if (usesColorMask(materialName)) {
            return true;
        }
        String name = materialName.toLowerCase(Locale.ROOT);
        if (name.contains("multitexture") || name.contains("masked")) {
            return true;
        }
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, materialName).orElse(null);
        if (entry == null) {
            return false;
        }
        try {
            return BrMaterialResolver.resolve(entry, matMap).hasDefine("MASKED_MULTITEXTURE");
        } catch (IllegalStateException exception) {
            return entry.defines().add().stream().flatMap(Collection::stream).anyMatch("MASKED_MULTITEXTURE"::equals);
        }
    }

    /**
     * 求值 RC 的 color 系列字段，返回 vertex color。
     *
     * is_hurt_color 仅在实体受伤时（hurtTime &gt; 0）应用，
     * on_fire_color 仅在实体着火时应用，
     * overlay_color 和 color 每帧应用。
     * 优先级：is_hurt &gt; on_fire &gt; overlay &gt; color。
     */
    @SuppressWarnings("deprecation")
    private float @org.jspecify.annotations.Nullable [] evalRcColor(MolangScope scope) {
        net.minecraft.world.entity.LivingEntity entity = scope.getHostContext()
                .get(io.github.tt432.eyelib.capability.RenderData.class)
                .map(rd -> rd.getOwner())
                .filter(net.minecraft.world.entity.LivingEntity.class::isInstance)
                .map(net.minecraft.world.entity.LivingEntity.class::cast)
                .orElse(null);

        boolean isHurt = entity != null && entity.hurtTime > 0;
        boolean isOnFire = entity != null && entity.displayFireAnimation();

        if (isHurt && isHurtColor.isPresent()) return isHurtColor.get().eval(scope);
        if (isOnFire && onFireColor.isPresent()) return onFireColor.get().eval(scope);
        // overlay_color 是受伤/特殊状态覆盖层的颜色（官方文档：配合 query.overlay_alpha 的 overlay 效果），
        // 不得作为常驻顶点染色——否则 A&S 发光层（overlay rgb=0）会被永久乘黑。
        if (color.isPresent()) return color.get().eval(scope);
        return null;
    }

    /**
     * 按当前材质名解析全部图层纹理。注入 {@code texture.material} 到 scope 中，
     * 使得 Bedrock 的 {@code "textures": ["texture.material"]} 表达式能按材质槽动态求值。
     * 返回顺序与 RC 的 textures 数组一致（先底层后顶层）。
     */
    private List<PortResourceLocation> resolveSlotTextures(MolangScope scope, BrClientEntity entity,
                                                           String materialName, boolean needReload,
                                                           List<Runnable> syncedActions) {
        if (textures.isEmpty()) {
            return List.of(TexturePresencePort.missingLocation());
        }

        // 按材质名查找实体纹理表，作为 texture.material 的动态值
        String texPath = entity.textures().get(materialName);
        if (texPath == null) {
            texPath = entity.textures().get("default");
        }

        MolangObject savedValue = scope.get("texture.material");
        boolean hadOldValue = !(savedValue instanceof MolangNull);

        if (texPath != null) {
            scope.set("texture.material", new MolangString(texPath));
        }

        try {
            List<PortResourceLocation> textureLayers = toPortLocations(resolveTextureLayerPaths(scope, entity));

            // alphatest 材质逐层走 clamped 纹理，避免 MC cutout threshold 0.1/0.5 丢弃低 alpha 像素；
            // emissive 材质同理：BE 发光着色器不丢弃低 alpha（alpha 是发光掩码），
            // 而 MC entityTranslucent 着色器在 alpha<0.1 时 discard（A&S 蜘蛛红眼 alpha 仅 1-10 会整体消失）
            if (!usesColorMask(materialName) && (isAlphatestMaterial(materialName) || isEmissiveMaterial(materialName))) {
                List<PortResourceLocation> clamped = new ArrayList<>(textureLayers.size());
                for (PortResourceLocation layer : textureLayers) {
                    clamped.add(clampedTexture(layer, syncedActions, needReload));
                }
                return clamped;
            }
            return textureLayers;
        } finally {
            if (texPath != null) {
                if (hadOldValue) {
                    scope.set("texture.material", savedValue);
                } else {
                    scope.remove("texture.material");
                }
            }
        }
    }

    /**
     * 创建单层 clamped 纹理副本：download 原纹理，clamp alpha，再上传。
     * 避免 premultiplied alpha 把低 alpha 颜色乘成黑色。
     * alphatest 材质使用此副本以避免低 alpha 像素被 MC cutout shader 丢弃。
     */
    private static PortResourceLocation clampedTexture(PortResourceLocation original,
                                                       List<Runnable> syncedActions, boolean needReload) {
        PortResourceLocation clamped = PortResourceLocation.of(original.namespace(), "clamped/" + original.path());
        if (needReload) {
            syncedActions.add(() -> {
                NativeImage img = NativeImagePort.download(original, NativeImagePort::copyImage);
                if (img != null) {
                    NativeImagePort.clampAlphaToBinary(img);
                    NativeImagePort.upload(clamped, img);
                }
            });
        }
        return clamped;
    }

    /**
     * 判断材质是否为 alphatest（非 blending）。
     * alphatest 材质需要 clamp alpha 以解决 MC cutout shader threshold 0.5
     * 丢弃 Bedrock 低 alpha 像素的问题。
     */
    /**
     * 判断材质是否为发光材质（解析链上含 USE_EMISSIVE / USE_ONLY_EMISSIVE define）。
     * 发光材质的纹理 alpha 是掩码而非半透明，需二值化以防 MC 着色器按阈值丢弃。
     */
    private static boolean isEmissiveMaterial(String materialName) {
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, materialName).orElse(null);
        if (entry == null) {
            return false;
        }
        try {
            var resolved = BrMaterialResolver.resolve(entry, matMap);
            return resolved.hasDefine("USE_EMISSIVE") || resolved.hasDefine("USE_ONLY_EMISSIVE");
        } catch (IllegalStateException exception) {
            return entry.defines().add().stream().flatMap(Collection::stream)
                        .anyMatch(d -> "USE_EMISSIVE".equals(d) || "USE_ONLY_EMISSIVE".equals(d));
        }
    }

    private static boolean isAlphatestMaterial(String materialName) {
        var matMap = MaterialManager.INSTANCE.all();
        var entry = matMap.get(materialName);
        if (entry == null) {
            // 尝试 suffix 查找
            for (var e : matMap.entrySet()) {
                String key = e.getKey();
                int idx = key.lastIndexOf(':');
                if (idx >= 0 && key.substring(idx + 1).equals(materialName)) {
                    entry = e.getValue();
                    break;
                }
            }
        }
        if (entry == null) {
            // 尝试 name 字段查找
            for (var e : matMap.entrySet()) {
                if (e.getValue().name().equals(materialName)) {
                    entry = e.getValue();
                    break;
                }
            }
        }
        return entry != null && io.github.tt432.eyelib.bridge.material.RenderTypeResolver.isAlphaTest(entry, matMap);
    }

    private static boolean usesColorMask(String materialName) {
        var matMap = MaterialManager.INSTANCE.all();
        var entry = BrMaterialResolver.find(matMap, materialName).orElse(null);
        if (entry == null) {
            return false;
        }
        try {
            return BrMaterialResolver.resolve(entry, matMap).hasDefine("USE_COLOR_MASK");
        } catch (IllegalStateException exception) {
            return entry.defines().add().stream().flatMap(Collection::stream).anyMatch("USE_COLOR_MASK"::equals);
        }
    }

    /**
     * 构建partVisibility：全骨骼初始为false，仅指定集合设为true。
     */
    private static Int2BooleanOpenHashMap buildVisibility(Set<Integer> allBoneIds, Set<Integer> visibleBones) {
        Int2BooleanOpenHashMap vis = new Int2BooleanOpenHashMap();
        for (int id : allBoneIds) {
            vis.put(id, visibleBones.contains(id));
        }
        return vis;
    }

    /**
     * 按Bedrock骨骼名模式匹配模型中的骨骼。
     * {@code *}后缀表示前缀匹配，无后缀表示精确匹配。
     * {@code *}单独出现时匹配全部骨骼。
     */
    /**
     * 判定骨骼名是否匹配 Bedrock 模式。
     * 后缀 {@code *} 表示前缀匹配，单独 {@code *} 或空前缀匹配全部，否则精确匹配。
     */
    public static boolean matchesBonePattern(String pattern, String boneName) {
        boolean isPrefix = pattern.endsWith("*");
        String lookup = isPrefix ? pattern.substring(0, pattern.length() - 1) : pattern;
        return pattern.equals("*") || lookup.isEmpty()
                || (isPrefix ? boneName.startsWith(lookup) : boneName.equals(lookup));
    }

    private static Set<Integer> matchBonePattern(String pattern, Collection<Model> models) {
        Set<Integer> result = new HashSet<>();
        for (Model model : models) {
            if (model == null) continue;
            for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
                int boneId = boneEntry.getIntKey();
                if (boneId < 0) continue;
                String boneName = GlobalBoneIdHandler.get(boneId);
                if (boneName == null) continue;
                if (matchesBonePattern(pattern, boneName)) {
                    result.add(boneId);
                }
            }
        }
        return result;
    }

    String get(MolangScope scope, MolangValue value, String type, Map<String, String> map) {
        MolangObject object = value.getObject(scope);

        if (object instanceof MolangNull) {
            // 只剥首个类别前缀（多点短名可能含同类片段，全量 replace 会误剥——规格 D7）
            var r = map.get(stripTypePrefix(value.context(), type));
            return Objects.requireNonNullElse(r, "minecraft:null");
        } else if (object instanceof MolangString || object instanceof MolangDynamicObject) {
            return Objects.requireNonNullElse(object.asString(), "minecraft:null");
        } else if (object instanceof MolangArray) {
            return "minecraft:null";
        } else {
            var r = map.get(stripTypePrefix(object.asString(), type));
            return Objects.requireNonNullElse(r, "minecraft:null");
        }
    }

    /** 剥首个 "<type>." 前缀并 lowercase（原 replace 全量替换对多点短名有误剥风险）。 */
    private static String stripTypePrefix(String raw, String type) {
        String s = raw.toLowerCase(Locale.ROOT);
        String prefix = type + ".";
        return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
    }

    private List<String> resolveTextureLayerPaths(MolangScope scope, BrClientEntity entity) {
        List<String> layerPaths = new ArrayList<>(textures.size());
        for (MolangValue texture : textures) {
            layerPaths.add(get(scope, texture, "texture", entity.textures()));
        }
        return layerPaths;
    }


    private List<PortResourceLocation> toPortLocations(List<String> layerPaths) {
        List<PortResourceLocation> resourceLocations = new ArrayList<>(layerPaths.size());
        for (String layerPath : layerPaths) {
            resourceLocations.add(PortResourceLocation.parse(layerPath));
        }
        return resourceLocations;
    }
}
