#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

// C2 静态实体实例化（ADR-0048，26.1.2）：非动画 ClientEntity 的摊平几何 + 逐 draw `InstanceTransform` UBO。
// 顶点为模型空间（bind 骨骼链已烘入）；实例 root 位姿/法线阵/tint/light/overlay 由 UBO 提供，
// 光照、UV 与 tint 量化与 entity_skinned.vsh 逐行一致。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

layout(std140) uniform InstanceTransform {
    vec4 TintColor;
    ivec2 OverlayUV;
    ivec2 LightUV;
    mat4 InstancePose;
    mat4 InstanceNormal;
};

#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

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
    vec4 posed = InstancePose * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * posed;
    sphericalVertexDistance = fog_spherical_distance(posed.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(posed.xyz);

    // 法线：mat3 变换 + 归一化（与 CPU 路径 transformNormal 的 lenSq>1e-8 守卫一致）
    vec3 n = mat3(InstanceNormal) * Normal;
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
