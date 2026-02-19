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
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public record RepackedImage(
            BBModel model,
            Texture atlasTexture,
            NativeImage atlasImage
    ) {
    }

    private record AtlasRegion(int x, int y, int w, int h) {
    }

    private record AtlasPacking(int width, int height, Int2ObjectMap<AtlasRegion> regions) {
    }

    public RepackedImage repackImage() throws IOException {
        if (textures == null || textures.isEmpty() || elements == null) {
            throw new IOException("Missing textures/elements");
        }

        int padding = 1;

        List<NativeImage> images = new ArrayList<>(textures.size());
        int[] w = new int[textures.size()];
        int[] h = new int[textures.size()];

        for (int i = 0; i < textures.size(); i++) {
            Texture tex = textures.get(i);
            NativeImage img = null;
            try {
                img = tex.getNativeImage();
            } catch (Exception ignored) {
            }
            images.add(img);

            int tw = tex.uvWidth() > 0 ? tex.uvWidth() : (tex.width() > 0 ? tex.width() : 1);
            int th = tex.uvHeight() > 0 ? tex.uvHeight() : (tex.height() > 0 ? tex.height() : 1);

            if (img != null) {
                tw = Math.max(tw, img.getWidth());
                th = Math.max(th, img.getHeight());
            }
            w[i] = tw;
            h[i] = th;
        }

        AtlasPacking packing = packAtlas(w, h, padding);

        NativeImage atlas = new NativeImage(packing.width(), packing.height(), true);
        for (int i = 0; i < textures.size(); i++) {
            AtlasRegion region = packing.regions().get(i);
            if (region == null) continue;
            NativeImage src = images.get(i);
            if (src == null) continue;

            int copyW = Math.min(region.w(), src.getWidth());
            int copyH = Math.min(region.h(), src.getHeight());
            for (int yy = 0; yy < copyH; yy++) {
                for (int xx = 0; xx < copyW; xx++) {
                    atlas.setPixelRGBA(region.x() + xx, region.y() + yy, src.getPixelRGBA(xx, yy));
                }
            }
        }

        for (NativeImage img : images) {
            if (img != null) {
                img.close();
            }
        }

        Texture atlasTexture = new Texture(
                "atlas",
                null,
                null,
                null,
                "atlas",
                null,
                packing.width(),
                packing.height(),
                packing.width(),
                packing.height(),
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                0,
                null,
                null,
                false,
                true,
                true,
                false,
                UUID.randomUUID().toString(),
                null,
                null
        );

        List<Element> newElements = new ArrayList<>(elements.size());
        for (Element el : elements) {
            Faces faces = el.faces();
            if (faces == null) {
                newElements.add(el);
                continue;
            }
            Faces newFaces = new Faces(
                    remapFace(faces.north(), packing, padding),
                    remapFace(faces.east(), packing, padding),
                    remapFace(faces.south(), packing, padding),
                    remapFace(faces.west(), packing, padding),
                    remapFace(faces.up(), packing, padding),
                    remapFace(faces.down(), packing, padding)
            );
            newElements.add(el.withFaces(newFaces));
        }

        BBModel repackedModel = new BBModel(
                meta, name, modelIdentifier, visibleBox, resolution,
                newElements, outliner, List.of(atlasTexture),
                groups, null, null, null
        );

        return new RepackedImage(repackedModel, atlasTexture, atlas);
    }

    private static FaceData remapFace(FaceData face, AtlasPacking packing, int padding) {
        if (face == null) return null;
        if (face.uv() == null) {
            if (face.texture() == -1) return face;
            return new FaceData(null, 0, face.cullFace(), face.rotation(), face.tint());
        }

        if (face.texture() != -1) {
            AtlasRegion region = packing.regions().get(face.texture());
            int ox = region != null ? region.x() : 0;
            int oy = region != null ? region.y() : 0;

            return new FaceData(new Vector4f(
                    face.uv().x + ox,
                    face.uv().y + oy,
                    face.uv().z + ox,
                    face.uv().w + oy
            ), 0, face.cullFace(), face.rotation(), face.tint());
        } else {
            return face;
        }
    }

    private static AtlasPacking packAtlas(int[] w, int[] h, int padding) throws IOException {
        long area = 0;
        int maxW = 1;
        int maxH = 1;
        int n = w.length;

        int[] rw = new int[n];
        int[] rh = new int[n];

        for (int i = 0; i < n; i++) {
            int ww = Math.max(1, w[i]) + padding * 2;
            int hh = Math.max(1, h[i]) + padding * 2;
            rw[i] = ww;
            rh[i] = hh;
            area += (long) ww * (long) hh;
            maxW = Math.max(maxW, ww);
            maxH = Math.max(maxH, hh);
        }

        int size = (int) Math.ceil(Math.sqrt(area));
        int start = nextPow2(Math.max(size, Math.max(maxW, maxH)));
        int atlasW = start;
        int atlasH = start;

        for (int i = 0; i < 12; i++) {
            AtlasPacking packed = tryPack(atlasW, atlasH, rw, rh, padding);
            if (packed != null) {
                return packed;
            }
            if (atlasW <= atlasH) atlasW *= 2;
            else atlasH *= 2;
        }

        throw new IOException("Failed to pack textures into atlas");
    }

    private static AtlasPacking tryPack(int atlasW, int atlasH, int[] rw, int[] rh, int padding) {
        int n = rw.length;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBRPContext ctx = STBRPContext.malloc(stack);
            STBRPNode.Buffer nodes = STBRPNode.malloc(Math.max(1, atlasW), stack);
            STBRectPack.stbrp_init_target(ctx, atlasW, atlasH, nodes);

            Int2ObjectMap<AtlasRegion> regions = new Int2ObjectOpenHashMap<>();
            try (STBRPRect.Buffer rects = STBRPRect.malloc(n, stack)) {
                for (int i = 0; i < n; i++) {
                    rects.get(i).id(i).w(rw[i]).h(rh[i]);
                }

                STBRectPack.stbrp_pack_rects(ctx, rects);

                for (int i = 0; i < n; i++) {
                    STBRPRect r = rects.get(i);
                    if (!r.was_packed()) {
                        return null;
                    }
                    regions.put(r.id(), new AtlasRegion(r.x() + padding, r.y() + padding, rw[r.id()] - padding * 2, rh[r.id()] - padding * 2));
                }
            }

            return new AtlasPacking(atlasW, atlasH, regions);
        }
    }

    private static int nextPow2(int v) {
        int x = Math.max(1, v - 1);
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
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
