package io.github.tt432.eyelib.client.model.bbmodel;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.With;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
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

        Vector3f from,
        Vector3f to,
        int autouv,
        int color,
        Vector3f origin,

        @SerializedName("uv_offset")
        Vector2f uvOffset,

        double inflate,

        Faces faces,
        String type,
        String uuid,
        Vector3f rotation
) {
    public static final Codec<Element> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(Element::name),
            Codec.BOOL.fieldOf("box_uv").forGetter(Element::boxUv),
            Codec.STRING.fieldOf("render_order").forGetter(Element::renderOrder),
            Codec.BOOL.fieldOf("locked").forGetter(Element::locked),
            Codec.BOOL.fieldOf("allow_mirror_modeling").forGetter(Element::allowMirrorModeling),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("from").forGetter(Element::from),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("to").forGetter(Element::to),
            Codec.INT.fieldOf("autouv").forGetter(Element::autouv),
            Codec.INT.fieldOf("color").forGetter(Element::color),
            EyelibCodec.FLOATS2VEC3F_CODEC.fieldOf("origin").forGetter(Element::origin),
            EyelibCodec.FLOATS2VEC2F_CODEC.optionalFieldOf("uv_offset", new Vector2f()).forGetter(Element::uvOffset),
            Codec.DOUBLE.optionalFieldOf("inflate", 0D).forGetter(Element::inflate),
            Faces.CODEC.fieldOf("faces").forGetter(Element::faces),
            Codec.STRING.fieldOf("type").forGetter(Element::type),
            Codec.STRING.fieldOf("uuid").forGetter(Element::uuid),
            EyelibCodec.FLOATS2VEC3F_CODEC.optionalFieldOf("rotation", new Vector3f()).forGetter(Element::rotation)
    ).apply(ins, Element::new));

    @Nullable
    public Model.Cube createBbCube(int textureIndex, List<Texture> textures) {
        Vector3f[] corners = getCorners();

        if (rotation() != null && origin() != null) {
            Vector3f origin = new Vector3f(origin()).div(16);
            Vector3f rotation = new Vector3f(rotation()).mul(EyeMath.DEGREES_TO_RADIANS);

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
                getUv(4, textureIndex, textures),
                getUv(5, textureIndex, textures),
                getUv(1, textureIndex, textures),
                getUv(0, textureIndex, textures),
                getUv(3, textureIndex, textures),
                getUv(2, textureIndex, textures)
        );

        List<Model.Face> faces = new ArrayList<>();

        for (int i = 0; i < vertexes.size(); i++) {
            if (uvs.get(i) != null) {
                List<Model.Vertex> vertices = new ArrayList<>();

                for (int j = 0; j < vertexes.get(i).size(); j++) {
                    vertices.add(new Model.Vertex(vertexes.get(i).get(j), uvs.get(i).get(j), normals.get(i)));
                }

                faces.add(new Model.Face(vertices, normals.get(i)));
            }
        }

        if (faces.isEmpty()) {
            return null;
        } else {
            return new Model.Cube(faces);
        }
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

    @Nullable
    private List<Vector2f> getUv(int faceIndex, int textureIndex, List<Texture> textures) {
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
            return null;
        }

        float width;
        float height;

        if (faceData.texture() == textureIndex) {
            Texture texture = textures.get(faceData.texture());

            width = texture.imageWidth();
            height = texture.imageHeight();
        } else {
            return null;
        }

        if (width == 0 || height == 0) {
            return null;
        }

        float u0 = faceData.uv().x / width;
        float v0 = faceData.uv().y / height;
        float u1 = faceData.uv().z / width;
        float v1 = faceData.uv().w / height;

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
        float maxX = (float) (to.x + inflate) * scalar;
        float maxY = (float) (to.y + inflate) * scalar;
        float maxZ = (float) (to.z + inflate) * scalar;
        float minX = (float) (from.x - inflate) * scalar;
        float minY = (float) (from.y - inflate) * scalar;
        float minZ = (float) (from.z - inflate) * scalar;

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
