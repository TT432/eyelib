package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.stream.Collectors;

public record BBModel(
        Meta meta,
        String name,
        @SerializedName("model_identifier") String modelIdentifier,
        @SerializedName("visible_box") double[] visibleBox,
        Resolution resolution,
        List<Element> elements,
        List<JsonElement> outliner,
        List<Texture> textures,
        List<Group> groups,

        // Derived
        Int2ObjectMap<BoneImpl> toplevelBones,
        Int2ObjectMap<BoneImpl> allBones,
        ModelLocator modelLocator
) implements Model<BoneImpl> {

    record Outliner(
            String uuid,
            boolean isOpen,
            List<String> cubes,
            List<Outliner> children
    ) {
        record CubeOrOutliner(
                Outliner outliner,
                String uuid
        ) {
        }

        public static final Codec<Outliner> CODEC = Codec.recursive("Outliner", self ->
                RecordCodecBuilder.create(ins -> ins.group(
                        Codec.STRING.fieldOf("uuid").forGetter(Outliner::uuid),
                        Codec.BOOL.fieldOf("isOpen").forGetter(Outliner::isOpen),
                        Codec.withAlternative(
                                        self.xmap(
                                                o -> new CubeOrOutliner(o, null),
                                                c -> c.outliner != null ? c.outliner : null
                                        ),
                                        Codec.STRING.xmap(
                                                o -> new CubeOrOutliner(null, o),
                                                c -> c.uuid != null ? c.uuid : null
                                        )
                                )
                                .listOf().fieldOf("children").forGetter(o -> {
                                    List<CubeOrOutliner> result = new ArrayList<>();
                                    o.children.forEach(oo -> result.add(new CubeOrOutliner(oo, null)));
                                    o.cubes.forEach(oo -> result.add(new CubeOrOutliner(null, oo)));
                                    return result;
                                })
                ).apply(ins, (uuid, isOpen, children) -> new Outliner(
                        uuid,
                        isOpen,
                        children.stream().filter(c -> c.uuid != null).map(CubeOrOutliner::uuid).toList(),
                        children.stream().filter(c -> c.outliner != null).map(CubeOrOutliner::outliner).toList()
                ))));
    }

    public BBModel {
        Int2ObjectMap<BoneImpl> computedTopLevel = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<BoneImpl> computedAllBones = new Int2ObjectOpenHashMap<>();

        var groupMap = groups.stream().map(g -> Map.entry(g.uuid(), g)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        initBones(elements, outliner, this, computedTopLevel, computedAllBones, groupMap, textures);

        toplevelBones = computedTopLevel;
        allBones = computedAllBones;

        Int2ObjectMap<GroupLocator> roots = new Int2ObjectOpenHashMap<>();
        for (BoneImpl bone : computedTopLevel.values()) {
            GroupLocator rootGroup = new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>());
            buildLocator(bone, rootGroup);
            roots.put(bone.id(), rootGroup);
        }
        modelLocator = new ModelLocator(roots);
    }

    @Override
    public ModelRuntimeData<BoneImpl> data() {
        return new BbModelRuntimeData();
    }

    private static void initBones(List<Element> elements, List<JsonElement> outliner, BBModel model,
                                  Int2ObjectMap<BoneImpl> topLevelBonesMap, Int2ObjectMap<BoneImpl> allBonesMap,
                                  Map<String, Group> groupMap, List<Texture> textures) {
        Map<String, Element> elementMap = new HashMap<>();
        if (elements != null) {
            for (Element element : elements) {
                if (element.uuid() != null) {
                    elementMap.put(element.uuid(), element);
                }
            }
        }

        if (outliner == null || outliner.isEmpty()) {
            BoneImpl root = new BoneImpl(GlobalBoneIdHandler.get("root"), -1);
            if (elements != null) {
                for (Element el : elements) {
                    root.addCube(el.createBbCube(textures));
                }
            }
            topLevelBonesMap.put(root.id(), root);
            allBonesMap.put(root.id(), root);
        } else {
            for (JsonElement element : outliner) {
                processOutlinerEntry(element, null, elementMap, model, topLevelBonesMap, allBonesMap, groupMap, textures);
            }
        }
    }

    private static void processOutlinerEntry(JsonElement entry, BoneImpl parent,
                                             Map<String, Element> elementMap, BBModel model,
                                             Int2ObjectMap<BoneImpl> topLevelBonesMap, Int2ObjectMap<BoneImpl> allBonesMap,
                                             Map<String, Group> groupMap, List<Texture> textures) {
        if (entry.isJsonObject()) {
            JsonObject obj = entry.getAsJsonObject();
            String uuid = obj.get("uuid").getAsString();
            BoneImpl bone = new BoneImpl(GlobalBoneIdHandler.get(uuid), parent == null ? -1 : parent.id());
            Group group = groupMap.get(uuid);

            if (group.origin() != null) {
                bone.origin().set(
                        group.origin()[0] / 16,
                        group.origin()[1] / 16,
                        group.origin()[2] / 16
                );
            }

            if (group.rotation() != null) {
                bone.rotation().set(
                        group.rotation()[0] * EyeMath.DEGREES_TO_RADIANS,
                        group.rotation()[1] * EyeMath.DEGREES_TO_RADIANS,
                        group.rotation()[2] * EyeMath.DEGREES_TO_RADIANS
                );
            }

            if (parent != null) {
                parent.addChild(bone);
            } else {
                topLevelBonesMap.put(bone.id(), bone);
            }
            allBonesMap.put(bone.id(), bone);

            if (obj.has("children")) {
                for (JsonElement child : obj.getAsJsonArray("children")) {
                    processOutlinerEntry(child, bone, elementMap, model, topLevelBonesMap, allBonesMap, groupMap, textures);
                }
            }
        } else if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String uuid = entry.getAsString();
            Element el = elementMap.get(uuid);
            if (el != null && parent != null) {
                parent.addCube(el.createBbCube(textures));
            }
        }
    }

    private static void buildLocator(BoneImpl bone, GroupLocator parentLocator) {
        for (BoneImpl child : bone.children().values()) {
            GroupLocator childLocator = new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>());
            parentLocator.children().put(child.id(), childLocator);
            buildLocator(child, childLocator);
        }
    }

    public Int2ObjectMap<BBModel> splitByTexture() {
        Int2ObjectMap<BBModel> result = new Int2ObjectOpenHashMap<>();
        if (elements == null) return result;

        Int2ObjectOpenHashMap<List<Element>> textureToElements = new Int2ObjectOpenHashMap<>();

        for (Element element : elements) {
            Faces faces = element.faces();
            if (faces == null) continue;

            Set<Integer> usedTextures = new HashSet<>();
            checkFace(faces.north(), usedTextures);
            checkFace(faces.east(), usedTextures);
            checkFace(faces.south(), usedTextures);
            checkFace(faces.west(), usedTextures);
            checkFace(faces.up(), usedTextures);
            checkFace(faces.down(), usedTextures);

            for (Integer textureId : usedTextures) {
                textureToElements.computeIfAbsent(textureId.intValue(), k -> new ArrayList<>()).add(element);
            }
        }

        for (Int2ObjectMap.Entry<List<Element>> entry : textureToElements.int2ObjectEntrySet()) {
            int textureId = entry.getIntKey();
            List<Element> originalElements = entry.getValue();

            List<Element> newElements = new ArrayList<>();
            for (Element originalElement : originalElements) {
                Faces newFaces = originalElement.faces(); // Start with the same faces

                if (shouldRemove(newFaces.north(), textureId)) newFaces = newFaces.withNorth(null);
                if (shouldRemove(newFaces.east(), textureId)) newFaces = newFaces.withEast(null);
                if (shouldRemove(newFaces.south(), textureId)) newFaces = newFaces.withSouth(null);
                if (shouldRemove(newFaces.west(), textureId)) newFaces = newFaces.withWest(null);
                if (shouldRemove(newFaces.up(), textureId)) newFaces = newFaces.withUp(null);
                if (shouldRemove(newFaces.down(), textureId)) newFaces = newFaces.withDown(null);

                // Create a new element with the updated faces
                Element newElement = originalElement.withFaces(newFaces);
                newElements.add(newElement);
            }

            BBModel newModel = new BBModel(
                    meta, name, modelIdentifier, visibleBox, resolution,
                    newElements, outliner, textures,
                    groups, null, null, null
            );
            result.put(textureId, newModel);
        }

        return result;
    }

    private void checkFace(FaceData face, Set<Integer> usedTextures) {
        if (face != null && face.texture() != null) {
            usedTextures.add(face.texture());
        }
    }

    private boolean shouldRemove(FaceData face, int targetTextureId) {
        return face == null || face.texture() == null || face.texture() != targetTextureId;
    }

    @Override
    public ModelLocator locator() {
        return modelLocator;
    }

    @Override
    public String name() {
        return name != null ? name : "unknown";
    }

    private static class BbModelRuntimeData implements ModelRuntimeData<BoneImpl> {
        private final Int2ObjectMap<Vector3f> positions = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectMap<Vector3f> rotations = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectMap<Vector3f> scales = new Int2ObjectOpenHashMap<>();

        @Override
        public Vector3fc pivot(BoneImpl model) {
            return model.origin();
        }

        @Override
        public Vector3fc initPosition(BoneImpl model) {
            return new Vector3f();
        }

        @Override
        public Vector3fc position(BoneImpl model) {
            return positions.getOrDefault(model.id(), new Vector3f());
        }

        @Override
        public void position(BoneImpl model, float x, float y, float z) {
            positions.put(model.id(), new Vector3f(x, y, z));
        }

        @Override
        public Vector3fc initRotation(BoneImpl model) {
            return model.rotation();
        }

        @Override
        public Vector3fc rotation(BoneImpl model) {
            return rotations.computeIfAbsent(model.id(), k -> new Vector3f(model.rotation()));
        }

        @Override
        public void rotation(BoneImpl model, float x, float y, float z) {
            rotations.put(model.id(), new Vector3f(x, y, z));
        }

        @Override
        public Vector3fc initScale(BoneImpl model) {
            return new Vector3f(1, 1, 1);
        }

        @Override
        public Vector3fc scale(BoneImpl model) {
            return scales.computeIfAbsent(model.id(), k -> new Vector3f(1, 1, 1));
        }

        @Override
        public void scale(BoneImpl model, float x, float y, float z) {
            scales.put(model.id(), new Vector3f(x, y, z));
        }
    }
}
