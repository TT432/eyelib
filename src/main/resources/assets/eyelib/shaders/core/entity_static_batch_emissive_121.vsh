#version 430

#moj_import <minecraft:light.glsl>

// C2 静态实体实例化发光变体（ADR-0048，1.21.1）：无 fog/lightmap，实例布局与 1.20.1 版本一致。

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

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    StaticInstance inst = instances[gl_InstanceID];
    vec4 posed = inst.pose * vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * posed;

    vertexDistance = length(posed.xyz);

    vec3 n = mat3(inst.normal) * Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    vec4 Color = floor(clamp(inst.tint, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    overlayColor = texelFetch(Sampler1, ivec2(inst.overlay), 0);
    texCoord0 = UV0;
}
