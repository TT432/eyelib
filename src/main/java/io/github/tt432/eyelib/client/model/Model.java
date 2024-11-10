package io.github.tt432.eyelib.client.model;

import io.github.tt432.chin.util.Lists;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public interface Model {
    String name();

    Map<String, ? extends Bone> toplevelBones();

    ModelRuntimeData<?, ?, ?> data();

    ModelLocator locator();

    interface Bone {
        String name();

        Map<String, ? extends Bone> children();

        List<? extends Cube> cubes();
    }

    interface Cube {
        default List<List<Vector3fc>> vertexes() {
            return Lists.asList(faceCount(), i -> Lists.asList(pointsPerFace(), j -> new Vector3f(positionX(i, j), positionY(i, j), positionZ(i, j))));
        }

        default List<List<Vector2fc>> uvs() {
            return Lists.asList(faceCount(), i -> Lists.asList(pointsPerFace(), j -> new Vector2f(uvU(i, j), uvV(i, j))));
        }

        default List<Vector3fc> normals() {
            return Lists.asList(faceCount(), i -> new Vector3f(normalX(i), normalY(i), normalZ(i)));
        }

        interface ConstCube extends Cube {
            @Override
            List<List<Vector3fc>> vertexes();

            @Override
            List<List<Vector2fc>> uvs();

            @Override
            List<Vector3fc> normals();

            @Override
            default float positionX(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).x();
            }

            @Override
            default float positionY(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).y();
            }

            @Override
            default float positionZ(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).z();
            }

            @Override
            default float uvU(int faceIndex, int pointIndex) {
                return uvs().get(faceIndex).get(pointIndex).x();
            }

            @Override
            default float uvV(int faceIndex, int pointIndex) {
                return uvs().get(faceIndex).get(pointIndex).y();
            }

            @Override
            default float normalX(int faceIndex) {
                return normals().get(faceIndex).x();
            }

            @Override
            default float normalY(int faceIndex) {
                return normals().get(faceIndex).y();
            }

            @Override
            default float normalZ(int faceIndex) {
                return normals().get(faceIndex).z();
            }
        }

        int faceCount();

        int pointsPerFace();

        float positionX(int faceIndex, int pointIndex);

        float positionY(int faceIndex, int pointIndex);

        float positionZ(int faceIndex, int pointIndex);

        float uvU(int faceIndex, int pointIndex);

        float uvV(int faceIndex, int pointIndex);

        float normalX(int faceIndex);

        float normalY(int faceIndex);

        float normalZ(int faceIndex);
    }
}
