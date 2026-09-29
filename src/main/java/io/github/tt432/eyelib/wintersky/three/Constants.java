package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 constants.js 逐字移植（wintersky 用到的常量子集）。
 * 数值与 three.js 保持一致，便于与 JS 端 oracle 对照。
 */
public final class Constants {

    private Constants() {
    }

    // side
    public static final int FrontSide = 0;
    public static final int BackSide = 1;
    public static final int DoubleSide = 2;

    // blending
    public static final int NoBlending = 0;
    public static final int NormalBlending = 1;
    public static final int AdditiveBlending = 2;

    // texture filters
    public static final int NearestFilter = 1003;
    public static final int LinearFilter = 1006;
    public static final int LinearMipmapLinearFilter = 1008;

    // wrapping
    public static final int ClampToEdgeWrapping = 1001;

    // texture format / type / encoding / mapping
    public static final int RGBAFormat = 1023;
    public static final int UnsignedByteType = 1009;
    public static final int LinearEncoding = 3000;
    public static final int UVMapping = 300;

    // buffer attribute usage
    public static final int StaticDrawUsage = 35044;
}
