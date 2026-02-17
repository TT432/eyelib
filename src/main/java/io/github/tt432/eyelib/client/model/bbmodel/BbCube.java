package io.github.tt432.eyelib.client.model.bbmodel;

import io.github.tt432.eyelib.client.model.Model;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/**
 * @author TT432
 */
public record BbCube(
        int faceCount,
        int pointsPerFace,
        List<List<Vector3f>> vertexes,
        List<List<Vector2f>> uvs,
        List<Vector3f> normals
) implements Model.Cube.ConstCube {

}
