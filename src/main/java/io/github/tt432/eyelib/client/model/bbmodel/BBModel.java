package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.util.client.Textures;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@With
public record BBModel(
        Meta meta,
        String name,
        @SerializedName("model_identifier")
        String modelIdentifier,
        @SerializedName("visible_box")
        List<Double> visibleBox,
        Resolution resolution,
        List<Element> elements,
        List<Outliner> outliner,
        List<Texture> textures,
        List<Group> groups,

        // Derived
        Int2ObjectMap<BoneImpl> toplevelBones,
        Int2ObjectMap<BoneImpl> allBones,
        ModelLocator modelLocator
) implements Model<BoneImpl> {
    public static final Codec<BBModel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Meta.CODEC.fieldOf("meta").forGetter(BBModel::meta),
            Codec.STRING.fieldOf("name").forGetter(BBModel::name),
            Codec.STRING.fieldOf("model_identifier").forGetter(BBModel::modelIdentifier),
            Codec.DOUBLE.listOf().fieldOf("visible_box").forGetter(BBModel::visibleBox),
            Resolution.CODEC.fieldOf("resolution").forGetter(BBModel::resolution),
            Element.CODEC.listOf().fieldOf("elements").forGetter(BBModel::elements),
            Outliner.CODEC.listOf().fieldOf("outliner").forGetter(BBModel::outliner),
            Texture.CODEC.listOf().fieldOf("textures").forGetter(BBModel::textures),
            Group.CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(BBModel::groups)
    ).apply(ins, BBModel::new));

    public BBModel(Meta meta, String name, String modelIdentifier, List<Double> visibleBox, Resolution resolution, List<Element> elements, List<Outliner> outliner, List<Texture> textures, List<Group> groups) {
        this(meta, name, modelIdentifier, visibleBox, resolution, elements, outliner, textures, groups, new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>(), new ModelLocator(new Int2ObjectOpenHashMap<>()));
    }

    public BBModel {
        Int2ObjectMap<BoneImpl> computedTopLevel = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<BoneImpl> computedAllBones = new Int2ObjectOpenHashMap<>();

        var groupMap = groups.stream().map(g -> Map.entry(g.uuid(), g)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        initBones(elements, outliner, computedTopLevel, computedAllBones, groupMap, textures);

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

    private static void initBones(List<Element> elements, List<Outliner> outliner,
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
            for (var element : outliner) {
                processOutlinerEntry(element, null, elementMap, topLevelBonesMap, allBonesMap, groupMap, textures);
            }
        }
    }

    private static void processOutlinerEntry(Outliner entry, BoneImpl parent, Map<String, Element> elementMap,
                                             Int2ObjectMap<BoneImpl> topLevelBonesMap, Int2ObjectMap<BoneImpl> allBonesMap,
                                             Map<String, Group> groupMap, List<Texture> textures) {
        String uuid = entry.uuid();
        Group group = groupMap.get(uuid);
        if (group == null && entry.group().isPresent()) {
            group = entry.group().get();
        }
        if (group == null) return;
        BoneImpl bone = new BoneImpl(GlobalBoneIdHandler.get(group.name()), parent == null ? -1 : parent.id());

        if (group.origin() != null) {
            bone.origin().set(group.origin()).div(16);
        }

        if (group.rotation() != null) {
            bone.rotation().set(group.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
        }

        if (parent != null) {
            parent.addChild(bone);
        } else {
            topLevelBonesMap.put(bone.id(), bone);
        }
        allBonesMap.put(bone.id(), bone);

        for (Outliner child : entry.children()) {
            processOutlinerEntry(child, bone, elementMap, topLevelBonesMap, allBonesMap, groupMap, textures);
        }

        for (String cube : entry.cubes()) {
            Element el = elementMap.get(cube);
            if (el != null && parent != null) {
                bone.addCube(el.createBbCube(textures));
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

    public Textures.RepackedImage<BoneImpl, BBModel> repackImage() throws IOException {
        if (textures == null || textures.isEmpty() || elements == null) {
            throw new IOException("Missing textures/elements");
        }

        List<NativeImage> images = new ArrayList<>(textures.size());

        for (Texture tex : textures) {
            int tw = tex.uvWidth() > 0 ? tex.uvWidth() : (tex.width() > 0 ? tex.width() : 1);
            int th = tex.uvHeight() > 0 ? tex.uvHeight() : (tex.height() > 0 ? tex.height() : 1);

            NativeImage img = null;
            try {
                img = tex.getNativeImage();
            } catch (Exception ignored) {
            }

            if (img != null) {
                tw = Math.max(tw, img.getWidth());
                th = Math.max(th, img.getHeight());
            }

            NativeImage resized = new NativeImage(tw, th, true);
            if (img != null) {
                int copyW = Math.min(tw, img.getWidth());
                int copyH = Math.min(th, img.getHeight());
                for (int yy = 0; yy < copyH; yy++) {
                    for (int xx = 0; xx < copyW; xx++) {
                        resized.setPixelRGBA(xx, yy, img.getPixelRGBA(xx, yy));
                    }
                }
                img.close();
            }

            images.add(resized);
        }

        class EmptyModel implements Model<BoneImpl> {
            private final Int2ObjectMap<BoneImpl> empty = new Int2ObjectOpenHashMap<>();

            @Override
            public String name() {
                return "empty";
            }

            @Override
            public Int2ObjectMap<BoneImpl> toplevelBones() {
                return empty;
            }

            @Override
            public Int2ObjectMap<BoneImpl> allBones() {
                return empty;
            }

            @Override
            public ModelRuntimeData<BoneImpl> data() {
                return new BbModelRuntimeData();
            }

            @Override
            public ModelLocator locator() {
                return new ModelLocator(new Int2ObjectOpenHashMap<>());
            }
        }

        List<EmptyModel> emptyModels = new ArrayList<>(textures.size());
        for (int i = 0; i < textures.size(); i++) {
            emptyModels.add(new EmptyModel());
        }

        Textures.RepackedImage<BoneImpl, EmptyModel> repacked = Textures.repackedImage(
                emptyModels,
                images,
                (bone, cubes) -> bone,
                (bone, id) -> bone,
                (bone, parent) -> bone,
                (model, bones) -> model
        );

        for (NativeImage img : images) {
            if (img != null) {
                img.close();
            }
        }

        Int2ObjectMap<Textures.AtlasRegion> regions = repacked.regions();

        List<Element> newElements = new ArrayList<>(elements.size());
        for (Element el : elements) {
            Faces faces = el.faces();
            if (faces == null) {
                newElements.add(el);
                continue;
            }
            Faces newFaces = new Faces(
                    remapFace(faces.north(), regions),
                    remapFace(faces.east(), regions),
                    remapFace(faces.south(), regions),
                    remapFace(faces.west(), regions),
                    remapFace(faces.up(), regions),
                    remapFace(faces.down(), regions)
            );
            newElements.add(el.withFaces(newFaces));
        }

        return new Textures.RepackedImage<>(
                this.withElements(newElements)
                        .withTextures(List.of(repacked.atlasTexture()))
                        .withToplevelBones(null)
                        .withAllBones(null)
                        .withModelLocator(null),
                repacked.atlasTexture(),
                repacked.atlasImage(),
                repacked.regions()
        );
    }

    private static FaceData remapFace(FaceData face, Int2ObjectMap<Textures.AtlasRegion> regions) {
        if (face == null) return null;
        if (face.uv() == null) {
            if (face.texture() == -1) return face;
            return new FaceData(null, 0, face.cullFace(), face.rotation(), face.tint());
        }

        if (face.texture() != -1) {
            Textures.AtlasRegion region = regions.get(face.texture());
            int ox = region != null ? region.x() : 0;
            int oy = region != null ? region.y() : 0;

            return new FaceData(new Vector4f(
                    face.uv().x + ox,
                    face.uv().y + oy,
                    face.uv().z + ox,
                    face.uv().w + oy
            ), 0, face.cullFace(), face.rotation(), face.tint());
        }

        return face;
    }

    private void checkFace(FaceData face, Set<Integer> usedTextures) {
        if (face != null && face.texture() != -1) {
            usedTextures.add(face.texture());
        }
    }

    private boolean shouldRemove(FaceData face, int targetTextureId) {
        return face == null || face.texture() == -1 || face.texture() != targetTextureId;
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
