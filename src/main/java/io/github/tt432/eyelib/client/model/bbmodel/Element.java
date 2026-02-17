package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.With;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

@With
public record Element(
        String name,

        @SerializedName("box_uv")
        boolean boxUv,

        @SerializedName("render_order")
        String renderOrder,

        boolean locked,

        @SerializedName("allow_mirror_modeling")
        boolean allowMirrorModeling,

        double[] from,
        double[] to,
        int autouv,
        int color,
        double[] origin,

        @SerializedName("uv_offset")
        double[] uvOffset,

        double inflate,

        Faces faces,
        String type,
        String uuid,
        double[] rotation
) {
    public BbCube createBbCube(List<Texture> textures) {
        Vector3f[] corners = getCorners();

        if (rotation() != null && origin() != null) {
            Vector3f origin = new Vector3f(
                    (float) origin()[0],
                    (float) origin()[1],
                    (float) origin()[2]
            ).div(16);

            Vector3f rotation = new Vector3f(
                    (float) rotation()[0],
                    (float) rotation()[1],
                    (float) rotation()[2]
            ).mul(EyeMath.DEGREES_TO_RADIANS);

            Matrix4f transform = new Matrix4f()
                    .translation(origin)
                    .rotateAffineZYX(rotation.z, rotation.y, rotation.x)
                    .translate(origin.negate(new Vector3f()));

            for (Vector3f corner : corners) {
                corner.mulPosition(transform);
            }
        }

        var LFU = corners[0];
        var RFU = corners[1];
        var RBU = corners[2];
        var LBU = corners[3];
        var LFD = corners[4];
        var RFD = corners[5];
        var RBD = corners[6];
        var LBD = corners[7];

        List<List<Vector3f>> vertexes = ObjectList.of(
                ObjectList.of(LFU, RFU, RBU, LBU), // up
                ObjectList.of(LBD, RBD, RFD, LFD), // down
                ObjectList.of(RBU, RFU, RFD, RBD), // e
                ObjectList.of(RFU, LFU, LFD, RFD), // n
                ObjectList.of(LFU, LBU, LBD, LFD), // w
                ObjectList.of(LBU, RBU, RBD, LBD) // s
        );

        // 4. Normals (Calculated from vertices)
        ObjectList<Vector3f> normals = normals(vertexes);

        // 5. UVs
        ObjectList<List<Vector2f>> uvs = ObjectList.of(
                getUv(4, textures),
                getUv(5, textures),
                getUv(1, textures),
                getUv(0, textures),
                getUv(3, textures),
                getUv(2, textures)
        );

        return new BbCube(6, 4, vertexes, uvs, normals);
    }

    private ObjectList<Vector3f> normals(List<List<Vector3f>> vertexes) {
        return ObjectList.of(
                getNormal(vertexes.get(0).get(0), vertexes.get(0).get(1), vertexes.get(0).get(2)),
                getNormal(vertexes.get(1).get(0), vertexes.get(1).get(1), vertexes.get(1).get(2)),
                getNormal(vertexes.get(2).get(0), vertexes.get(2).get(1), vertexes.get(2).get(2)),
                getNormal(vertexes.get(3).get(0), vertexes.get(3).get(1), vertexes.get(3).get(2)),
                getNormal(vertexes.get(4).get(0), vertexes.get(4).get(1), vertexes.get(4).get(2)),
                getNormal(vertexes.get(5).get(0), vertexes.get(5).get(1), vertexes.get(5).get(2))
        );
    }

    private Vector3f getNormal(Vector3f a, Vector3f b, Vector3f c) {
        return b.sub(a, new Vector3f()).cross(c.sub(a, new Vector3f())).normalize();
    }

    private List<Vector2f> getUv(int faceIndex, List<Texture> textures) {
        Faces faces = faces();
        FaceData faceData = null;
        if (faces != null) {
            faceData = switch (faceIndex) {
                case 0 -> faces.north();
                case 1 -> faces.east();
                case 2 -> faces.south();
                case 3 -> faces.west();
                case 4 -> faces.up();
                case 5 -> faces.down();
                default -> null;
            };
        }

        if (faceData == null || faceData.uv() == null) {
            return ObjectList.of(new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f());
        }

        Texture texture = textures.get(faceData.texture());

        float width = texture.uvWidth();
        float height = texture.uvHeight();
        if (width == 0 || height == 0) {
            return ObjectList.of(new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f());
        }

        double[] uvArray = faceData.uv(); // [u0, v0, u1, v1]
        float u0 = (float) uvArray[0] / width;
        float v0 = (float) uvArray[1] / height;
        float u1 = (float) uvArray[2] / width;
        float v1 = (float) uvArray[3] / height;

        return rotateUv(ObjectList.of(
                new Vector2f(u0, v0), // LU
                new Vector2f(u1, v0), // RU
                new Vector2f(u1, v1), // RD
                new Vector2f(u0, v1)  // LD
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

    private Vector3f[] getCorners() {
        final float scalar = 1F / 16F;
        float maxX = (float) (to()[0] + inflate) * scalar;
        float maxY = (float) (to()[1] + inflate) * scalar;
        float maxZ = (float) (to()[2] + inflate) * scalar;
        float minX = (float) (from()[0] - inflate) * scalar;
        float minY = (float) (from()[1] - inflate) * scalar;
        float minZ = (float) (from()[2] - inflate) * scalar;

        return new Vector3f[]{
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, minY, maxZ),
        };
    }
}
