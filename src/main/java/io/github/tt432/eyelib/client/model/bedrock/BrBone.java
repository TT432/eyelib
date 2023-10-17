package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.util.EyeMath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

/**
 * @author TT432
 */
@Getter
@RequiredArgsConstructor
public class BrBone {
    private static final Gson gson = new Gson();

    final String name;
    /**
     * that bone is toplevel if parent is null
     */
    @Nullable
    final String parent;
    final Vector3f pivot;
    final Vector3f rotation;
    /**
     * using on 1.16.0 TODO
     */
    @Nullable
    final String binding;
    /**
     * bb has this field, but can't resolve mean TODO
     */
    final boolean reset;
    /**
     * bb has this field, but can't resolve mean TODO
     */
    @Nullable
    final String material;

    final List<BrBone> children;
    final List<BrCube> cubes;
    // TODO 效果不明  推测：类似 BlockModel
    final List<BrTextureMesh> texture_meshes;
    final Map<String, BrLocator> locators;

    @Setter
    @NotNull
    Vector3f renderScala = new Vector3f(1);
    @Nullable
    @Setter
    Vector3f renderPivot;
    @Nullable
    @Setter
    Vector3f renderRotation;

    public BrBone copy() {
        List<BrBone> copiedChildren = new ArrayList<>();

        for (BrBone child : children) {
            copiedChildren.add(child.copy());
        }

        return new BrBone(name, parent, pivot, rotation, binding, reset, material, copiedChildren, cubes, texture_meshes, locators);
    }

    public static BrBone parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        final String name;
        final String parent;
        final Vector3f pivot;
        final Vector3f rotation;
        final String binding;
        final boolean reset;
        final String material;
        final List<BrBone> children = new ArrayList<>();
        final List<BrCube> cubes = new ArrayList<>();
        final List<BrTextureMesh> texture_meshes = new ArrayList<>();
        final Map<String, BrLocator> locators = new HashMap<>();

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

        if(jsonObject.get("locators") instanceof JsonObject jo) {
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                locators.put(entry.getKey(), BrLocator.parse(entry.getKey(), entry.getValue()));
            }
        }

        return new BrBone(name, parent, pivot, rotation, binding, reset, material, children, cubes, texture_meshes, locators);
    }
}
