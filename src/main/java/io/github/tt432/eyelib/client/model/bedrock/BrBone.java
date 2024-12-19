package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.With;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

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
        @Nullable String binding,
        boolean reset,
        @Nullable String material,
        Map<String, BrBone> children,
        List<BrCube> cubes,
        List<BrTextureMesh> texture_meshes,
        Map<String, BrLocator> locators
) implements Model.Bone {

    private static final Gson gson = new Gson();

    public static BrBone parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        final String name;
        final String parent;
        final Vector3f pivot;
        final Vector3f rotation;
        final String binding;
        final boolean reset;
        final String material;
        final Map<String, BrBone> children = new Object2ObjectOpenHashMap<>();
        final List<BrCube> cubes = new ObjectArrayList<>();
        final List<BrTextureMesh> texture_meshes = new ObjectArrayList<>();
        final Map<String, BrLocator> locators = new Object2ObjectOpenHashMap<>();

        name = jsonObject.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        Objects.requireNonNull(name, "cube must have name.");
        parent = jsonObject.get("parent") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        pivot = jsonObject.get("pivot") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        pivot.div(16).mul(-1, 1, 1);
        rotation = jsonObject.get("rotation") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
        binding = jsonObject.get("binding") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        reset = jsonObject.get("reset") instanceof JsonPrimitive jp && jp.getAsBoolean();
        material = jsonObject.get("material") instanceof JsonPrimitive jp ? jp.getAsString() : null;

        if (jsonObject.get("cubes") instanceof JsonArray ja) {
            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject jo) {
                    cubes.add(BrCube.parse(textureHeight, textureWidth, jo));
                }
            }
        }

        // TODO parse texture_meshes

        if (jsonObject.get("locators") instanceof JsonObject jo) {
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                locators.put(entry.getKey(), BrLocator.parse(entry.getKey(), entry.getValue()));
            }
        }

        return new BrBone(name, parent, pivot, rotation, binding, reset, material, children, cubes, texture_meshes, locators);
    }
}
