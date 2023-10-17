package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.util.EyeMath;
import it.unimi.dsi.fastutil.Pair;
import org.joml.*;

import java.util.Arrays;

/**
 * @author TT432
 */
public record BrCube(
        BrFace[] faces
) {

    private static final Gson gson = new Gson();

    public static BrCube parse(int textureHeight, int textureWidth, JsonObject jsonObject) {
        Vector3f origin = jsonObject.get("origin") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        Vector3f size = jsonObject.get("size") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f();
        Vector3f rotation = jsonObject.get("rotation") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
        Vector3f pivot = jsonObject.get("pivot") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);

        float inflate = jsonObject.get("inflate") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;

        // left: u0v0, right: uvSize
        Pair<Vector2f, Vector2f> up;
        Pair<Vector2f, Vector2f> down;
        Pair<Vector2f, Vector2f> north;
        Pair<Vector2f, Vector2f> east;
        Pair<Vector2f, Vector2f> south;
        Pair<Vector2f, Vector2f> west;

        if (jsonObject.get("uv") instanceof JsonArray ja) {
            //  box
            // |   | u | d |   |
            // | w | n | e | s |
            Vector2f uv = new Vector2f(gson.fromJson(ja, float[].class));

            int uvSizeX = (int) size.x;
            int uvSizeY = (int) size.y;
            int uvSizeZ = (int) size.z;

            up = Pair.of(new Vector2f(uv.x + uvSizeZ, uv.y), new Vector2f(uvSizeX, uvSizeZ));
            down = Pair.of(new Vector2f(uv.x + uvSizeX + uvSizeZ, uv.y), new Vector2f(uvSizeX, uvSizeZ));
            west = Pair.of(new Vector2f(uv.x, uv.y + uvSizeZ), new Vector2f(uvSizeZ, uvSizeY));
            north = Pair.of(new Vector2f(uv.x + uvSizeZ, uv.y + uvSizeZ), new Vector2f(uvSizeX, uvSizeY));
            east = Pair.of(new Vector2f(uv.x + uvSizeZ + uvSizeX, uv.y + uvSizeZ), new Vector2f(uvSizeZ, uvSizeY));
            south = Pair.of(new Vector2f(uv.x + uvSizeZ + uvSizeX + uvSizeZ, uv.y + uvSizeZ), new Vector2f(uvSizeX, uvSizeY));

            boolean mirror = jsonObject.get("mirror") instanceof JsonPrimitive jp && jp.getAsBoolean();

            if (mirror) {
                var t = west;
                west = east;
                east = t;
            }
        } else if (jsonObject.get("uv") instanceof JsonObject jo) {
            //  face
            up = getUVFromFace(jo, "up");
            down = getUVFromFace(jo, "down");
            north = getUVFromFace(jo, "north");
            east = getUVFromFace(jo, "east");
            south = getUVFromFace(jo, "south");
            west = getUVFromFace(jo, "west");
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

        origin
                .set(-(origin.x + size.x), origin.y, origin.z)
                .div(16);

        float maxX = origin.x + (size.x + inflate) / 16;
        float maxY = origin.y + (size.y + inflate) / 16;
        float maxZ = origin.z + (size.z + inflate) / 16;

        float minX = origin.x - inflate / 16;
        float minY = origin.y - inflate / 16;
        float minZ = origin.z - inflate / 16;

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ)
        };

        pivot.div(16).mul(-1, 1, 1);
        rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);

        Matrix4f translate = new Matrix4f()
                .translation(pivot)
                .rotate(new Quaternionf().rotationZYX(rotation.z, rotation.y, rotation.x))
                .translate(pivot.negate(new Vector3f()));

        corners = Arrays.stream(corners)
                .map(v -> translate.transformAffine(new Vector4f(v, 1)))
                .map(v4 -> new Vector3f(v4.x, v4.y, v4.z).div(v4.w))
                .toArray(Vector3f[]::new);

        Vector3f[][] faces = new Vector3f[][]{
                {corners[6], corners[2], corners[3], corners[7],},
                {corners[0], corners[1], corners[5], corners[4],},
                {corners[3], corners[0], corners[4], corners[7],},
                {corners[2], corners[1], corners[0], corners[3],},
                {corners[6], corners[5], corners[1], corners[2],},
                {corners[7], corners[4], corners[5], corners[6],}
        };

        return new BrCube(new BrFace[]{
                new BrFace(new Vector3f(0, 1, 0), up, faces[0]),
                new BrFace(new Vector3f(0, -1, 0), down, faces[1]),
                new BrFace(new Vector3f(1, 0, 0), east, faces[2]),
                new BrFace(new Vector3f(0, 0, -1), north, faces[3]),
                new BrFace(new Vector3f(-1, 0, 0), west, faces[4]),
                new BrFace(new Vector3f(0, 0, 1), south, faces[5])
        });
    }

    private static Pair<Vector2f, Vector2f> zero() {
        return Pair.of(new Vector2f(), new Vector2f());
    }

    private static Pair<Vector2f, Vector2f> getUVFromFace(JsonObject uvJson, String face) {
        JsonObject faceJson = uvJson.get(face).getAsJsonObject();
        return Pair.of(
                new Vector2f(gson.fromJson(faceJson.get("uv"), float[].class)),
                new Vector2f(gson.fromJson(faceJson.get("uv_size"), float[].class))
        );
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BrCube bc && Arrays.equals(bc.faces, faces);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(faces);
    }

    @Override
    public String toString() {
        return "BrCube { faces: " + Arrays.toString(faces) + " }";
    }
}
