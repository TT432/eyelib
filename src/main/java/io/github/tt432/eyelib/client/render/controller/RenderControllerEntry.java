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
import io.github.tt432.eyelib.util.client.NativeImages;
import io.github.tt432.eyelib.util.client.Textures;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

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
        this(geometry, textures, arrays, materials, part_visibility, new AtomicBoolean(), new RenderControllerRuntime());
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
        StringBuilder sb = new StringBuilder();
        for (MolangValue mv : textures) {
            sb.append(get(scope, mv, "texture", entity.textures()));
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

    public RenderControllerEntry {
        NeoForge.EVENT_BUS.addListener(ManagerEntryChangedEvent.class, e -> {
            if (e.getManagerName().equals("Texture")) {
                needReloadTexture.set(true);
            }
        });
    }

    public ModelComponent setupModel(MolangScope scope, BrClientEntity entity) {
        initArrays(scope, entity);
        ResourceLocation texture;

        if (!textures.isEmpty()) {
            texture = getTexture(scope, entity);

            boolean needReloadTexture = this.needReloadTexture.get();

            if (Minecraft.getInstance().getTextureManager().getTexture(texture) == MissingTextureAtlasSprite.getTexture()
                    || needReloadTexture) {
                List<ResourceLocation> list = new ArrayList<>();
                for (MolangValue mv : textures) {
                    ResourceLocation parse = ResourceLocation.parse(get(scope, mv, "texture", entity.textures()));
                    list.add(parse);
                }
                NativeImages.uploadImage(texture, Textures.layerMerging(list));
            }

            ResourceLocation emissiveTexture = getEmissiveTexture(scope, entity);

            if (Minecraft.getInstance().getTextureManager().getTexture(emissiveTexture) == MissingTextureAtlasSprite.getTexture()
                    || needReloadTexture) {
                List<ResourceLocation> list = new ArrayList<>();
                for (MolangValue mv : textures) {
                    ResourceLocation resourceLocation = ResourceLocation.parse(get(scope, mv, "texture", entity.textures())).withPath(s -> replacePng(s, ".png", ".emissive.png"));
                    list.add(resourceLocation);
                }
                NativeImages.uploadImage(emissiveTexture, Textures.layerMerging(list));
            }

            this.needReloadTexture.set(false);
        } else {
            texture = MissingTextureAtlasSprite.getLocation();
        }

        ModelComponent component = new ModelComponent();

        component.setInfo(new ModelComponent.SerializableInfo(
                get(scope, geometry, "geometry", entity.geometry()),
                texture,
                ResourceLocation.parse(materials.containsKey("*") ? get(scope, materials.get("*"), "material", entity.materials()) : "")
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
