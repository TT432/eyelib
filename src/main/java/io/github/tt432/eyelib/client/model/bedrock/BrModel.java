package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * @param version 1.12.0 or 1.16.0
 * @author TT432
 */
public record BrModel(
        String version,
        String identifier,
        int textureWidth,
        int textureHeight,
        AABB visibleBox,
        Map<String, BrBone> toplevelBones,
        Map<String, BrBone> allBones,
        ModelLocator locator
) implements Model {
    private static final Gson gson = new Gson();

    @Override
    public String name() {
        return identifier;
    }

    @Override
    public ModelRuntimeData<?, ?, ?> data() {
        return new BoneRenderInfos();
    }

    public static BrModel parse(String modelName, JsonObject object) {
        String version;
        String identifier;
        int textureWidth;
        int textureHeight;
        AABB visibleBox;
        Map<String, BrBone> toplevelBones = new HashMap<>();
        Map<String, BrBone> allBones = new HashMap<>();

        if (!(object.get("format_version") instanceof JsonPrimitive versionJson)) {
            throw new JsonParseException("can't parse model %s. not found 'format_version'.".formatted(modelName));
        }

        String versionString = versionJson.getAsString();

        if (!versionString.equals("1.12.0") && !versionString.equals("1.16.0")) {
            throw new JsonParseException("can't load model %s, format version must be 1.12.0 or 1.16.0.".formatted(modelName));
        }

        version = versionString;

        if (!(object.get("minecraft:geometry") instanceof JsonArray geometryArray
                && !geometryArray.isEmpty()
                && geometryArray.get(0) instanceof JsonObject geometry)) {
            throw new JsonParseException("can't parse model %s. not found 'minecraft:geometry'.");
        }

        // description
        JsonObject description = geometry.get("description").getAsJsonObject();
        identifier = description.get("identifier").getAsString();
        textureWidth = description.get("texture_width").getAsInt();
        textureHeight = description.get("texture_height").getAsInt();

        float w = description.get("visible_bounds_width").getAsFloat();
        float h = description.get("visible_bounds_height").getAsFloat();
        float[] offset = gson.fromJson(description.get("visible_bounds_offset"), float[].class);

        //  model space -> world space
        visibleBox = new AABB(
                (offset[0] / 16) - w / 2,
                (offset[0] / 16) + w / 2,
                (offset[1] / 16) - h / 2,
                (offset[1] / 16) + h / 2,
                (offset[2] / 16) - w / 2,
                (offset[2] / 16) + w / 2
        );

        if (!(geometry.get("bones") instanceof JsonArray ja)) {
            throw new JsonParseException("can't parse model %s. not found 'bones'.".formatted(modelName));
        }

        for (JsonElement jsonElement : ja) {
            if (!(jsonElement instanceof JsonObject jo)) {
                throw new JsonParseException("can't parse model %s. element of 'bones' isn't JsonObject.".formatted(modelName));
            }

            BrBone parse = BrBone.parse(textureHeight, textureWidth, jo);
            allBones.put(parse.name(), parse);
        }

        allBones.forEach((name, bone) -> {
            if (bone.parent() == null)
                toplevelBones.put(name, bone);
            else
                allBones.get(bone.parent()).children().put(name, bone);
        });

        Map<String, GroupLocator> locators = new HashMap<>();
        toplevelBones.forEach((k, v) -> locators.put(k, getLocator(v)));

        return new BrModel(version, identifier, textureWidth, textureHeight, visibleBox, toplevelBones, allBones,
                new ModelLocator(locators));
    }

    private static GroupLocator getLocator(BrBone bone) {
        Map<String, GroupLocator> children = new HashMap<>();
        bone.children().forEach((name, group) -> children.put(name, getLocator(group)));
        return new GroupLocator(children, bone.locators().values().stream().map(BrLocator::locatorEntry).toList());
    }
}
