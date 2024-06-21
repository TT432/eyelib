package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.Jomls;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;

/**
 * @author TT432
 */
public record BrCube(
        BrFace[] faces
) {

    private static final Gson gson = new Gson();

    private static Vector3f parse(JsonObject object, String ele) {
        if (object.get(ele) instanceof JsonArray ja) {
            return new Vector3f(ja.get(0).getAsFloat(), ja.get(1).getAsFloat(), ja.get(2).getAsFloat());
        }

        return new Vector3f();
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
                for (Pair<Vector2f, Vector2f> face : List.of(up, down, west, north, east, south)) {
                    face.left().x += face.right().x;
                    face.right().x *= -1;
                }
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

        Vector3f[][] faces = new Vector3f[][]{
                {corners[6], corners[2], corners[3], corners[7],},
                {corners[4], corners[0], corners[1], corners[5],},
                {corners[3], corners[0], corners[4], corners[7],},
                {corners[2], corners[1], corners[0], corners[3],},
                {corners[6], corners[5], corners[1], corners[2],},
                {corners[7], corners[4], corners[5], corners[6],}
        };

        return new BrCube(new BrFace[]{
                new BrFace(Jomls.from(Direction.UP.step()), up, faces[0]),
                new BrFace(Jomls.from(Direction.DOWN.step()), down, faces[1]),
                new BrFace(Jomls.from(Direction.EAST.step()), east, faces[2]),
                new BrFace(Jomls.from(Direction.NORTH.step()), north, faces[3]),
                new BrFace(Jomls.from(Direction.WEST.step()), west, faces[4]),
                new BrFace(Jomls.from(Direction.SOUTH.step()), south, faces[5])
        });
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

    private static Pair<Vector2f, Vector2f> getUVFromFace(JsonObject uvJson, String face) {
        if (uvJson.get(face) instanceof JsonObject faceJson) {
            return Pair.of(
                    new Vector2f(gson.fromJson(faceJson.get("uv"), float[].class)),
                    new Vector2f(gson.fromJson(faceJson.get("uv_size"), float[].class))
            );
        } else {
            return Pair.of(new Vector2f(), new Vector2f());
        }
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
