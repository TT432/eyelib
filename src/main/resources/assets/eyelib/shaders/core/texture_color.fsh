#version 150

uniform sampler2D Sampler0;
uniform float AlphaCutoff;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < AlphaCutoff) discard;
    fragColor = color;
}
