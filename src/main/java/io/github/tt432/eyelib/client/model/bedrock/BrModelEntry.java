package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author TT432
 */
public record BrModelEntry(
        String name,
        int textureWidth,
        int textureHeight,
        AABB visibleBox,
        Map<String, BrBone> toplevelBones,
        Map<String, BrBone> allBones,
        ModelLocator locator
) implements Model {
    public static final Codec<BrModelEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(BrModelEntry::name),
            Codec.INT.fieldOf("textureWidth").forGetter(BrModelEntry::textureWidth),
            Codec.INT.fieldOf("textureHeight").forGetter(BrModelEntry::textureHeight),
            ChinExtraCodecs.tuple(Vec3.CODEC, Vec3.CODEC).bmap(AABB::new, aabb -> Tuple.of(aabb.getMinPosition(), aabb.getMaxPosition())).fieldOf("visibleBox").forGetter(BrModelEntry::visibleBox),
            Codec.unboundedMap(Codec.STRING, BrBone.CODEC).fieldOf("allBones").forGetter(BrModelEntry::allBones)
    ).apply(ins, (string, integer, integer2, aabb, allBones) -> {
        Object2ObjectOpenHashMap<String, BrBone> toplevelBones = new Object2ObjectOpenHashMap<>();

        allBones.forEach((name, bone) -> {
            if (bone.parent() == null || allBones.get(bone.parent()) == null)
                toplevelBones.put(name, bone);
            else
                allBones.get(bone.parent()).children().put(name, bone);
        });

        Map<String, GroupLocator> locators = new HashMap<>();
        toplevelBones.forEach((k, v) -> locators.put(k, getLocator(v)));

        return new BrModelEntry(string, integer, integer2, aabb, toplevelBones, allBones, new ModelLocator(locators));
    }));
    private static final Gson gson = new Gson();

    @Override
    public ModelRuntimeData<?, ?, ?> data() {
        return new BoneRenderInfos();
    }

    public static BrModelEntry parse2(String identifier, JsonObject geometry) {
        int textureWidth;
        int textureHeight;
        AABB visibleBox;
        Map<String, BrBone> toplevelBones = new HashMap<>();
        Map<String, BrBone> allBones = new HashMap<>();

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

        Map<String, GroupLocator> locators = new HashMap<>();

        if (geometry.has("bones")) {
            for (JsonElement jsonElement : geometry.get("bones").getAsJsonArray()) {
                BrBone parse = BrBone.parse(textureHeight, textureWidth, jsonElement.getAsJsonObject());
                if (parse.parent() != null) parse = parse.withParent(parse.parent().toLowerCase(Locale.ROOT));
                allBones.put(parse.name().toLowerCase(Locale.ROOT), parse);
            }

            allBones.forEach((name, bone) -> {
                if (bone.parent() == null || allBones.get(bone.parent()) == null)
                    toplevelBones.put(name, bone);
                else
                    allBones.get(bone.parent()).children().put(name, bone);
            });

            toplevelBones.forEach((k, v) -> locators.put(k, getLocator(v)));
        }

        return new BrModelEntry(identifier, textureWidth, textureHeight, visibleBox,
                toplevelBones, allBones, new ModelLocator(locators));
    }

    public static BrModelEntry parse(JsonObject geometry) {
        String identifier;
        int textureWidth;
        int textureHeight;
        AABB visibleBox;
        Map<String, BrBone> toplevelBones = new HashMap<>();
        Map<String, BrBone> allBones = new HashMap<>();

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

        Map<String, GroupLocator> locators = new HashMap<>();

        if (geometry.has("bones")) {
            for (JsonElement jsonElement : geometry.get("bones").getAsJsonArray()) {
                BrBone parse = BrBone.parse(textureHeight, textureWidth, jsonElement.getAsJsonObject());
                parse = parse.withName(parse.name().toLowerCase(Locale.ROOT));
                if (parse.parent() != null) parse = parse.withParent(parse.parent().toLowerCase(Locale.ROOT));
                allBones.put(parse.name(), parse);
            }

            allBones.forEach((name, bone) -> {
                if (bone.parent() == null || allBones.get(bone.parent()) == null)
                    toplevelBones.put(name, bone);
                else
                    allBones.get(bone.parent()).children().put(name, bone);
            });

            toplevelBones.forEach((k, v) -> locators.put(k, getLocator(v)));
        }

        return new BrModelEntry(identifier, textureWidth, textureHeight, visibleBox,
                toplevelBones, allBones, new ModelLocator(locators));
    }

    private static GroupLocator getLocator(BrBone bone) {
        Map<String, GroupLocator> children = new HashMap<>();
        bone.children().forEach((name, group) -> children.put(name, getLocator(group)));
        return new GroupLocator(children, bone.locators().values().stream().map(BrLocator::locatorEntry).toList());
    }
}
