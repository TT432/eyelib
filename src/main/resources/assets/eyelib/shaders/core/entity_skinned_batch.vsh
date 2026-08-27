#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

// C1 跨实体合批 compute 蒙皮（1.20.1，ADR-0032）：rendertype_entity_cutout.vsh 的直通变体。
// 顶点已被 skin_batch.comp 蒙皮（模型空间），此处仅做 MV/Proj 变换、雾化、光照混合。
// 与 entity_skinned_compute.vsh 的差异：TintColor/OverlayUV/LightUV 由逐实体 uniform 改为
// 逐顶点属性（合批绘制内逐实体差异由顶点携带）。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in vec4 Tint;
in vec2 LightUV;
in vec2 OverlayUV;

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
    vec4 skinned = vec4(Position, 1.0);
    gl_Position = ProjMat * ModelViewMat * skinned;

    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * skinned.xyz, FogShape);

    vec3 n = Normal;
    float lenSq = dot(n, n);
    if (lenSq > 1.0E-8) {
        n *= inversesqrt(lenSq);
    }

    vec4 Color = floor(clamp(Tint, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    lightMapColor = texelFetch(Sampler2, ivec2(LightUV) / 16, 0);
    overlayColor = texelFetch(Sampler1, ivec2(OverlayUV), 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(n, 0.0);
}
