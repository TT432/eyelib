package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 BufferAttribute 移植。
 *
 * <p>{@code array} 为 {@link Float32Array}（float32 精度，与 JS TypedArray 一致）。
 * {@code needsUpdate = true} 语义移植为 {@link #setNeedsUpdate(boolean)}（version 自增）。
 */
public class BufferAttribute {

    public String name = "";
    public Float32Array array;
    public final int itemSize;
    public final int count;
    public boolean normalized;

    public int usage = Constants.StaticDrawUsage;
    public int version = 0;

    public BufferAttribute(Float32Array array, int itemSize) {
        this(array, itemSize, false);
    }

    public BufferAttribute(Float32Array array, int itemSize, boolean normalized) {
        this.array = array;
        this.itemSize = itemSize;
        this.count = array.length() / itemSize;
        this.normalized = normalized;
    }

    /** JS {@code set needsUpdate(value)}：value === true 时 version++。 */
    public void setNeedsUpdate(boolean value) {
        if (value) this.version++;
    }

    public BufferAttribute setUsage(int value) {
        this.usage = value;
        return this;
    }

    public float getX(int index) {
        return this.array.get(index * this.itemSize);
    }

    public float getY(int index) {
        return this.array.get(index * this.itemSize + 1);
    }

    public float getZ(int index) {
        return this.array.get(index * this.itemSize + 2);
    }

    public float getW(int index) {
        return this.array.get(index * this.itemSize + 3);
    }
}
