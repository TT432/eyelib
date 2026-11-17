#version 430

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

// C2 静态实体实例化（ADR-0048，1.20.1）：非动画 ClientEntity 的摊平几何 + 逐实例 SSBO。
// 顶点为模型空间（bind 骨骼链已烘入），实例 root 位姿/法线阵/tint/light/overlay 由 StaticInstances
// 提供（binding 4）。光照、UV 与 tint 量化与 entity_skinned_batch.vsh 逐行一致。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

struct StaticInstance {
    mat4 pose;
    mat4 normal;
    vec4 tint;
    vec2 light;
    vec2 overlay;
};

layout(std430, binding = 4) readonly buffer StaticInstances {
    StaticInstance instances[];
};

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    StaticInstance inst = instances[gl_InstanceID];
    vec4 posed = inst.pose * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * posed;

    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * posed.xyz, FogShape);

    vec3 n = mat3(inst.normal) * Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    vec4 Color = floor(clamp(inst.tint, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    lightMapColor = texelFetch(Sampler2, ivec2(inst.light) / 16, 0);
    overlayColor = texelFetch(Sampler1, ivec2(inst.overlay), 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(n, 0.0);
}
