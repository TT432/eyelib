package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.chin.util.Tuple;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author TT432
 */
public record BrCube(
        int faceCount,
        int pointsPerFace,
        List<List<Vector3f>> vertexes,
        List<List<Vector2f>> uvs,
        List<Vector3f> normals
) implements Model.Cube.ConstCube {
    public static final Codec<BrCube> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("faceCount").forGetter(BrCube::faceCount),
            Codec.INT.fieldOf("pointsPerFace").forGetter(BrCube::pointsPerFace),
            ExtraCodecs.VECTOR3F.listOf().listOf().fieldOf("vertexes").forGetter(BrCube::vertexes),
            ChinExtraCodecs.tuple(Codec.FLOAT, Codec.FLOAT).bmap(Vector2f::new, v2f -> Tuple.of(v2f.x, v2f.y)).listOf().listOf().fieldOf("uvs").forGetter(BrCube::uvs),
            ExtraCodecs.VECTOR3F.listOf().fieldOf("normals").forGetter(BrCube::normals)
    ).apply(ins, BrCube::new));
    private static final Gson gson = new Gson();

    private static Vector3f parse(JsonObject object, String ele) {
        if (object.get(ele) instanceof JsonArray ja) {
            return new Vector3f(ja.get(0).getAsFloat(), ja.get(1).getAsFloat(), ja.get(2).getAsFloat());
        }

        return new Vector3f();
    }

    private static void flipX(Pair<Vector2f, Vector2f> face) {
        face.left().x += face.right().x;
        face.right().x *= -1;
    }

    private static void flip(Pair<Vector2f, Vector2f> face) {
        face.left().x += face.right().x;
        face.right().x *= -1;
        face.left().y += face.right().y;
        face.right().y *= -1;
    }

    private enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        CLOCKWISE_270;

        public List<Vector2f> rotateUv(List<Vector2f> uvs) {
            return switch (this) {
                case NONE -> uvs;
                case CLOCKWISE_270 -> List.of(uvs.get(3), uvs.get(0), uvs.get(1), uvs.get(2));
                case CLOCKWISE_180 -> List.of(uvs.get(2), uvs.get(3), uvs.get(0), uvs.get(1));
                case CLOCKWISE_90 -> List.of(uvs.get(1), uvs.get(2), uvs.get(3), uvs.get(0));
            };
        }

        public static Rotation fromDegree(int degree) {
            return switch (degree) {
                case 90 -> CLOCKWISE_90;
                case 180 -> CLOCKWISE_180;
                case 270 -> CLOCKWISE_270;
                default -> NONE;
            };
        }
    }

    public static BrCube parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        Vector3f origin = parse(jsonObject, "origin");
        Vector3f size = parse(jsonObject, "size");
        Vector3f rotation = parse(jsonObject, "rotation");
        Vector3f pivot = parse(jsonObject, "pivot");

        float inflate = jsonObject.get("inflate") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;

        // left: u0v0, right: uvSize
        Pair<Vector2f, Vector2f> up;
        Pair<Vector2f, Vector2f> down;
        Pair<Vector2f, Vector2f> north;
        Pair<Vector2f, Vector2f> east;
        Pair<Vector2f, Vector2f> south;
        Pair<Vector2f, Vector2f> west;
        Rotation[] uvRotations = { Rotation.NONE, Rotation.NONE, Rotation.NONE, Rotation.NONE, Rotation.NONE, Rotation.NONE };
        Set<Integer> removeIndexes = new HashSet<>();

        if (jsonObject.get("uv") instanceof JsonArray ja) {
            //  box
            // |   | u | d |   |
            // | w | n | e | s |
            Vector2f uv = new Vector2f(gson.fromJson(ja, float[].class));

            int uvSizeX = (int) size.x;
            int uvSizeY = (int) size.y;
            int uvSizeZ = (int) size.z;

            up = Pair.of(new Vector2f(uv.x + uvSizeZ, uv.y), new Vector2f(uvSizeX, uvSizeZ));
            down = Pair.of(new Vector2f(uv.x + uvSizeX + uvSizeZ + uvSizeX, uv.y), new Vector2f(-uvSizeX, uvSizeZ));
            west = Pair.of(new Vector2f(uv.x, uv.y + uvSizeZ), new Vector2f(uvSizeZ, uvSizeY));
            north = Pair.of(new Vector2f(uv.x + uvSizeZ, uv.y + uvSizeZ), new Vector2f(uvSizeX, uvSizeY));
            east = Pair.of(new Vector2f(uv.x + uvSizeZ + uvSizeX, uv.y + uvSizeZ), new Vector2f(uvSizeZ, uvSizeY));
            south = Pair.of(new Vector2f(uv.x + uvSizeZ + uvSizeX + uvSizeZ, uv.y + uvSizeZ), new Vector2f(uvSizeX, uvSizeY));

            boolean mirror = jsonObject.get("mirror") instanceof JsonPrimitive jp && jp.getAsBoolean();

            if (mirror) {
                for (Pair<Vector2f, Vector2f> face : java.util.List.of(up, down, west, north, east, south)) {
                    flipX(face);
                }
                var t = west;
                west = east;
                east = t;
            }
        } else if (jsonObject.get("uv") instanceof JsonObject jo) {
            //  face
            up = getUVFromFace(jo, "up", 0, removeIndexes, uvRotations);
            down = getUVFromFace(jo, "down", 1, removeIndexes, uvRotations);
            east = getUVFromFace(jo, "west", 2, removeIndexes, uvRotations);
            north = getUVFromFace(jo, "north", 3, removeIndexes, uvRotations);
            west = getUVFromFace(jo, "east", 4, removeIndexes, uvRotations);
            south = getUVFromFace(jo, "south", 5, removeIndexes, uvRotations);
        } else {
            up = zero();
            down = zero();
            north = zero();
            east = zero();
            south = zero();
            west = zero();
        }

        up.left().div(textureWidth);
        down.left().div(textureWidth);
        north.left().div(textureWidth);
        east.left().div(textureWidth);
        south.left().div(textureWidth);
        west.left().div(textureWidth);

        up.right().div(textureHeight);
        down.right().div(textureHeight);
        north.right().div(textureHeight);
        east.right().div(textureHeight);
        south.right().div(textureHeight);
        west.right().div(textureHeight);

        origin.set(-(origin.x + size.x), origin.y, origin.z).div(16);

        Vector3f[] corners = getCorners(origin, size, inflate);

        pivot.div(16).mul(-1, 1, 1);
        rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);

        Matrix4f translate = new Matrix4f()
                .translation(pivot)
                .rotateAffineZYX(rotation.z, rotation.y, rotation.x)
                .translate(-pivot.x, -pivot.y, -pivot.z);

        for (Vector3f corner : corners) {
            corner.mulPosition(translate);
        }

        List<List<Vector3f>> vertexes = ObjectList.of(
                ObjectList.of(corners[6], corners[2], corners[3], corners[7]),
                ObjectList.of(corners[1], corners[5], corners[4], corners[0]),
                ObjectList.of(corners[3], corners[0], corners[4], corners[7]),
                ObjectList.of(corners[2], corners[1], corners[0], corners[3]),
                ObjectList.of(corners[6], corners[5], corners[1], corners[2]),
                ObjectList.of(corners[7], corners[4], corners[5], corners[6])
        );

        var uvs = ObjectList.of(
                uvRotations[0].rotateUv(getUv(up)),
                uvRotations[1].rotateUv(getUv(down)),
                uvRotations[2].rotateUv(getUv(east)),
                uvRotations[3].rotateUv(getUv(north)),
                uvRotations[4].rotateUv(getUv(west)),
                uvRotations[5].rotateUv(getUv(south))
        );

        ObjectList<Vector3f> normals = ObjectList.of(
                getNormal(corners[6], corners[2], corners[3]),
                getNormal(corners[1], corners[5], corners[4]),
                getNormal(corners[3], corners[0], corners[4]),
                getNormal(corners[2], corners[1], corners[0]),
                getNormal(corners[6], corners[5], corners[1]),
                getNormal(corners[7], corners[4], corners[5])
        );

        for (int i = 0; i < 6; i++) {
            if (Float.isNaN(normals.get(i).x) || Float.isNaN(normals.get(i).y) || Float.isNaN(normals.get(i).z)) {
                removeIndexes.add(i);
            }
        }

        if (!removeIndexes.isEmpty()) {
            List<List<Vector3f>> newVertexes = new ObjectArrayList<>();
            ObjectList<List<Vector2f>> newUvs = new ObjectArrayList<>();
            ObjectList<Vector3f> newNormals = new ObjectArrayList<>();

            for (int i = 0; i < 6; i++) {
                if (!removeIndexes.contains(i)) {
                    newVertexes.add(vertexes.get(i));
                    newUvs.add(uvs.get(i));
                    newNormals.add(normals.get(i));
                }
            }

            vertexes = newVertexes;
            uvs = newUvs;
            normals = newNormals;
        }

        return new BrCube(
                normals.size(),
                4,
                vertexes,
                uvs,
                normals
        );
    }

    private static Vector3f getNormal(Vector3f a, Vector3f b, Vector3f c) {
        return b.sub(a, new Vector3f()).cross(c.sub(a, new Vector3f())).normalize();
    }

    private static List<Vector2f> getUv(Pair<Vector2f, Vector2f> uv) {
        var uv1 = uv.right().add(uv.left(), new Vector2f());

        return ObjectList.of(
                uv.left(),
                new Vector2f(uv.left().x, uv1.y),
                uv1,
                new Vector2f(uv1.x, uv.left().y)
        );
    }

    @NotNull
    private static Vector3f[] getCorners(Vector3f origin, Vector3f size, float inflate) {
        final float scalar = 1F / 16F;
        float maxX = origin.x + (size.x + inflate) * scalar;
        float maxY = origin.y + (size.y + inflate) * scalar;
        float maxZ = origin.z + (size.z + inflate) * scalar;
        float minX = origin.x - inflate * scalar;
        float minY = origin.y - inflate * scalar;
        float minZ = origin.z - inflate * scalar;

        return new Vector3f[]{
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ)
        };
    }

    private static Pair<Vector2f, Vector2f> zero() {
        return Pair.of(new Vector2f(), new Vector2f());
    }

    private static Pair<Pair<Vector2f, Vector2f>, Rotation> getUVFromFaceJson(JsonObject uvJson, String face) {
        if (uvJson.get(face) instanceof JsonObject faceJson) {
            return Pair.of(
                    Pair.of(
                            new Vector2f(gson.fromJson(faceJson.get("uv"), float[].class)),
                            faceJson.get("uv_size") instanceof JsonArray ja ? new Vector2f(gson.fromJson(ja, float[].class)) : new Vector2f(1, 1)
                    ),
                    Rotation.fromDegree(faceJson.get("uv_rotation") instanceof JsonPrimitive jp ? jp.getAsInt() : 0)
            );
        } else {
            return null;
        }
    }

    private static Pair<Vector2f, Vector2f> getUVFromFace(JsonObject uvJson, String face, int index, Set<Integer> removeIndexes, Rotation[] uvRotations) {
        var ret = getUVFromFaceJson(uvJson, face);
        if (ret == null) {
            removeIndexes.add(index);
            return zero();
        } else {
            uvRotations[index] = ret.right();
            return ret.left();
        }
    }

}
