#version 150

#ifndef X_CENTER
#define X_CENTER 0.0
#endif

#ifndef Y_CENTER
#define Y_CENTER 0.0
#endif

#ifndef HALF_WIDTH
#define HALF_WIDTH 0.22
#endif

#ifndef HALF_HEIGHT
#define HALF_HEIGHT 0.22
#endif

const vec2 POSITIONS[6] = vec2[6](
    vec2(X_CENTER - HALF_WIDTH, Y_CENTER - HALF_HEIGHT),
    vec2(X_CENTER + HALF_WIDTH, Y_CENTER - HALF_HEIGHT),
    vec2(X_CENTER + HALF_WIDTH, Y_CENTER + HALF_HEIGHT),
    vec2(X_CENTER - HALF_WIDTH, Y_CENTER - HALF_HEIGHT),
    vec2(X_CENTER + HALF_WIDTH, Y_CENTER + HALF_HEIGHT),
    vec2(X_CENTER - HALF_WIDTH, Y_CENTER + HALF_HEIGHT)
);

void main() {
    gl_Position = vec4(POSITIONS[gl_VertexID], 0.0, 1.0);
}
