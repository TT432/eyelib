package io.github.tt432.eyelib.client.model.importer;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bedrock.BedrockGeometryModel;
import io.github.tt432.eyelib.client.model.bbmodel.BBModel;
import io.github.tt432.eyelib.client.model.bbmodel.Element;
import io.github.tt432.eyelib.client.model.bbmodel.FaceData;
import io.github.tt432.eyelib.client.model.bbmodel.Faces;
import io.github.tt432.eyelib.client.model.bbmodel.Group;
import io.github.tt432.eyelib.client.model.bbmodel.Outliner;
import io.github.tt432.eyelib.client.model.bbmodel.Texture;
import io.github.tt432.eyelib.util.EntryStreams;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record ImportedModelData(
        String name,
        AABB visibleBox,
        List<ImportedModelTexture> textures,
        List<ImportedBoneData> bones
) {
    record ImportedModelTexture(int width, int height, @Nullable NativeImage nativeImage) {
    }

    public static ImportedModelData fromBlockbench(BBModel source) {
        Map<String, Group> groupMap = source.groups().stream().map(group -> Map.entry(group.uuid(), group)).collect(EntryStreams.collect());
        Map<String, Element> elementMap = new HashMap<>();
        for (Element element : source.elements()) {
            if (element.uuid() != null) {
                elementMap.put(element.uuid(), element);
            }
        }

        List<ImportedBoneData> bones = new ArrayList<>();
        for (Outliner entry : source.outliner()) {
            processOutlinerEntry(entry, null, elementMap, groupMap, source.textures(), bones);
        }

        return new ImportedModelData(source.modelIdentifier(), visibleBox(source.visibleBox()), importedTextures(source.textures()), bones);
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
            Vector3f bedrockRotation = new Vector3f(bone.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
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
                    bone.textureMeshes().stream().map(tm -> importedTextureMesh(tm, bone.pivot())).toList()
            ));
        }

        return new ImportedModelData(
                source.description().identifier(),
                visibleBox(source.description()),
                List.of(new ImportedModelTexture(
                        Math.max(source.description().textureWidth(), 1),
                        Math.max(source.description().textureHeight(), 1),
                        null
                )),
                bones
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
        NativeImage atlasImage = repackTextureImage(packedAtlasWidth, packedAtlasHeight, textureOffsets);
        List<ImportedBoneData> repackedBones = bones.stream()
                .map(bone -> repackBone(bone, packedAtlasWidth, packedAtlasHeight, textureOffsets))
                .toList();
        return new ImportedModelData(name, visibleBox, List.of(new ImportedModelTexture(packedAtlasWidth, packedAtlasHeight, atlasImage)), repackedBones);
    }

    @Nullable
    private NativeImage repackTextureImage(int atlasWidth, int atlasHeight, int[] textureOffsets) {
        if (textures.stream().anyMatch(texture -> texture.nativeImage() == null)) {
            return null;
        }

        NativeImage atlasImage = new NativeImage(atlasWidth, atlasHeight, true);
        for (int i = 0; i < textures.size(); i++) {
            NativeImage sourceImage = textures.get(i).nativeImage();
            if (sourceImage == null) {
                continue;
            }

            sourceImage.copyRect(atlasImage, 0, 0, textureOffsets[i], 0, sourceImage.getWidth(), sourceImage.getHeight(), false, false);
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
                .map(texture -> new ImportedModelTexture(textureWidth(texture), textureHeight(texture), texture.nativeImage()))
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
        for (String cubeId : entry.cubes()) {
            Element element = elementMap.get(cubeId);
            if (element == null) {
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
                group.rotation() == null ? new Vector3f() : new Vector3f(group.rotation()).mul(EyeMath.DEGREES_TO_RADIANS),
                cubes,
                importedLocators(entry, group),
                null,
                false,
                group.mirror_uv(),
                null,
                List.of()
        );
        bones.add(bone);

        for (Outliner child : entry.children()) {
            processOutlinerEntry(child, bone, elementMap, groupMap, textures, bones);
        }
    }

    @Nullable
    private static ImportedCubeData importedCube(Element element, List<Texture> textures) {
        Vector3f[] corners = corners(element);
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
                uv(element.faces(), 4, textures),
                uv(element.faces(), 5, textures),
                uv(element.faces(), 1, textures),
                uv(element.faces(), 0, textures),
                uv(element.faces(), 3, textures),
                uv(element.faces(), 2, textures)
        );

        List<ImportedFaceData> faces = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            if (uvs.get(i) == null) {
                continue;
            }

            List<Vector3f> facePositions = positions.get(i).stream().map(Vector3f::new).toList();
            List<Vector2f> faceUvs = uvs.get(i).stream().map(Vector2f::new).toList();
            faces.add(new ImportedFaceData(facePositions, faceUvs, new Vector3f(normals.get(i)), textureIndex(element.faces(), i), null));
        }

        return faces.isEmpty() ? null : new ImportedCubeData(faces);
    }

    private static Vector3f[] corners(Element element) {
        final float scalar = 1F / 16F;
        float maxX = (float) (element.to().x + element.inflate()) * scalar;
        float maxY = (float) (element.to().y + element.inflate()) * scalar;
        float maxZ = (float) (element.to().z + element.inflate()) * scalar;
        float minX = (float) (element.from().x - element.inflate()) * scalar;
        float minY = (float) (element.from().y - element.inflate()) * scalar;
        float minZ = (float) (element.from().z - element.inflate()) * scalar;

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
        Vector3f rotation = new Vector3f(element.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
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

        // Align with Blockbench Bedrock parser behavior:
        // parseCube() flips up/down UV rectangles (face.uv = [u1,v1,u0,v0]).
        List<Vector2f> sourceUvs = ("up".equals(faceName) || "down".equals(faceName))
                ? List.of(uvsWork.get(2), uvsWork.get(3), uvsWork.get(0), uvsWork.get(1))
                : uvsWork;

        // The face position order above already matches Blockbench CubeFace.UVToLocal()
        // corner mapping for Bedrock cubes. Reordering here breaks UV-to-vertex pairing.
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
                    : new Vector3f(le.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
            if (le.rotation() != null) {
                rotRad.x *= -1;
                rotRad.y *= -1;
            }
            list.add(new ImportedLocatorData(le.name(), offset, rotRad, le.ignoreInheritedScale(), le.nullObject()));
        }
        return list;
    }

    private static ImportedTextureMeshData importedTextureMesh(BedrockGeometryModel.TextureMeshDef tm, Vector3f bonePivotPixels) {
        Vector3f localPivot = new Vector3f(tm.localPivot());
        localPivot.z *= -1;

        Vector3f position = new Vector3f(tm.position());
        position.y *= -1;
        position.y += bonePivotPixels.y;
        position.x *= -1;

        Vector3f rotationDeg = new Vector3f(tm.rotation());
        rotationDeg.x *= -1;
        rotationDeg.y *= -1;

        Vector3f posBlock = new Vector3f(-position.x / 16f, position.y / 16f, position.z / 16f);
        Vector3f lpBlock = new Vector3f(-localPivot.x / 16f, localPivot.y / 16f, localPivot.z / 16f);

        Vector3f rotRad = new Vector3f(rotationDeg).mul(EyeMath.DEGREES_TO_RADIANS);
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
        // Match Blockbench Bedrock parseCube: from.x = -(origin.x + size.x), with inflate expanding the box
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

        // Match Blockbench Bedrock parseCube transforms:
        // pivot.x *= -1; rotation.x/y *= -1
        Vector3f origin = new Vector3f(cube.pivot()).div(16);
        origin.x *= -1;
        Vector3f rotation = new Vector3f(cube.rotation()).mul(EyeMath.DEGREES_TO_RADIANS);
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
        float dx = cube.size().x;
        float dy = cube.size().y;
        float dz = cube.size().z;
        // Match Blockbench Bedrock parseCube box-uv side strip order: east, north, west, south.
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
    private static List<Vector2f> uv(Faces faces, int faceIndex, List<Texture> textures) {
        FaceData faceData = switch (faceIndex) {
            case 0 -> faces.north();
            case 1 -> faces.east();
            case 2 -> faces.south();
            case 3 -> faces.west();
            case 4 -> faces.up();
            case 5 -> faces.down();
            default -> null;
        };
        if (faceData == null || faceData.uv() == null) {
            return null;
        }
        if (faceData.texture() < 0 || faceData.texture() >= textures.size()) {
            return null;
        }

        Texture texture = textures.get(faceData.texture());
        float width = texture.uvWidth();
        float height = texture.uvHeight();
        if (width == 0 || height == 0) {
            return null;
        }

        float u0 = faceData.uv().x / width;
        float v0 = faceData.uv().y / height;
        float u1 = faceData.uv().z / width;
        float v1 = faceData.uv().w / height;
        return rotateUv(ObjectList.of(
                new Vector2f(u0, v0),
                new Vector2f(u1, v0),
                new Vector2f(u1, v1),
                new Vector2f(u0, v1)
        ), faceData.rotation());
    }

    private static List<Vector2f> rotateUv(List<Vector2f> uvs, int degree) {
        return switch (degree) {
            case 90 -> List.of(uvs.get(1), uvs.get(2), uvs.get(3), uvs.get(0));
            case 180 -> List.of(uvs.get(2), uvs.get(3), uvs.get(0), uvs.get(1));
            case 270 -> List.of(uvs.get(3), uvs.get(0), uvs.get(1), uvs.get(2));
            default -> uvs;
        };
    }

    private static int textureIndex(Faces faces, int faceIndex) {
        FaceData faceData = switch (faceIndex) {
            case 0 -> faces.north();
            case 1 -> faces.east();
            case 2 -> faces.south();
            case 3 -> faces.west();
            case 4 -> faces.up();
            case 5 -> faces.down();
            default -> null;
        };
        return faceData == null ? -1 : faceData.texture();
    }

    private static List<ImportedLocatorData> importedLocators(Outliner entry, Group group) {
        return List.of();
    }

    private static AABB visibleBox(List<Double> visibleBox) {
        if (visibleBox.size() < 3) {
            return Model.EMPTY_VISIBLE_BOX;
        }

        double width = visibleBox.get(0);
        double height = visibleBox.get(1);
        double depth = visibleBox.get(2);
        return new AABB(-width / 2, 0, -depth / 2, width / 2, height, depth / 2);
    }

    private static AABB visibleBox(BedrockGeometryModel.Description description) {
        if (description.visibleBoundsWidth() <= 0 || description.visibleBoundsHeight() <= 0) {
            return Model.EMPTY_VISIBLE_BOX;
        }

        double halfWidth = description.visibleBoundsWidth() / 2D;
        double minY = description.visibleBoundsOffset().y - description.visibleBoundsHeight() / 2D;
        double maxY = description.visibleBoundsOffset().y + description.visibleBoundsHeight() / 2D;
        return new AABB(-halfWidth, minY, -halfWidth, halfWidth, maxY, halfWidth);
    }
}
