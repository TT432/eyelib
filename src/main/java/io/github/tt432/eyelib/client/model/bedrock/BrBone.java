package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.With;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @param parent         that bone is toplevel if parent is null
 * @param binding        using on 1.16.0 TODO
 * @param reset          bb has this field, but can't resolve mean TODO
 * @param material       bb has this field, but can't resolve mean TODO
 * @param texture_meshes TODO 效果不明  推测：类似 BlockModel
 * @author TT432
 */
@With
public record BrBone(
        String name,
        @Nullable String parent,
        Vector3f pivot,
        Vector3f rotation,
        MolangValue binding,
        boolean reset,
        @Nullable String material,
        Map<String, BrBone> children,
        List<BrCube> cubes,
        List<BrTextureMesh> texture_meshes,
        Map<String, BrLocator> locators
) implements Model.Bone {

    public static final Codec<BrBone> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(BrBone::name),
            Codec.STRING.optionalFieldOf("parent", "").forGetter(BrBone::parent),
            ExtraCodecs.VECTOR3F.fieldOf("pivot").forGetter(BrBone::pivot),
            ExtraCodecs.VECTOR3F.fieldOf("rotation").forGetter(BrBone::rotation),
            MolangValue.CODEC.optionalFieldOf("binding", MolangValue.ZERO).forGetter(BrBone::binding),
            Codec.BOOL.fieldOf("reset").forGetter(BrBone::reset),
            Codec.STRING.optionalFieldOf("material", "").forGetter(BrBone::material),
            BrCube.CODEC.listOf().fieldOf("cubes").forGetter(BrBone::cubes),
            BrTextureMesh.CODEC.listOf().optionalFieldOf("texture_meshes", List.of()).forGetter(BrBone::texture_meshes),
            Codec.unboundedMap(Codec.STRING, BrLocator.CODEC).fieldOf("locators").forGetter(BrBone::locators)
    ).apply(ins, (string, string2, vector3f, vector3f2, string3, aBoolean, string4, brCubes, brTextureMeshes, stringBrLocatorMap) -> new BrBone(string, string2, vector3f, vector3f2, string3, aBoolean, string4, new Object2ObjectOpenHashMap<>(), brCubes, brTextureMeshes, stringBrLocatorMap)));
    private static final Gson gson = new Gson();

    public static BrBone parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        final String name;
        final String parent;
        final Vector3f pivot;
        final Vector3f rotation;
        final MolangValue binding;
        final boolean reset;
        final String material;
        final Map<String, BrBone> children = new Object2ObjectOpenHashMap<>();
        final List<BrCube> cubes = new ObjectArrayList<>();
        final List<BrTextureMesh> texture_meshes = new ObjectArrayList<>();
        final Map<String, BrLocator> locators = new Object2ObjectOpenHashMap<>();

        name = jsonObject.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : "";
        Objects.requireNonNull(name, "cube must have name.");
        parent = jsonObject.get("parent") instanceof JsonPrimitive jp ? jp.getAsString() : "";
        pivot = jsonObject.get("pivot") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        pivot.div(16).mul(-1, 1, 1);
        rotation = jsonObject.get("rotation") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
        binding = jsonObject.get("binding") instanceof JsonPrimitive jp ? MolangValue.CODEC.parse(JsonOps.INSTANCE, jp).getOrThrow(false, IllegalArgumentException::new) : MolangValue.ZERO;
        reset = jsonObject.get("reset") instanceof JsonPrimitive jp && jp.getAsBoolean();
        material = jsonObject.get("material") instanceof JsonPrimitive jp ? jp.getAsString() : "";

        if (jsonObject.get("cubes") instanceof JsonArray ja) {
            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject jo) {
                    cubes.add(BrCube.parse(textureHeight, textureWidth, jo));
                }
            }
        }

        if (jsonObject.get("texture_meshes") instanceof JsonArray ja) {
            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject jo) {
                    texture_meshes.add(BrTextureMesh.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, IllegalArgumentException::new));
                }
            }
        }

        if (jsonObject.get("locators") instanceof JsonObject jo) {
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                locators.put(entry.getKey(), BrLocator.parse(entry.getKey(), entry.getValue()));
            }
        }

        return new BrBone(name, parent, pivot, rotation, binding, reset, material, children, cubes, texture_meshes, locators);
    }
}
