#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

// C1 GPU 蒙皮 compute 路径（≤26.1，ADR-0032）：rendertype_entity_cutout.vsh 的直通变体。
// 顶点已被 skin.comp 蒙皮（模型空间），此处仅做 MV/Proj 变换、雾化、光照混合；
// 除蒙皮数学外与 entity_skinned_legacy.vsh 逐行一致。

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

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

    vec4 Color = floor(clamp(TintColor, 0.0, 1.0) * 255.0) / 255.0;

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, Color);
    lightMapColor = texelFetch(Sampler2, LightUV / 16, 0);
    overlayColor = texelFetch(Sampler1, OverlayUV, 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(n, 0.0);
}
