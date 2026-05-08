#version 150

out vec4 fragColor;

void main() {
    vec3 color = vec3(1.0, 1.0, 1.0);
#ifdef MATERIAL_RED
    color = vec3(1.0, 0.0, 0.0);
#endif
#ifdef MATERIAL_GREEN
    color = vec3(0.0, 1.0, 0.0);
#endif
#ifdef MATERIAL_BLUE
    color = vec3(0.0, 0.0, 1.0);
#endif
#ifdef MATERIAL_YELLOW
    color = vec3(1.0, 1.0, 0.0);
#endif

    float alpha = 1.0;
#ifdef MATERIAL_ALPHA_HALF
    alpha = 0.5;
#endif

    fragColor = vec4(color, alpha);
}
