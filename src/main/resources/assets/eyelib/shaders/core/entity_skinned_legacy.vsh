#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

// C1 GPU 蒙皮（≤26.1，ADR-0032）：rendertype_entity_cutout.vsh 的蒙皮变体。
// 顶点为绑定姿态（静态驻留 GPU），骨骼世界矩阵/法线阵经 BonePose/BoneNormal uniform 数组传入；
// 蒙皮数学与 CPU 路径 BakedBone.transformPos/transformNormal 逐行同构。
// tint/overlay/light 为逐实体 uniform（不再占顶点属性）。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in ivec2 BoneIndex;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform vec4 TintColor;
uniform ivec2 OverlayUV;
uniform ivec2 LightUV;
// 与 Java 侧 SkinningLayout.MAX_BONES 保持一致（1.20.1 核心着色器无 define 机制，只能硬编码）
uniform mat4 BonePose[96];
uniform mat4 BoneNormal[96];

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    int bone = BoneIndex.x;
    vec4 skinned = BonePose[bone] * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * skinned;

    // 与 vanilla 一致：fog_distance(ModelViewMat, IViewRotMat * Position, FogShape)，Position=蒙皮后实体空间坐标
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * skinned.xyz, FogShape);

    // 法线：mat3 变换 + 归一化（与 CPU 路径 transformNormal 的 lenSq>1e-8 守卫一致）
    vec3 n = mat3(BoneNormal[bone]) * Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    // tint 经与 CPU 路径一致的字节量化（(int)(clamp(x)*255)/255），消除 ULP 级差异
    vec4 Color = floor(clamp(TintColor, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    lightMapColor = texelFetch(Sampler2, LightUV / 16, 0);
    overlayColor = texelFetch(Sampler1, OverlayUV, 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(n, 0.0);
}
