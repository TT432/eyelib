package io.github.tt432.eyelib.client.model.bbmodel;

import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.With;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
@With
public record BBBone(
        int id,
        int parent,
        List<Model.Cube> cubes,
        Vector3f origin,
        Vector3f rotation,
        GroupLocator locator
) {

    public BBBone(int id, int parentId) {
        this(id, parentId, new ArrayList<>(), new Vector3f(), new Vector3f(), new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>()));
    }

    public Model.Bone createBone() {
        return new Model.Bone(id, parent, origin, rotation, new Vector3f(), new Vector3f(1), null,
                new Int2ObjectOpenHashMap<>(), cubes, locator);
    }
}
