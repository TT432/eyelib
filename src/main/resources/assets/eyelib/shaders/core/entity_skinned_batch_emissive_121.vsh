#version 150

#moj_import <minecraft:light.glsl>

// C1 跨实体合批 compute 蒙皮（1.21.1）：rendertype_entity_translucent_emissive.vsh 的直通变体。
// 顶点已被 skin_batch.comp 蒙皮；无 fog.glsl/lightmap，vertexDistance = length(MV*pos)，无 normal 输出。
// 与 entity_skinned_compute_emissive_121.vsh 差异仅 TintColor/OverlayUV 由 uniform 改为逐顶点属性。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in vec4 Tint;
in vec2 OverlayUV;

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
    vec4 skinned = vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * skinned;

    vertexDistance = length((ModelViewMat * skinned).xyz);

    vec3 n = Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    vec4 Color = floor(clamp(Tint, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    overlayColor = texelFetch(Sampler1, ivec2(OverlayUV), 0);
    texCoord0 = UV0;
}
