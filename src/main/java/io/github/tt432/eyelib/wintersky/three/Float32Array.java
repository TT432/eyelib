package io.github.tt432.eyelib.wintersky.three;

/**
 * JS {@code Float32Array} 的最小复刻：float32 精度存储 + {@code set()} 批量写入。
 *
 * <p>three.js 的 {@code BufferAttribute.array} 是 {@code Float32Array}，
 * 写入其中的 double 会被截断为 float32。为保证与 JS 端逐位一致，
 * 移植版用 {@code float[]} 承载属性数据。
 */
public final class Float32Array {

    public final float[] data;

    public Float32Array(int length) {
        this.data = new float[length];
    }

    public Float32Array(double[] values) {
        this.data = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            this.data[i] = (float) values[i];
        }
    }

    public Float32Array(float[] values) {
        this.data = values;
    }

    public int length() {
        return data.length;
    }

    public float get(int index) {
        return data[index];
    }

    /** JS {@code typedArray.set(array)}：从 0 偏移整体写入。 */
    public void set(float[] values) {
        set(values, 0);
    }

    /** JS {@code typedArray.set(array, offset)}。 */
    public void set(float[] values, int offset) {
        System.arraycopy(values, 0, data, offset, values.length);
    }

    /** JS {@code typedArray.set([...])}：double 值写入时按 float32 截断。 */
    public void set(double[] values) {
        for (int i = 0; i < values.length; i++) {
            data[i] = (float) values[i];
        }
    }
    /** JS {@code typedArray.fill(1)}。 */
    public Float32Array fill(float value) {
        java.util.Arrays.fill(data, value);
        return this;
    }

    /** JS {@code new Float32Array(otherArray)}：拷贝构造。 */
    public Float32Array copy() {
        return new Float32Array(data.clone());
    }
}
