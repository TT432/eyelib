package io.github.tt432.eyelib.client.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.entity.RenderControllerRuntime;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.type.*;
import io.github.tt432.eyelib.client.render.texture.NativeImageIO;
import io.github.tt432.eyelib.client.render.texture.TextureLayerMerger;
import io.github.tt432.eyelib.util.client.texture.TexturePathHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author TT432
 */
public record RenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        Map<String, MolangValue> materials,
        Map<String, MolangValue> part_visibility,
        AtomicBoolean needReloadTexture,
        RenderControllerRuntime renderControllerRuntime
) {
    public RenderControllerEntry(
            MolangValue geometry,
            List<MolangValue> textures,
            Map<String, Map<String, List<String>>> arrays,
            Map<String, MolangValue> materials,
            Map<String, MolangValue> part_visibility
    ) {
        this(geometry, textures, arrays, materials, part_visibility, new AtomicBoolean(true), new RenderControllerRuntime());
    }

    public static final Codec<RenderControllerEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("geometry", MolangValue.ZERO).forGetter(RenderControllerEntry::geometry),
            MolangValue.CODEC.listOf().optionalFieldOf("textures", List.of()).forGetter(RenderControllerEntry::textures),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())).optionalFieldOf("arrays", Map.of()).forGetter(RenderControllerEntry::arrays),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    l -> {
                        Map<String, MolangValue> result = new Object2ObjectOpenHashMap<>();
                        for (Map<String, MolangValue> map : l) {
                            for (Map.Entry<String, MolangValue> stringMolangValueEntry : map.entrySet()) {
                                if (result.put(stringMolangValueEntry.getKey(), stringMolangValueEntry.getValue()) != null) {
                                    throw new IllegalStateException("Duplicate key");
                                }
                            }
                        }
                        return result;
                    },
                    List::of
            ).optionalFieldOf("materials", Map.of()).forGetter(RenderControllerEntry::materials),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    l -> {
                        Map<String, MolangValue> result = new Object2ObjectOpenHashMap<>();
                        for (Map<String, MolangValue> map : l) {
                            for (Map.Entry<String, MolangValue> stringMolangValueEntry : map.entrySet()) {
                                if (result.put(stringMolangValueEntry.getKey(), stringMolangValueEntry.getValue()) != null) {
                                    throw new IllegalStateException("Duplicate key");
                                }
                            }
                        }
                        return result;
                    },
                    List::of
            ).optionalFieldOf("part_visibility", Map.of()).forGetter(RenderControllerEntry::part_visibility)
    ).apply(ins, RenderControllerEntry::new));

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
        return composeTextureLocation(resolveTextureLayerPaths(scope, entity), ".png");
    }

    public ResourceLocation getEmissiveTexture(MolangScope scope, BrClientEntity entity) {
        return composeTextureLocation(resolveTextureLayerPaths(scope, entity), ".emissive.png");
    }

    public RenderControllerEntry {
        MinecraftForge.EVENT_BUS.<ManagerEntryChangedEvent>addListener(e -> {
            if (e.getManagerName().equals("Texture")) {
                needReloadTexture.set(true);
            }
        });
    }

    public ModelComponent setupModel(MolangScope scope, BrClientEntity entity, List<Runnable> syncedActions) {
        initArrays(scope, entity);
        ResourceLocation texture;

        if (!textures.isEmpty()) {
            List<String> textureLayerPaths = resolveTextureLayerPaths(scope, entity);
            texture = getTexture(scope, entity);

            ResourceLocation emissiveTexture = getEmissiveTexture(scope, entity);

            boolean needReloadTexture = this.needReloadTexture.get();

            if (needReloadTexture) {
                List<ResourceLocation> textureLayers = toResourceLocations(textureLayerPaths);
                syncedActions.add(() -> NativeImageIO.upload(texture, TextureLayerMerger.merge(textureLayers)));

                List<ResourceLocation> emissiveTextureLayers = toResourceLocations(toEmissiveTextureLayerPaths(textureLayerPaths));
                syncedActions.add(() -> NativeImageIO.upload(emissiveTexture, TextureLayerMerger.merge(emissiveTextureLayers)));
            }

            this.needReloadTexture.set(false);
        } else {
            texture = MissingTextureAtlasSprite.getLocation();
        }

        ModelComponent component = new ModelComponent();

        component.setInfo(new ModelComponent.SerializableInfo(
                get(scope, geometry, "geometry", entity.geometry()),
                texture,
                new ResourceLocation(materials.containsKey("*") ? get(scope, materials.get("*"), "material", entity.materials()) : "")
        ));

        var partVisibility = component.getPartVisibility();
        renderControllerRuntime.evalPartVisibility(entity, this, partVisibility, scope);

        return component;
    }

    String get(MolangScope scope, MolangValue value, String type, Map<String, String> map) {
        MolangObject object = value.getObject(scope);

        if (object instanceof MolangNull) {
            var r = map.get(value.context().toLowerCase(Locale.ROOT).replace(type + ".", ""));

            return Objects.requireNonNullElse(r, "minecraft:null");
        } else if (object instanceof MolangString || object instanceof MolangDynamicObject) {
            return Objects.requireNonNullElse(object.asString(), "minecraft:null");
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
            emissiveLayerPaths.add(TexturePathHelper.getEmissiveTexturePath(layerPath));
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
