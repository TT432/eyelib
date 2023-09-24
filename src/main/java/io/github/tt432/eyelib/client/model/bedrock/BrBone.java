package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.util.EyeMath;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@Getter
public class BrBone {
    private static final Gson gson = new Gson();

    String name;
    /**
     * that bone is toplevel if parent is null
     */
    @Nullable
    String parent;
    Vector3f pivot;
    Vector3f rotation;
    /**
     * using on 1.16.0 TODO
     */
    @Nullable
    String binding;
    /**
     * bb has this field, but can't resolve mean TODO
     */
    boolean reset;
    /**
     * bb has this field, but can't resolve mean TODO
     */
    @Nullable
    String material;

    @Setter
    @NotNull
    Vector3f renderScala = new Vector3f(1);
    @Nullable
    @Setter
    Vector3f renderPivot;
    @Nullable
    @Setter
    Vector3f renderRotation;

    List<BrBone> children = new ArrayList<>();
    List<BrCube> cubes;
    // TODO 效果不明  推测：类似 BlockModel
    List<BrTextureMesh> texture_meshes = new ArrayList<>();
    Map<String, BrLocator> locators;

    public static BrBone parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        BrBone result = new BrBone();

        result.name = jsonObject.get("name") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        Objects.requireNonNull(result.name, "cube must have name.");
        result.parent = jsonObject.get("parent") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.pivot = jsonObject.get("pivot") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        result.pivot.div(16).mul(-1,1,1);
        result.rotation = jsonObject.get("rotation") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        result.rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1,-1,1);
        result.binding = jsonObject.get("binding") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.reset = jsonObject.get("reset") instanceof JsonPrimitive jp && jp.getAsBoolean();
        result.material = jsonObject.get("material") instanceof JsonPrimitive jp ? jp.getAsString() : null;
        result.cubes = jsonObject.get("cubes") instanceof JsonArray ja
                ? ja.asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .map(jo -> BrCube.parse(textureHeight, textureWidth, jo))
                    .toList()
                : new ArrayList<>();
        // TODO parse texture_meshes
        result.locators = jsonObject.get("locators") instanceof JsonObject jo
                ? jo.asMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> BrLocator.parse(entry.getKey(),entry.getValue())))
                : new HashMap<>();

        return result;
    }
}
