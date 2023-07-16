package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@Slf4j
@Getter
public class BrModel {
    private static final Gson gson = new Gson();

    /** 1.12.0 or 1.16.0 */
    String version;

    String identifier;
    int textureWidth;
    int textureHeight;

    AABB visibleBox;

    List<BrBone> toplevelBones = new ArrayList<>();
    Map<String, BrBone> allBones = new HashMap<>();

    public static BrModel parse(String modelName, JsonObject object) {
        BrModel result = new BrModel();

        if (object.get("format_version") instanceof JsonPrimitive versionJson) {
            String versionString = versionJson.getAsString();

            if (!versionString.equals("1.12.0") && !versionString.equals("1.16.0")) {
                log.error("can't load model {}, format version must be 1.12.0 or 1.16.0.", modelName);
                return null;
            }

            result.version = versionString;
        }

        if (object.get("minecraft:geometry") instanceof JsonArray geometryArray
                && !geometryArray.isEmpty()
                && geometryArray.get(0) instanceof JsonObject geometry) {
            // description
            JsonObject description = geometry.get("description").getAsJsonObject();
            result.identifier = description.get("identifier").getAsString();
            result.textureWidth = description.get("texture_width").getAsInt();
            result.textureHeight = description.get("texture_height").getAsInt();

            float w = description.get("visible_bounds_width").getAsFloat();
            float h = description.get("visible_bounds_height").getAsFloat();
            float[] offset = gson.fromJson(description.get("visible_bounds_offset"), float[].class);

            //  model space -> world space
            result.visibleBox = new AABB(
                    (offset[0] / 16) - w / 2,
                    (offset[0] / 16) + w / 2,
                    (offset[1] / 16) - h / 2,
                    (offset[1] / 16) + h / 2,
                    (offset[2] / 16) - w / 2,
                    (offset[2] / 16) + w / 2
            );

            // bones
            result.allBones = geometry.get("bones") instanceof JsonArray ja
                    ? ja.asList().stream()
                        .filter(JsonElement::isJsonObject)
                        .map(JsonElement::getAsJsonObject)
                        .map(jo -> BrBone.parse(result.textureHeight, result.textureWidth, jo))
                        .collect(Collectors.toMap(bone -> bone.name, bone -> bone))
                    : new HashMap<>();

            result.allBones.forEach((name, bone) -> {
                if (bone.parent == null)
                    result.toplevelBones.add(bone);
                else
                    result.allBones.get(bone.parent).children.add(bone);
            });

            return result;
        } else {
            log.error("need last one minecraft:geometry for model {}.", modelName);
            return null;
        }
    }
}
