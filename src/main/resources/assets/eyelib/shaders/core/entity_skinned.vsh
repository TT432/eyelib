#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

// C1 GPU 蒙皮（ADR-0032）：entity.vsh 的蒙皮变体。
// 顶点为绑定姿态（静态驻留 GPU），骨骼世界矩阵/法线阵经 BonePalette UBO 传入；
// 蒙皮数学与 CPU 路径 BakedBone.transformPos/transformNormal 逐行同构。
// tint/overlay/light 为逐实体常量，随 UBO header 传入（不再占顶点属性）。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in uint BoneIndex;

#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

layout(std140) uniform BonePalette {
    vec4 TintColor;
    ivec2 OverlayUV;
    ivec2 LightUV;
    mat4 BonePose[MAX_BONES];
    mat4 BoneNormal[MAX_BONES];
};

out float sphericalVertexDistance;
out float cylindricalVertexDistance;

#ifdef PER_FACE_LIGHTING
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
#else
out vec4 vertexColor;
#endif

#ifndef EMISSIVE
out vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
out vec4 overlayColor;
#endif

out vec2 texCoord0;

void main() {
    mat4 bonePose = BonePose[BoneIndex];
    vec4 skinned = bonePose * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * skinned;
    sphericalVertexDistance = fog_spherical_distance(skinned.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(skinned.xyz);

    // 法线：mat3 变换 + 归一化（与 CPU 路径 transformNormal 的 lenSq>1e-8 守卫一致；
    // light.glsl 不 normalize，此处必须保证单位长度）
    vec3 n = mat3(BoneNormal[BoneIndex]) * Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    // tint 经与 CPU 路径一致的字节量化（(int)(clamp(x)*255)/255），消除 ULP 级差异
    vec4 Color = floor(clamp(TintColor, 0.0, 1.0) * 255.0) / 255.0;

#ifdef PER_FACE_LIGHTING
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, n);
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, Color);
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);
#elif defined(NO_CARDINAL_LIGHTING)
    vertexColor = Color;
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
#endif
#ifndef EMISSIVE
    lightMapColor = sample_lightmap(Sampler2, LightUV);
#endif
#ifndef NO_OVERLAY
    overlayColor = texelFetch(Sampler1, OverlayUV, 0);
#endif
    texCoord0 = UV0;
#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif
}
