package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Float32BufferAttribute 移植：接受 double 数组，按 float32 截断存储。
 */
public class Float32BufferAttribute extends BufferAttribute {

    public Float32BufferAttribute(double[] array, int itemSize) {
        super(new Float32Array(array), itemSize);
    }
}
