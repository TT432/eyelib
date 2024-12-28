package io.github.tt432.eyelib.client.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.type.MolangArray;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.util.client.NativeImages;
import io.github.tt432.eyelib.util.client.Textures;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * @param part_visibility TODO
 * @author TT432
 */
public record RenderControllerEntry(
        MolangValue geometry,
        List<MolangValue> textures,
        Map<String, Map<String, List<String>>> arrays,
        Map<String, MolangValue> materials,
        Map<String, MolangValue> part_visibility
) {
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

    public void initArrays(MolangScope scope) {
        for (Map<String, List<String>> value : arrays.values()) {
            value.forEach((name, list) -> {
                List<MolangString> result = new ArrayList<>();
                for (String s : list) {
                    result.add(new MolangString(s));
                }
                scope.set(name.toLowerCase(Locale.ROOT), new MolangArray<>(result));
            });
        }
    }

    public ResourceLocation getTexture(MolangScope scope, BrClientEntity entity) {
        StringBuilder sb = new StringBuilder();
        for (MolangValue mv : textures) {
            String s = entity.textures().get(mv.getObject(scope).asString()
                    .toLowerCase(Locale.ROOT).replace("texture.", ""));
            sb.append(s);
        }
        return ResourceLocation.fromNamespaceAndPath("complex", sb.toString().replace(":", "_") + ".png");
    }

    public ResourceLocation getEmissiveTexture(MolangScope scope, BrClientEntity entity) {
        StringBuilder sb = new StringBuilder();
        for (MolangValue mv : textures) {
            String s = entity.textures().get(mv.getObject(scope).asString()
                    .toLowerCase(Locale.ROOT).replace("texture.", ""));
            sb.append(s);
        }
        return ResourceLocation.fromNamespaceAndPath("complex", sb.toString().replace(":", "_") + ".emissive.png");
    }

    public void setupModel(MolangScope scope, BrClientEntity entity, ModelComponent component) {
        initArrays(scope);
        ResourceLocation texture;

        if (!textures.isEmpty()) {
            texture = getTexture(scope, entity);

            if (Minecraft.getInstance().getTextureManager().getTexture(texture) == MissingTextureAtlasSprite.getTexture()) {
                List<ResourceLocation> list = new ArrayList<>();
                for (MolangValue mv : textures) {
                    ResourceLocation parse = ResourceLocation.parse(entity.textures().get(mv.getObject(scope).asString().toLowerCase(Locale.ROOT).replace("texture.", "") instanceof String s ? s : "minecraft:missingno"));
                    list.add(parse);
                }
                NativeImages.uploadImage(texture, Textures.layerMerging(list));
            }

            ResourceLocation emissiveTexture = getEmissiveTexture(scope, entity);

            if (Minecraft.getInstance().getTextureManager().getTexture(emissiveTexture) == MissingTextureAtlasSprite.getTexture()) {
                List<ResourceLocation> list = new ArrayList<>();
                for (MolangValue mv : textures) {
                    ResourceLocation resourceLocation = ResourceLocation.parse(entity.textures().get(mv.getObject(scope).asString().toLowerCase(Locale.ROOT).replace("texture.", "") instanceof String s ? s : "minecraft:missingno")).withPath(s -> replacePng(s, ".png", ".emissive.png"));
                    list.add(resourceLocation);
                }
                NativeImages.uploadImage(emissiveTexture, Textures.layerMerging(list));
            }
        } else {
            texture = MissingTextureAtlasSprite.getLocation();
        }

        ModelComponent.SerializableInfo serializableInfo = component.getSerializableInfo();

        if (serializableInfo == null) {
            component.setInfo(new ModelComponent.SerializableInfo(
                    entity.geometry().get(geometry.getObject(scope).asString()),
                    texture,
                    ResourceLocation.parse(materials.containsKey("*") ? get(scope, materials.get("*"), "material", entity.materials()) : ""),
                    List.of()
            ));
        } else {
            if (geometry != MolangValue.ZERO)
                serializableInfo = serializableInfo.withModel(get(scope, geometry, "geometry", entity.geometry()));
            if (!textures.isEmpty())
                serializableInfo = serializableInfo.withTexture(texture);
            var material = materials.containsKey("*") ? get(scope, materials.get("*"), "material", entity.materials()) : "";
            if (material != null && !materials.isEmpty() && !material.isEmpty())
                serializableInfo = serializableInfo.withRenderType(ResourceLocation.parse(material));

            component.setInfo(serializableInfo);
        }
    }

    String get(MolangScope scope, MolangValue value, String type, Map<String, String> map) {
        MolangObject object = value.getObject(scope);

        if (object instanceof MolangNull) {
            var r = map.get(value.context().toLowerCase(Locale.ROOT).replace(type + ".", ""));

            return Objects.requireNonNullElse(r, "minecraft:null");
        } else {
            var r = map.get(object.asString().toLowerCase(Locale.ROOT).replace(type + ".", ""));

            return Objects.requireNonNullElse(r, "minecraft:null");
        }
    }

    static String replacePng(String originalString, String old, String newStr) {
        int lastIndexOfDot = originalString.lastIndexOf(old);

        if (lastIndexOfDot != -1) {
            String beforeDot = originalString.substring(0, lastIndexOfDot);
            return beforeDot + newStr;
        } else {
            return originalString;
        }
    }
}
