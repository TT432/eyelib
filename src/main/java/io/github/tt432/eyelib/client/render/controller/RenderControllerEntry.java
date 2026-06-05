package io.github.tt432.eyelib.client.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.capability.component.RenderControllerComponent;
import io.github.tt432.eyelib.client.render.texture.NativeImageIO;
import io.github.tt432.eyelib.client.render.texture.TextureLayerMerger;
import io.github.tt432.eyelibattachment.capability.ModelComponentInfo;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibmodel.GlobalBoneIdHandler;
import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibmolang.MolangMapEntry;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.type.*;
import io.github.tt432.eyelibutil.texture.TexturePaths;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * 渲染控制器条目。
 *
 * @author TT432
 */
@NullMarked
public record RenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        List<MolangMapEntry> materials,
        Map<String, MolangValue> part_visibility
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
            ).optionalFieldOf("part_visibility", Map.of()).forGetter(RenderControllerEntry::part_visibility)
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

    public ResourceLocation getTexture(MolangScope scope, BrClientEntity entity) {
        return composeTextureLocation(resolveTextureLayerPaths(scope, entity), "");
    }

    public ResourceLocation getEmissiveTexture(MolangScope scope, BrClientEntity entity) {
        List<String> layerPaths = resolveTextureLayerPaths(scope, entity);
        return composeTextureLocation(toEmissiveTextureLayerPaths(layerPaths), "");
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

        // 每个唯一材质创建一个 ModelComponent
        for (var groupEntry : materialBoneGroups.entrySet()) {
            String materialName = groupEntry.getKey();
            Set<Integer> visibleBones = groupEntry.getValue();

            ResourceLocation matTexture = resolveSlotTexture(scope, entity, materialName,
                    needReloadTexture, syncedActions);

            ModelComponent comp = new ModelComponent();
            comp.setInfo(new ModelComponentInfo(geometryResult, matTexture, new ResourceLocation(materialName)));

            Int2BooleanOpenHashMap vis = buildVisibility(allBoneIds, visibleBones);
            renderControllerSlot.runtime().evalPartVisibility(vis, scope);
            comp.getPartVisibility().putAll(vis);

            components.add(comp);
        }

        if (needReloadTexture) {
            renderControllerSlot.markTextureUploaded();
        }

        return components;
    }

    /**
     * 按当前材质名解析纹理。注入 {@code texture.material} 到 scope 中，
     * 使得 Bedrock 的 {@code "textures": ["texture.material"]} 表达式能按材质槽动态求值。
     */
    private ResourceLocation resolveSlotTexture(MolangScope scope, BrClientEntity entity,
                                                String materialName, boolean needReload,
                                                List<Runnable> syncedActions) {
        if (textures.isEmpty()) {
            return MissingTextureAtlasSprite.getLocation();
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
            List<String> textureLayerPaths = resolveTextureLayerPaths(scope, entity);
            ResourceLocation texture = getTexture(scope, entity);

            if (needReload) {
                List<ResourceLocation> textureLayers = toResourceLocations(textureLayerPaths);
                syncedActions.add(() -> NativeImageIO.upload(texture, TextureLayerMerger.merge(textureLayers)));

                ResourceLocation emissiveTexture = getEmissiveTexture(scope, entity);
                List<ResourceLocation> emissiveTextureLayers = toResourceLocations(toEmissiveTextureLayerPaths(textureLayerPaths));
                syncedActions.add(() -> NativeImageIO.upload(emissiveTexture, TextureLayerMerger.merge(emissiveTextureLayers)));
            }

            return texture;
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
    private static Set<Integer> matchBonePattern(String pattern, Collection<Model> models) {
        Set<Integer> result = new HashSet<>();
        boolean isPrefix = pattern.endsWith("*");
        String lookup = isPrefix ? pattern.substring(0, pattern.length() - 1) : pattern;

        for (Model model : models) {
            if (model == null) continue;
            for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
                int boneId = boneEntry.getIntKey();
                if (boneId < 0) continue;
                String boneName = GlobalBoneIdHandler.get(boneId);
                if (boneName == null) continue;
                // 纯 "*" 匹配全部，空前缀也匹配全部
                boolean match = pattern.equals("*") || lookup.isEmpty()
                        || (isPrefix ? boneName.startsWith(lookup) : boneName.equals(lookup));
                if (match) {
                    result.add(boneId);
                }
            }
        }
        return result;
    }

    String get(MolangScope scope, MolangValue value, String type, Map<String, String> map) {
        MolangObject object = value.getObject(scope);

        if (object instanceof MolangNull) {
            var r = map.get(value.context().toLowerCase(Locale.ROOT).replace(type + ".", ""));
            return Objects.requireNonNullElse(r, "minecraft:null");
        } else if (object instanceof MolangString || object instanceof MolangDynamicObject) {
            return Objects.requireNonNullElse(object.asString(), "minecraft:null");
        } else if (object instanceof MolangArray) {
            return "minecraft:null";
        } else {
            var r = map.get(object.asString().toLowerCase(Locale.ROOT).replace(type + ".", ""));
            return Objects.requireNonNullElse(r, "minecraft:null");
        }
    }

    private ResourceLocation composeTextureLocation(List<String> layerPaths, String suffix) {
        StringBuilder pathBuilder = new StringBuilder();
        for (String layerPath : layerPaths) {
            pathBuilder.append(layerPath);
        }
        return new ResourceLocation("complex", pathBuilder.toString().replace(":", "_") + suffix);
    }

    private List<String> resolveTextureLayerPaths(MolangScope scope, BrClientEntity entity) {
        List<String> layerPaths = new ArrayList<>(textures.size());
        for (MolangValue texture : textures) {
            layerPaths.add(get(scope, texture, "texture", entity.textures()));
        }
        return layerPaths;
    }

    private List<String> toEmissiveTextureLayerPaths(List<String> layerPaths) {
        List<String> emissiveLayerPaths = new ArrayList<>(layerPaths.size());
        for (String layerPath : layerPaths) {
            emissiveLayerPaths.add(TexturePaths.emissivePath(layerPath));
        }
        return emissiveLayerPaths;
    }

    private List<ResourceLocation> toResourceLocations(List<String> layerPaths) {
        List<ResourceLocation> resourceLocations = new ArrayList<>(layerPaths.size());
        for (String layerPath : layerPaths) {
            resourceLocations.add(new ResourceLocation(layerPath));
        }
        return resourceLocations;
    }
}
