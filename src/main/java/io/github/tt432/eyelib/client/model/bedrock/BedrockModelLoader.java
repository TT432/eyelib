package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.client.model.importer.ModelImportException;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockModelLoader {
    private static final Gson GSON = new Gson();

    public BedrockGeometryModel load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            return load(root);
        } catch (RuntimeException e) {
            throw new ModelImportException("Failed to parse Bedrock model: " + path, e);
        }
    }

    public BedrockGeometryModel load(JsonObject root) throws IOException {
        JsonArray geometriesJson = root.getAsJsonArray("minecraft:geometry");
        if (geometriesJson == null) {
            throw new ModelImportException("Bedrock model is missing minecraft:geometry");
        }

        List<BedrockGeometryModel.Geometry> geometries = new java.util.ArrayList<>();
        for (JsonElement geometry : geometriesJson) {
            geometries.add(parseGeometry(geometry.getAsJsonObject()));
        }
        return new BedrockGeometryModel(geometries);
    }

    private BedrockGeometryModel.Geometry parseGeometry(JsonObject json) {
        JsonObject description = json.getAsJsonObject("description");
        BedrockGeometryModel.Description parsedDescription = new BedrockGeometryModel.Description(
                requiredString(description, "identifier"),
                getInt(description, "texture_width", 16),
                getInt(description, "texture_height", 16),
                getFloat(description, "visible_bounds_width", 0),
                getFloat(description, "visible_bounds_height", 0),
                getVector3(description, "visible_bounds_offset", new Vector3f())
        );

        JsonArray bonesJson = json.getAsJsonArray("bones");
        List<BedrockGeometryModel.Bone> bones = new java.util.ArrayList<>();
        if (bonesJson != null) {
            for (JsonElement bone : bonesJson) {
                bones.add(parseBone(bone.getAsJsonObject()));
            }
        }
        return new BedrockGeometryModel.Geometry(parsedDescription, bones);
    }

    private BedrockGeometryModel.Bone parseBone(JsonObject json) {
        JsonArray cubesJson = json.getAsJsonArray("cubes");
        List<BedrockGeometryModel.Cube> cubes = new java.util.ArrayList<>();
        if (cubesJson != null) {
            for (JsonElement cube : cubesJson) {
                cubes.add(parseCube(cube.getAsJsonObject()));
            }
        }
        return new BedrockGeometryModel.Bone(
                requiredString(json, "name"),
                optionalString(json, "parent"),
                getVector3(json, "pivot", new Vector3f()),
                getVector3(json, "rotation", new Vector3f()),
                cubes
        );
    }

    private BedrockGeometryModel.Cube parseCube(JsonObject json) {
        JsonElement uv = json.get("uv");
        Vector2f boxUv = uv != null && uv.isJsonArray() ? getVector2(json, "uv", new Vector2f()) : null;
        Map<String, BedrockGeometryModel.FaceUv> faceUvs = new LinkedHashMap<>();
        if (uv != null && uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                faceUvs.put(entry.getKey(), new BedrockGeometryModel.FaceUv(
                        getVector2(face, "uv", new Vector2f()),
                        getVector2(face, "uv_size", new Vector2f()),
                        getInt(face, "uv_rotation", 0)
                ));
            }
        }

        return new BedrockGeometryModel.Cube(
                getVector3(json, "origin", new Vector3f()),
                getVector3(json, "size", new Vector3f()),
                json.has("pivot") ? getVector3(json, "pivot", new Vector3f()) : null,
                getVector3(json, "rotation", new Vector3f()),
                boxUv,
                faceUvs
        );
    }

    private static String requiredString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required Bedrock field: " + key);
        }
        return value.getAsString();
    }

    @Nullable
    private static String optionalString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null ? null : value.getAsString();
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsInt();
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        JsonElement value = json.get(key);
        return value == null ? fallback : value.getAsFloat();
    }

    private static Vector2f getVector2(JsonObject json, String key, Vector2f fallback) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            return new Vector2f(fallback);
        }

        JsonArray array = value.getAsJsonArray();
        return new Vector2f(
                array.size() > 0 ? array.get(0).getAsFloat() : fallback.x,
                array.size() > 1 ? array.get(1).getAsFloat() : fallback.y
        );
    }

    private static Vector3f getVector3(JsonObject json, String key, Vector3f fallback) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            return new Vector3f(fallback);
        }

        JsonArray array = value.getAsJsonArray();
        return new Vector3f(
                array.size() > 0 ? array.get(0).getAsFloat() : fallback.x,
                array.size() > 1 ? array.get(1).getAsFloat() : fallback.y,
                array.size() > 2 ? array.get(2).getAsFloat() : fallback.z
        );
    }
}
