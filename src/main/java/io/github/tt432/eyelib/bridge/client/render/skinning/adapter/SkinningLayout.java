package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

/**
 * 骨骼调色板 UBO 的 std140 布局常量与纯计算（无 GL 依赖，三版本可编译）。
 *
 * <p>布局（单位字节）：
 * <pre>
 *   0  vec4  TintColor
 *   16 ivec2 OverlayUV
 *   24 ivec2 LightUV
 *   32 mat4  BonePose[boneCount]    （连续，stride 64）
 *   .. mat4  BoneNormal[boneCount]  （上左 3×3 为法线阵，余为 0）
 * </pre>
 * VS 端数组上限由着色器 define MAX_BONES 固定；block 尺寸按模型实际骨骼数收缩。
 */
final class SkinningLayout {
    /** VS/FS 编译期常量（withShaderDefine）。96 骨骼 → block 12320B < GL 保证的 16384B 下限。 */
    static final int MAX_BONES = 96;
    static final int HEADER_SIZE = 32;
    static final int MAT4_SIZE = 64;

    private SkinningLayout() {
    }

    static boolean skinnable(int boneCount) {
        return boneCount > 0 && boneCount <= MAX_BONES;
    }

    static int blockSize(int boneCount) {
        return HEADER_SIZE + 2 * MAT4_SIZE * boneCount;
    }

    static int poseOffset(int slot) {
        return HEADER_SIZE + slot * MAT4_SIZE;
    }

    static int normalArrayOffset(int boneCount) {
        return HEADER_SIZE + MAT4_SIZE * boneCount;
    }

    static int normalOffset(int slot, int boneCount) {
        return normalArrayOffset(boneCount) + slot * MAT4_SIZE;
    }
}
