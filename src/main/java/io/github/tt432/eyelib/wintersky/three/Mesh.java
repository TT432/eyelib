package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Mesh 移植（构造与字段子集）。
 */
public class Mesh extends Object3D {

    public BufferGeometry geometry;
    public Material material;

    public Mesh() {
        this(new BufferGeometry(), new Material());
    }

    public Mesh(BufferGeometry geometry, Material material) {
        super();

        this.type = "Mesh";
        this.isMesh = true;

        this.geometry = geometry;
        this.material = material;
    }
}
