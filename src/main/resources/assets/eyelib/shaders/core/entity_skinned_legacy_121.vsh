#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

// C1 GPU 蒙皮（1.21.1，ADR-0032）：rendertype_entity_cutout.vsh（1.21.1 版）的蒙皮变体。
// 与 1.20.1 版差异（vanilla 同步变更）：fog_distance(pos, shape) 2 参签名、无 IViewRotMat、无 normal 输出。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in ivec2 BoneIndex;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform vec4 TintColor;
uniform ivec2 OverlayUV;
uniform ivec2 LightUV;
// 与 Java 侧 SkinningLayout.MAX_BONES 保持一致（核心着色器无 define 机制，只能硬编码）
uniform mat4 BonePose[96];
uniform mat4 BoneNormal[96];

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    int bone = BoneIndex.x;
    vec4 skinned = BonePose[bone] * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * skinned;

    vertexDistance = fog_distance(skinned.xyz, FogShape);

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
}
