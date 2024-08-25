package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import org.joml.Vector2fc;
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
        List<List<Vector3fc>> vertexes();

        List<List<Vector2fc>> uvs();

        List<Vector3fc> normals();
    }
}
