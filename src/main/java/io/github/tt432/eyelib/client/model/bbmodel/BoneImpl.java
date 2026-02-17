package io.github.tt432.eyelib.client.model.bbmodel;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public class BoneImpl implements Model.Bone<BoneImpl> {
    private final int id;
    private final int parentId;
    private final Int2ObjectMap<BoneImpl> children = new Int2ObjectOpenHashMap<>();
    private final List<BbCube> cubes = new ArrayList<>();
    private final Vector3f origin = new Vector3f();
    private final Vector3f rotation = new Vector3f();

    public BoneImpl(int id, int parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public Vector3f origin() {
        return origin;
    }

    public Vector3f rotation() {
        return rotation;
    }

    public void addChild(BoneImpl child) {
        children.put(child.id(), child);
    }

    public void addCube(BbCube cube) {
        cubes.add(cube);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int parent() {
        return parentId;
    }

    @Override
    public MolangValue binding() {
        return MolangValue.FALSE_VALUE;
    }

    @Override
    public Int2ObjectMap<BoneImpl> children() {
        return children;
    }

    @Override
    public List<? extends Model.Cube> cubes() {
        return cubes;
    }
}
