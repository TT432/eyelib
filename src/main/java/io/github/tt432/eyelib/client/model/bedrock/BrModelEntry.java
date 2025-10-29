package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.util.codec.Tuple;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record BrModelEntry(
        String name,
        int textureWidth,
        int textureHeight,
        AABB visibleBox,
        Int2ObjectMap<BrBone> toplevelBones,
        Int2ObjectMap<BrBone> allBones,
        ModelLocator locator
) implements Model {
    public static final Codec<BrModelEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(BrModelEntry::name),
            Codec.INT.fieldOf("textureWidth").forGetter(BrModelEntry::textureWidth),
            Codec.INT.fieldOf("textureHeight").forGetter(BrModelEntry::textureHeight),
            ChinExtraCodecs.tuple(Vec3.CODEC, Vec3.CODEC).bmap(AABB::new, aabb -> Tuple.of(new Vec3(aabb.minX, aabb.minY, aabb.minZ), new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ))).fieldOf("visibleBox").forGetter(BrModelEntry::visibleBox),
            GlobalBoneIdHandler.map(BrBone.CODEC).fieldOf("allBones").forGetter(BrModelEntry::allBones)
    ).apply(ins, (string, integer, integer2, aabb, allBones) -> {
        Int2ObjectMap<BrBone> toplevelBones = new Int2ObjectOpenHashMap<>();

        allBones.int2ObjectEntrySet().forEach((entry) -> {
            var name = entry.getIntKey();
            var bone = entry.getValue();
            if (bone.parent() == -1 || allBones.get(bone.parent()) == null)
                toplevelBones.put(name, bone);
            else
                allBones.get(bone.parent()).children().put(name, bone);
        });

        Int2ObjectMap<GroupLocator> locators = new Int2ObjectOpenHashMap<>();
        toplevelBones.int2ObjectEntrySet().forEach(entry -> locators.put(entry.getIntKey(), getLocator(entry.getValue())));

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

        Int2ObjectMap<GroupLocator> locators = new Int2ObjectOpenHashMap<>();

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

            toplevelBones.forEach((k, v) -> locators.put(k, getLocator(v)));
        }

        return new BrModelEntry(identifier, textureWidth, textureHeight, visibleBox,
                toplevelBones, allBones, new ModelLocator(locators));
    }

    private static GroupLocator getLocator(BrBone bone) {
        Int2ObjectMap<GroupLocator> children = new Int2ObjectOpenHashMap<>();
        bone.children().int2ObjectEntrySet().forEach((entry) -> {
            var name = entry.getIntKey();
            var group = entry.getValue();
            children.put(name, getLocator(group));
        });
        List<LocatorEntry> list = new ArrayList<>();
        for (BrLocator brLocator : bone.locators().values()) {
            list.add(brLocator.locatorEntry());
        }
        return new GroupLocator(children, list);
    }
}
