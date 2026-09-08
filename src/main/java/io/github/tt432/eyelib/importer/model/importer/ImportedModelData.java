package io.github.tt432.eyelib.importer.model.importer;

import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.VisibleBox;
import io.github.tt432.eyelib.importer.model.bedrock.BedrockGeometryModel;
import io.github.tt432.eyelib.importer.model.bbmodel.BBModel;
import io.github.tt432.eyelib.importer.model.bbmodel.Element;
import io.github.tt432.eyelib.importer.model.bbmodel.FaceData;
import io.github.tt432.eyelib.importer.model.bbmodel.Faces;
import io.github.tt432.eyelib.importer.model.bbmodel.Group;
import io.github.tt432.eyelib.importer.model.bbmodel.Outliner;
import io.github.tt432.eyelib.importer.model.bbmodel.Texture;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 从 Blockbench 或 Bedrock 源数据转换得到的中间模型表示，支持纹理重打包。
 * @author TT432 */
public record ImportedModelData(
        String name,
        VisibleBox visibleBox,
        List<ImportedModelTexture> textures,
        List<ImportedBoneData> bones,
        boolean flipAnimation
) {
    /**
     * 动画应用时是否需要按基岩版 geo 约定翻转（rotation ×(-1,-1,1)，position ×(-1,1,1)）。
     * 基岩版 geo 导入器在导入时镜像了 X（pivot.x *= -1 等），动画翻转是配套的补偿；
     * bbmodel 导入器做恒等导入（不镜像），因此不需要此翻转。
     * 默认 true（向后兼容基岩版 geo 模型）。
     */
    public ImportedModelData {
    }

    /** 向后兼容构造器：默认 true（基岩版 geo 约定）。 */
    public ImportedModelData(String name, VisibleBox visibleBox, List<ImportedModelTexture> textures, List<ImportedBoneData> bones) {
        this(name, visibleBox, textures, bones, true);
    }

    private static final String SYNTHETIC_ROOT_BONE_NAME = "__bbmodel_root__";
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180D);

    public record ImportedModelTexture(int width, int height, @Nullable ImportedImageData imageData) {
    }

    public static ImportedModelData fromBlockbench(BBModel source) {
        Map<String, Group> groupMap = source.groups().stream().collect(Collectors.toMap(Group::uuid, group -> group));
        Map<String, Element> elementMap = new HashMap<>();
        for (Element element : source.elements()) {
            if (element.uuid() != null) {
                elementMap.put(element.uuid(), element);
            }
        }

        List<ImportedBoneData> bones = new ArrayList<>();
        List<String> rootCubeIds = new ArrayList<>();
        for (Outliner.CubeOrOutliner entry : source.outliner()) {
            if (entry.outliner() != null) {
                processOutlinerEntry(entry.outliner(), null, elementMap, groupMap, source.textures(), bones);
            } else if (entry.uuid() != null) {
                rootCubeIds.add(entry.uuid());
            }
        }

        ImportedBoneData syntheticRoot = importedRootCubeBone(rootCubeIds, elementMap, source.textures());
        if (syntheticRoot != null) {
            bones.add(syntheticRoot);
        }

        // bbmodel 导入器恒等导入（不镜像 X），动画不需要翻转补偿
        return new ImportedModelData(source.modelIdentifier(), VisibleBox.fromBlockbenchDimensions(source.visibleBox()), importedTextures(source.textures()), bones, false);
    }

    @Nullable
    private static ImportedBoneData importedRootCubeBone(List<String> cubeIds, Map<String, Element> elementMap, List<Texture> textures) {
        if (cubeIds.isEmpty()) {
            return null;
        }

        List<ImportedCubeData> cubes = new ArrayList<>();
        List<ImportedLocatorData> locators = new ArrayList<>();
        for (String cubeId : cubeIds) {
            Element element = elementMap.get(cubeId);
            if (element == null) {
                continue;
            }

            ImportedLocatorData locator = importedLocator(element);
            if (locator != null) {
                locators.add(locator);
                continue;
            }

            ImportedCubeData cube = importedCube(element, textures);
            if (cube != null) {
                cubes.add(cube);
            }
        }

        if (cubes.isEmpty() && locators.isEmpty()) {
            return null;
        }

        return new ImportedBoneData(
                GlobalBoneIdHandler.get(SYNTHETIC_ROOT_BONE_NAME),
                -1,
                new Vector3f(),
                new Vector3f(),
                cubes,
                locators,
                null,
                false,
                false,
                null,
                List.of()
        );
    }

    public static ImportedModelData fromBedrock(BedrockGeometryModel.Geometry source) {
        List<ImportedBoneData> bones = new ArrayList<>();
        Map<String, Integer> boneIds = new LinkedHashMap<>();
        for (BedrockGeometryModel.Bone bone : source.bones()) {
            boneIds.put(bone.name(), GlobalBoneIdHandler.get(bone.name()));
        }

        for (BedrockGeometryModel.Bone bone : source.bones()) {
            Integer parentId = bone.parent() == null ? null : boneIds.get(bone.parent());
            boolean boneMirror = bone.mirror();
            List<ImportedCubeData> cubes = bone.cubes().stream()
                    .map(cube -> importedCube(cube, source.description(), cube.mirror() != null ? cube.mirror() : boneMirror))
                    .filter(cube -> cube != null)
                    .toList();

            Vector3f bedrockPivot = new Vector3f(bone.pivot()).div(16);
            bedrockPivot.x *= -1;
            Vector3f bedrockRotation = new Vector3f(bone.rotation()).mul(DEGREES_TO_RADIANS);
            bedrockRotation.x *= -1;
            bedrockRotation.y *= -1;

            bones.add(new ImportedBoneData(
                    Objects.requireNonNull(boneIds.get(bone.name()), "Missing bone id for " + bone.name()),
                    parentId == null ? -1 : parentId,
                    bedrockPivot,
                    bedrockRotation,
                    cubes,
                    importedBedrockLocators(bone),
                    bone.material(),
                    bone.reset(),
                    boneMirror,
                    bone.binding(),
                    bone.textureMeshes().stream().map(tm -> importedTextureMesh(tm)).toList()
            ));
        }

        return new ImportedModelData(
                source.description().identifier(),
                VisibleBox.fromBedrockDescription(
                        source.description().visibleBoundsWidth(),
                        source.description().visibleBoundsHeight(),
                        source.description().visibleBoundsOffset().y
                ),
                List.of(new ImportedModelTexture(
                        Math.max(source.description().textureWidth(), 1),
                        Math.max(source.description().textureHeight(), 1),
                        null
                )),
                bones,
                true  // 基岩版 geo 导入器镜像了 X，动画需要翻转补偿
        );
    }

    ImportedModelData repackTextures() {
        if (textures.size() <= 1) {
            return this;
        }

        int atlasWidth = 0;
        int atlasHeight = 1;
        int[] textureOffsets = new int[textures.size()];
        for (int i = 0; i < textures.size(); i++) {
            ImportedModelTexture texture = textures.get(i);
            textureOffsets[i] = atlasWidth;
            atlasWidth += Math.max(texture.width(), 1);
            atlasHeight = Math.max(atlasHeight, Math.max(texture.height(), 1));
        }

        final int packedAtlasWidth = atlasWidth;
        final int packedAtlasHeight = atlasHeight;
        ImportedImageData atlasImage = repackTextureImage(packedAtlasWidth, packedAtlasHeight, textureOffsets);
        List<ImportedBoneData> repackedBones = bones.stream()
                .map(bone -> repackBone(bone, packedAtlasWidth, packedAtlasHeight, textureOffsets))
                .toList();
        return new ImportedModelData(name, visibleBox, List.of(new ImportedModelTexture(packedAtlasWidth, packedAtlasHeight, atlasImage)), repackedBones, flipAnimation);
    }

    @Nullable
    private ImportedImageData repackTextureImage(int atlasWidth, int atlasHeight, int[] textureOffsets) {
        if (textures.stream().anyMatch(texture -> texture.imageData() == null)) {
            return null;
        }

        ImportedImageData atlasImage = ImportedImageData.empty(atlasWidth, atlasHeight);
        for (int i = 0; i < textures.size(); i++) {
            ImportedImageData sourceImage = textures.get(i).imageData();
            if (sourceImage == null) {
                continue;
            }

            sourceImage.blitTo(atlasImage, textureOffsets[i], 0);
        }
        return atlasImage;
    }

    private ImportedBoneData repackBone(ImportedBoneData bone, int atlasWidth, int atlasHeight, int[] textureOffsets) {
        List<ImportedCubeData> repackedCubes = bone.cubes().stream()
                .map(cube -> repackCube(cube, atlasWidth, atlasHeight, textureOffsets))
                .toList();
        return new ImportedBoneData(
                bone.id(),
                bone.parentId(),
                new Vector3f(bone.pivot()),
                new Vector3f(bone.rotation()),
                repackedCubes,
                bone.locators(),
                bone.material(),
                bone.reset(),
                bone.mirrorUv(),
                bone.binding(),
                bone.textureMeshes()
        );
    }

    private ImportedCubeData repackCube(ImportedCubeData cube, int atlasWidth, int atlasHeight, int[] textureOffsets) {
        return new ImportedCubeData(cube.faces().stream()
                .map(face -> repackFace(face, atlasWidth, atlasHeight, textureOffsets))
                .toList());
    }

    private ImportedFaceData repackFace(ImportedFaceData face, int atlasWidth, int atlasHeight, int[] textureOffsets) {
        int textureIndex = face.textureIndex();
        if (textureIndex < 0 || textureIndex >= textures.size()) {
            return new ImportedFaceData(
                    face.positions(),
                    face.uvs(),
                    face.normal(),
                    face.textureIndex(),
                    face.materialInstance()
            );
        }

        ImportedModelTexture texture = textures.get(textureIndex);
        float textureWidth = Math.max(texture.width(), 1);
        float textureHeight = Math.max(texture.height(), 1);
        float atlasWidthF = Math.max(atlasWidth, 1);
        float atlasHeightF = Math.max(atlasHeight, 1);
        float atlasOffsetU = textureOffsets[textureIndex] / atlasWidthF;

        List<Vector2f> remappedUvs = face.uvs().stream()
                .map(uv -> new Vector2f(
                        atlasOffsetU + uv.x * (textureWidth / atlasWidthF),
                        uv.y * (textureHeight / atlasHeightF)
                ))
                .toList();
        return new ImportedFaceData(
                face.positions(),
                remappedUvs,
                face.normal(),
                0,
                face.materialInstance()
        );
    }

    private static List<ImportedModelTexture> importedTextures(List<Texture> textures) {
        return textures.stream()
                .map(texture -> new ImportedModelTexture(textureWidth(texture), textureHeight(texture), texture.imageData()))
                .toList();
    }

    private static int textureWidth(Texture texture) {
        return Math.max(texture.uvWidth(), Math.max(texture.width(), 1));
    }

    private static int textureHeight(Texture texture) {
        return Math.max(texture.uvHeight(), Math.max(texture.height(), 1));
    }

    private static void processOutlinerEntry(
            Outliner entry,
            @Nullable ImportedBoneData parent,
            Map<String, Element> elementMap,
            Map<String, Group> groupMap,
            List<Texture> textures,
            List<ImportedBoneData> bones
    ) {
        Group group = groupMap.get(entry.uuid());
        if (group == null && entry.group().isPresent()) {
            group = entry.group().get();
        }
        if (group == null) {
            return;
        }

        List<ImportedCubeData> cubes = new ArrayList<>();
        List<ImportedLocatorData> locators = new ArrayList<>();
        for (String cubeId : entry.cubes()) {
            Element element = elementMap.get(cubeId);
            if (element == null) {
                continue;
            }

            ImportedLocatorData locator = importedLocator(element);
            if (locator != null) {
                locators.add(locator);
                continue;
            }

            ImportedCubeData cube = importedCube(element, textures);
            if (cube != null) {
                cubes.add(cube);
            }
        }

        ImportedBoneData bone = new ImportedBoneData(
                GlobalBoneIdHandler.get(group.name()),
                parent == null ? -1 : parent.id(),
                group.origin() == null ? new Vector3f() : new Vector3f(group.origin()).div(16),
                group.rotation() == null ? new Vector3f() : new Vector3f(group.rotation()).mul(DEGREES_TO_RADIANS),
                cubes,
                locators,
                null,
                group.reset(),
                group.mirror_uv(),
                group.bedrockBinding().isEmpty() ? null : group.bedrockBinding(),
                List.of()
        );
        bones.add(bone);

        for (Outliner child : entry.children()) {
            processOutlinerEntry(child, bone, elementMap, groupMap, textures, bones);
        }
    }

    @Nullable
    private static ImportedCubeData importedCube(Element element, List<Texture> textures) {
        Vector3f from = element.from();
        Vector3f to = element.to();
        if (!"cube".equals(element.type())
                || element.faces() == null
                || from == null
                || to == null) {
            return null;
        }

        Vector3f[] corners = corners(element, from, to);
        applyRotation(element, corners);

        Vector3f lfu = corners[0];
        Vector3f rfu = corners[1];
        Vector3f rbu = corners[2];
        Vector3f lbu = corners[3];
        Vector3f lfd = corners[4];
        Vector3f rfd = corners[5];
        Vector3f rbd = corners[6];
        Vector3f lbd = corners[7];

        List<List<Vector3f>> positions = ObjectList.of(
                ObjectList.of(lfu, rfu, rbu, lbu),
                ObjectList.of(lbd, rbd, rfd, lfd),
                ObjectList.of(rbu, rfu, rfd, rbd),
                ObjectList.of(rfu, lfu, lfd, rfd),
                ObjectList.of(lfu, lbu, lbd, lfd),
                ObjectList.of(lbu, rbu, rbd, lbd)
        );
        List<Vector3f> normals = normals(positions);
        List<List<Vector2f>> uvs = ObjectList.of(
                uv(element, 4, textures),
                uv(element, 5, textures),
                uv(element, 1, textures),
                uv(element, 0, textures),
                uv(element, 3, textures),
                uv(element, 2, textures)
        );

        List<ImportedFaceData> faces = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            if (uvs.get(i) == null) {
                continue;
            }

            List<Vector3f> facePositions = positions.get(i).stream().map(Vector3f::new).toList();
            List<Vector2f> faceUvs = uvs.get(i).stream().map(Vector2f::new).toList();
            faces.add(new ImportedFaceData(facePositions, faceUvs, new Vector3f(normals.get(i)),
                    resolveTextureIndex(faceAt(element.faces(), i), textures), null));
        }

        return faces.isEmpty() ? null : new ImportedCubeData(faces);
    }

    private static Vector3f[] corners(Element element, Vector3f from, Vector3f to) {
        final float scalar = 1F / 16F;
        float maxX = (float) (to.x + element.inflate()) * scalar;
        float maxY = (float) (to.y + element.inflate()) * scalar;
        float maxZ = (float) (to.z + element.inflate()) * scalar;
        float minX = (float) (from.x - element.inflate()) * scalar;
        float minY = (float) (from.y - element.inflate()) * scalar;
        float minZ = (float) (from.z - element.inflate()) * scalar;

        return new Vector3f[]{
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, minY, maxZ)
        };
    }

    private static void applyRotation(Element element, Vector3f[] corners) {
        if (element.rotation() == null || element.origin() == null) {
            return;
        }

        Vector3f origin = new Vector3f(element.origin()).div(16);
        Vector3f rotation = new Vector3f(element.rotation()).mul(DEGREES_TO_RADIANS);
        Matrix4f transform = new Matrix4f()
                .translation(origin)
                .rotateAffineZYX(rotation.z, rotation.y, rotation.x)
                .translate(origin.negate(new Vector3f()));

        for (Vector3f corner : corners) {
            corner.mulPosition(transform);
        }
    }

    private static List<Vector3f> normals(List<List<Vector3f>> positions) {
        return ObjectList.of(
                normal(positions.get(0).get(0), positions.get(0).get(1), positions.get(0).get(2)),
                normal(positions.get(1).get(0), positions.get(1).get(1), positions.get(1).get(2)),
                normal(positions.get(2).get(0), positions.get(2).get(1), positions.get(2).get(2)),
                normal(positions.get(3).get(0), positions.get(3).get(1), positions.get(3).get(2)),
                normal(positions.get(4).get(0), positions.get(4).get(1), positions.get(4).get(2)),
                normal(positions.get(5).get(0), positions.get(5).get(1), positions.get(5).get(2))
        );
    }

    private static Vector3f normal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f normal = b.sub(a, new Vector3f()).cross(c.sub(a, new Vector3f()));
        return normal.lengthSquared() == 0 ? normal.zero() : normal.normalize();
    }

    @Nullable
    private static ImportedCubeData importedCube(BedrockGeometryModel.Cube cube, BedrockGeometryModel.Description description, boolean mirrorUv) {
        Vector3f[] corners = corners(cube);
        applyRotation(cube, corners);

        Vector3f lfu = corners[0];
        Vector3f rfu = corners[1];
        Vector3f rbu = corners[2];
        Vector3f lbu = corners[3];
        Vector3f lfd = corners[4];
        Vector3f rfd = corners[5];
        Vector3f rbd = corners[6];
        Vector3f lbd = corners[7];

        Map<String, List<Vector3f>> positions = Map.of(
                "up", ObjectList.of(lfu, rfu, rbu, lbu),
                "down", ObjectList.of(lbd, rbd, rfd, lfd),
                "east", ObjectList.of(rbu, rfu, rfd, rbd),
                "north", ObjectList.of(rfu, lfu, lfd, rfd),
                "west", ObjectList.of(lfu, lbu, lbd, lfd),
                "south", ObjectList.of(lbu, rbu, rbd, lbd)
        );

        List<ImportedFaceData> faces = new ArrayList<>();
        if (cube.boxUv() != null) {
            addFace(faces, positions, "north", bedrockBoxUv(cube, description, "north"), null, mirrorUv);
            addFace(faces, positions, "east", bedrockBoxUv(cube, description, "east"), null, mirrorUv);
            addFace(faces, positions, "south", bedrockBoxUv(cube, description, "south"), null, mirrorUv);
            addFace(faces, positions, "west", bedrockBoxUv(cube, description, "west"), null, mirrorUv);
            addFace(faces, positions, "up", bedrockBoxUv(cube, description, "up"), null, mirrorUv);
            addFace(faces, positions, "down", bedrockBoxUv(cube, description, "down"), null, mirrorUv);
        } else {
            for (Map.Entry<String, BedrockGeometryModel.FaceUv> entry : cube.faceUvs().entrySet()) {
                addFace(faces, positions, entry.getKey(), bedrockFaceUv(entry.getValue(), description), entry.getValue().materialInstance(), mirrorUv);
            }
        }

        return faces.isEmpty() ? null : new ImportedCubeData(faces);
    }

    private static void addFace(
            List<ImportedFaceData> faces,
            Map<String, List<Vector3f>> positions,
            String faceName,
            @Nullable List<Vector2f> uvs,
            @Nullable String materialInstance,
            boolean mirrorUv
    ) {
        List<Vector3f> facePositions = positions.get(faceName);
        if (facePositions == null || uvs == null) {
            return;
        }

        if (facePositions.size() < 4 || uvs.size() < 4) {
            return;
        }

        List<Vector2f> uvsWork = mirrorUv ? mirrorUvsHorizontally(uvs) : uvs;

        // 对齐 Blockbench Bedrock 解析器行为：
        // parseCube() 会翻转 up/down 的 UV 矩形 (face.uv = [u1,v1,u0,v0])。
        List<Vector2f> sourceUvs = ("up".equals(faceName) || "down".equals(faceName))
                ? List.of(uvsWork.get(2), uvsWork.get(3), uvsWork.get(0), uvsWork.get(1))
                : uvsWork;

        // 上面的面位置顺序已对齐 Blockbench CubeFace.UVToLocal()。
        // Bedrock 立方体的角映射。在这里重排会破坏 UV 与顶点的对应关系。
        List<Vector3f> copiedPositions = facePositions.stream().map(Vector3f::new).toList();
        List<Vector2f> copiedUvs = sourceUvs.stream().map(Vector2f::new).toList();
        faces.add(new ImportedFaceData(copiedPositions, copiedUvs, normal(
                copiedPositions.get(0),
                copiedPositions.get(1),
                copiedPositions.get(2)
        ), 0, materialInstance));
    }

    private static List<Vector2f> mirrorUvsHorizontally(List<Vector2f> uvs) {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        for (Vector2f uv : uvs) {
            minU = Math.min(minU, uv.x);
            maxU = Math.max(maxU, uv.x);
        }
        float sum = minU + maxU;
        List<Vector2f> out = new ArrayList<>(uvs.size());
        for (Vector2f uv : uvs) {
            out.add(new Vector2f(sum - uv.x, uv.y));
        }
        return out;
    }

    private static List<ImportedLocatorData> importedBedrockLocators(BedrockGeometryModel.Bone bone) {
        List<ImportedLocatorData> list = new ArrayList<>();
        for (BedrockGeometryModel.BoneLocatorEntry le : bone.locators()) {
            Vector3f offset = new Vector3f(le.offset()).div(16);
            offset.x *= -1;
            Vector3f rotRad = le.rotation() == null
                    ? new Vector3f()
                    : new Vector3f(le.rotation()).mul(DEGREES_TO_RADIANS);
            if (le.rotation() != null) {
                rotRad.x *= -1;
                rotRad.y *= -1;
            }
            list.add(new ImportedLocatorData(le.name(), offset, rotRad, le.ignoreInheritedScale(), le.nullObject()));
        }
        return list;
    }

    private static ImportedTextureMeshData importedTextureMesh(BedrockGeometryModel.TextureMeshDef tm) {
        // 转换约定经 Blockbench 5.x bedrock codec 实测（Blockbench 内部空间 == eyelib 几何空间）：
        // position=(-x,-y,z)、rotation=(-rx,-ry,rz)、local_pivot=(x,y,-z)、scale 不变
        Vector3f posBlock = new Vector3f(-tm.position().x / 16f, -tm.position().y / 16f, tm.position().z / 16f);
        Vector3f lpBlock = new Vector3f(tm.localPivot().x / 16f, tm.localPivot().y / 16f, -tm.localPivot().z / 16f);
        Vector3f rotRad = new Vector3f(tm.rotation()).mul(DEGREES_TO_RADIANS);
        rotRad.x *= -1;
        rotRad.y *= -1;

        Vector3f scale = new Vector3f(tm.scale());
        if (scale.lengthSquared() == 0) {
            scale.set(1, 1, 1);
        }
        return new ImportedTextureMeshData(tm.texture(), posBlock, rotRad, lpBlock, scale);
    }

    private static Vector3f[] corners(BedrockGeometryModel.Cube cube) {
        final float scalar = 1F / 16F;
        float inf = cube.inflate();
        float ox = cube.origin().x;
        float oy = cube.origin().y;
        float oz = cube.origin().z;
        float sx = cube.size().x;
        float sy = cube.size().y;
        float sz = cube.size().z;
        // 对齐 Blockbench Bedrock parseCube: from.x = -(origin.x + size.x)，inflate 向外扩展方块。
        float minX = (-(ox + sx + inf)) * scalar;
        float minY = (oy - inf) * scalar;
        float minZ = (oz - inf) * scalar;
        float maxX = minX + (sx + 2 * inf) * scalar;
        float maxY = minY + (sy + 2 * inf) * scalar;
        float maxZ = minZ + (sz + 2 * inf) * scalar;

        return new Vector3f[]{
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, minY, maxZ)
        };
    }

    private static void applyRotation(BedrockGeometryModel.Cube cube, Vector3f[] corners) {
        if (cube.pivot() == null || (cube.rotation().x == 0 && cube.rotation().y == 0 && cube.rotation().z == 0)) {
            return;
        }

        // 对齐 Blockbench Bedrock parseCube 变换：
        // pivot.x *= -1; rotation.x/y *= -1
        Vector3f origin = new Vector3f(cube.pivot()).div(16);
        origin.x *= -1;
        Vector3f rotation = new Vector3f(cube.rotation()).mul(DEGREES_TO_RADIANS);
        rotation.x *= -1;
        rotation.y *= -1;
        Matrix4f transform = new Matrix4f()
                .translation(origin)
                .rotateAffineZYX(rotation.z, rotation.y, rotation.x)
                .translate(origin.negate(new Vector3f()));

        for (Vector3f corner : corners) {
            corner.mulPosition(transform);
        }
    }

    private static List<Vector2f> bedrockFaceUv(BedrockGeometryModel.FaceUv faceUv, BedrockGeometryModel.Description description) {
        float width = Math.max(description.textureWidth(), 1);
        float height = Math.max(description.textureHeight(), 1);
        float u0 = faceUv.uv().x / width;
        float v0 = faceUv.uv().y / height;
        float u1 = (faceUv.uv().x + faceUv.uvSize().x) / width;
        float v1 = (faceUv.uv().y + faceUv.uvSize().y) / height;
        return rotateUv(ObjectList.of(
                new Vector2f(u0, v0),
                new Vector2f(u1, v0),
                new Vector2f(u1, v1),
                new Vector2f(u0, v1)
        ), faceUv.uvRotation());
    }

    private static List<Vector2f> bedrockBoxUv(BedrockGeometryModel.Cube cube, BedrockGeometryModel.Description description, String faceName) {
        Vector2f uv = cube.boxUv();
        if (uv == null) {
            return List.of();
        }
        float dx = (float) Math.floor(cube.size().x);
        float dy = (float) Math.floor(cube.size().y);
        float dz = (float) Math.floor(cube.size().z);
        // 对齐 Blockbench Bedrock parseCube box-uv 侧边条带顺序: east, north, west, south。
        Vector2f start = switch (faceName) {
            case "north" -> new Vector2f(uv.x + dz, uv.y + dz);
            case "east" -> new Vector2f(uv.x, uv.y + dz);
            case "west" -> new Vector2f(uv.x + dz + dx, uv.y + dz);
            case "south" -> new Vector2f(uv.x + dz + dx + dz, uv.y + dz);
            case "up" -> new Vector2f(uv.x + dz, uv.y);
            case "down" -> new Vector2f(uv.x + dz + dx, uv.y);
            default -> null;
        };
        Vector2f size = switch (faceName) {
            case "north", "south" -> new Vector2f(dx, dy);
            case "east", "west" -> new Vector2f(dz, dy);
            case "up", "down" -> new Vector2f(dx, dz);
            default -> null;
        };
        if (start == null || size == null) {
            return List.of();
        }
        return bedrockFaceUv(new BedrockGeometryModel.FaceUv(start, size, 0, null), description);
    }

    @Nullable
    private static List<Vector2f> uv(Element element, int faceIndex, List<Texture> textures) {
        FaceData faceData = faceAt(element.faces(), faceIndex);
        if (faceData == null) {
            return null;
        }
        int textureIndex = resolveTextureIndex(faceData, textures);
        if (textureIndex < 0 || textureIndex >= textures.size()) {
            return null;
        }

        Texture texture = textures.get(textureIndex);
        float width = texture.uvWidth();
        float height = texture.uvHeight();
        if (width == 0 || height == 0) {
            return null;
        }

        Vector4f rect = faceData.uv();
        if (rect == null) {
            // box_uv cube 可不写每面 uv：按 Blockbench updateUV 的展开表从 uv_offset 推导
            if (!element.boxUv()) {
                return null;
            }
            Vector3f boxFrom = element.from();
            Vector3f boxTo = element.to();
            if (boxFrom == null || boxTo == null) {
                return null;
            }
            rect = expandBoxUv(boxFrom, boxTo, element.uvOffset(), element.mirrorUv(), faceIndex);
        }

        float u0 = rect.x / width;
        float v0 = rect.y / height;
        float u1 = rect.z / width;
        float v1 = rect.w / height;
        return rotateUv(ObjectList.of(
                new Vector2f(u0, v0),
                new Vector2f(u1, v0),
                new Vector2f(u1, v1),
                new Vector2f(u0, v1)
        ), faceData.rotation());
    }

    @Nullable
    private static FaceData faceAt(@Nullable Faces faces, int faceIndex) {
        if (faces == null) {
            return null;
        }
        return switch (faceIndex) {
            case 0 -> faces.north();
            case 1 -> faces.east();
            case 2 -> faces.south();
            case 3 -> faces.west();
            case 4 -> faces.up();
            case 5 -> faces.down();
            default -> null;
        };
    }

    /** 面的纹理引用：int 索引直接使用；uuid 字符串按 textures 列表 uuid 解析，找不到返回 -1（面丢弃）。 */
    private static int resolveTextureIndex(@Nullable FaceData faceData, List<Texture> textures) {
        if (faceData == null) {
            return -1;
        }
        if (!faceData.textureUuid().isEmpty()) {
            for (int i = 0; i < textures.size(); i++) {
                if (faceData.textureUuid().equals(textures.get(i).uuid())) {
                    return i;
                }
            }
            return -1;
        }
        return faceData.texture();
    }

    /**
     * Blockbench box UV 展开表（cube.js updateUV）：texel 空间，相对 uv_offset。
     * up/down 的 size 为负表示矩形反向；mirror_uv 时各面水平翻转且 east/west 互换。
     */
    private static Vector4f expandBoxUv(Vector3f boxFrom, Vector3f boxTo, Vector2f uvOffset, boolean mirrorUv, int faceIndex) {
        float sx = (float) Math.floor(boxTo.x - boxFrom.x);
        float sy = (float) Math.floor(boxTo.y - boxFrom.y);
        float sz = (float) Math.floor(boxTo.z - boxFrom.z);

        // 顺序与 faceIndex 一致：north, east, south, west, up, down
        float[][] from = {
                {sz, sz},
                {0, sz},
                {sz * 2 + sx, sz},
                {sz + sx, sz},
                {sz + sx, sz},
                {sz * 2 + sx, 0},
        };
        float[][] size = {
                {sx, sy},
                {sz, sy},
                {sx, sy},
                {sz, sy},
                {-sx, -sz},
                {-sx, sz},
        };

        if (mirrorUv) {
            for (int i = 0; i < from.length; i++) {
                from[i][0] += size[i][0];
                size[i][0] *= -1;
            }
            swap(from, 1, 3);
            swap(size, 1, 3);
        }

        float u = uvOffset.x;
        float v = uvOffset.y;
        return new Vector4f(
                from[faceIndex][0] + u,
                from[faceIndex][1] + v,
                from[faceIndex][0] + size[faceIndex][0] + u,
                from[faceIndex][1] + size[faceIndex][1] + v
        );
    }

    private static void swap(float[][] array, int a, int b) {
        float[] tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }

    /** bbmodel locator / null_object → 定位器数据。恒等导入（不镜像 X），与 bbmodel cube 同空间。 */
    @Nullable
    private static ImportedLocatorData importedLocator(Element element) {
        boolean isNullObject = "null_object".equals(element.type());
        if (!isNullObject && !"locator".equals(element.type())) {
            return null;
        }

        Vector3f position = element.position() != null ? element.position() : element.from();
        Vector3f offset = position == null ? new Vector3f() : new Vector3f(position).div(16);
        Vector3f rotation = element.rotation() == null
                ? new Vector3f()
                : new Vector3f(element.rotation()).mul(DEGREES_TO_RADIANS);
        return new ImportedLocatorData(element.name(), offset, rotation, element.ignoreInheritedScale(), isNullObject);
    }

    private static List<Vector2f> rotateUv(List<Vector2f> uvs, int degree) {
        return switch (degree) {
            case 90 -> List.of(uvs.get(1), uvs.get(2), uvs.get(3), uvs.get(0));
            case 180 -> List.of(uvs.get(2), uvs.get(3), uvs.get(0), uvs.get(1));
            case 270 -> List.of(uvs.get(3), uvs.get(0), uvs.get(1), uvs.get(2));
            default -> uvs;
        };
    }

}