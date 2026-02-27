package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.util.EntryStreams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * @author TT432
 */
@With
public record BrModelEntry(
        String name,
        int textureWidth,
        int textureHeight,
        AABB visibleBox,
        Int2ObjectMap<BrBone> toplevelBones,
        Int2ObjectMap<BrBone> allBones
) {
    private static final Gson gson = new Gson();

    public static BrModelEntry parse2(String identifier, JsonObject geometry) {
        int textureWidth;
        int textureHeight;
        AABB visibleBox;
        Int2ObjectMap<BrBone> toplevelBones = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<BrBone> allBones = new Int2ObjectOpenHashMap<>();

        textureWidth = geometry.get("texturewidth") instanceof JsonPrimitive jp ? jp.getAsInt() : 256;
        textureHeight = geometry.get("textureheight") instanceof JsonPrimitive jp ? jp.getAsInt() : 256;

        float w = geometry.get("visible_bounds_width") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        float h = geometry.get("visible_bounds_height") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        float[] offset = geometry.get("visible_bounds_offset") instanceof JsonArray ja
                ? gson.fromJson(ja, float[].class)
                : new float[]{0, 0, 0};

        visibleBox = new AABB(
                (offset[0] / 16) - w / 2,
                (offset[0] / 16) + w / 2,
                (offset[1] / 16) - h / 2,
                (offset[1] / 16) + h / 2,
                (offset[2] / 16) - w / 2,
                (offset[2] / 16) + w / 2
        );

        Int2ObjectMap<GroupLocator> locators = new Int2ObjectOpenHashMap<>();

        if (geometry.has("bones")) {
            for (JsonElement jsonElement : geometry.get("bones").getAsJsonArray()) {
                BrBone parse = BrBone.parse(textureHeight, textureWidth, jsonElement.getAsJsonObject());
                if (parse.parent() != -1) parse = parse.withParent(parse.parent());
                allBones.put(parse.id(), parse);
            }

            allBones.int2ObjectEntrySet().forEach((entry) -> {
                var name = entry.getIntKey();
                var bone = entry.getValue();
                if (bone.parent() == -1 || allBones.get(bone.parent()) == null)
                    toplevelBones.put(name, bone);
                else
                    allBones.get(bone.parent()).children().put(name, bone);
            });
        }

        return new BrModelEntry(identifier, textureWidth, textureHeight, visibleBox, toplevelBones, allBones);
    }

    public static BrModelEntry parse(JsonObject geometry) {
        String identifier;
        int textureWidth;
        int textureHeight;
        AABB visibleBox;
        Int2ObjectMap<BrBone> toplevelBones = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<BrBone> allBones = new Int2ObjectOpenHashMap<>();

        // description
        JsonObject description = geometry.get("description").getAsJsonObject();
        identifier = description.get("identifier").getAsString();

        textureWidth = description.get("texture_width") instanceof JsonPrimitive jp ? jp.getAsInt() : 256;
        textureHeight = description.get("texture_height") instanceof JsonPrimitive jp ? jp.getAsInt() : 256;

        float w = description.get("visible_bounds_width") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        float h = description.get("visible_bounds_height") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        float[] offset = description.get("visible_bounds_offset") instanceof JsonArray ja
                ? gson.fromJson(ja, float[].class)
                : new float[]{0, 0, 0};

        //  model space -> world space
        visibleBox = new AABB(
                (offset[0] / 16) - w / 2,
                (offset[0] / 16) + w / 2,
                (offset[1] / 16) - h / 2,
                (offset[1] / 16) + h / 2,
                (offset[2] / 16) - w / 2,
                (offset[2] / 16) + w / 2
        );

        if (geometry.has("bones")) {
            for (JsonElement jsonElement : geometry.get("bones").getAsJsonArray()) {
                BrBone parse = BrBone.parse(textureHeight, textureWidth, jsonElement.getAsJsonObject());
                parse = parse.withId(parse.id());
                if (parse.parent() != -1) parse = parse.withParent(parse.parent());
                allBones.put(parse.id(), parse);
            }

            allBones.int2ObjectEntrySet().forEach((entry) -> {
                var name = entry.getIntKey();
                var bone = entry.getValue();
                if (bone.parent() == -1 || allBones.get(bone.parent()) == null)
                    toplevelBones.put(name, bone);
                else
                    allBones.get(bone.parent()).children().put(name, bone);
            });
        }

        return new BrModelEntry(identifier, textureWidth, textureHeight, visibleBox,
                toplevelBones, allBones);
    }

    public Model createModel() {
        return new Model(name, allBones.int2ObjectEntrySet().stream().map(e -> Map.entry(e.getIntKey(), e.getValue().createBone())).collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)));
    }
}
