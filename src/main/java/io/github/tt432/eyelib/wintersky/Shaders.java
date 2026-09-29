package io.github.tt432.eyelib.wintersky;

/**
 * wintersky shader 逐字内容（取 npm dist 1.3.3 内嵌字符串，经 glslify 处理，
 * 含 {@code #define GLSLIFY 1} 前缀；与 git master src/shaders/*.glsl 等价但格式不同）。
 */
public final class Shaders {

    private Shaders() {
    }

    public static final String VERTEX = """
            #define GLSLIFY 1
            attribute vec4 clr;varying vec2 vUv;varying vec4 vColor;void main(){vColor=clr;vUv=uv;vec4 mvPosition=modelViewMatrix*vec4(position,1.0);gl_Position=projectionMatrix*mvPosition;}""";

    public static final String FRAGMENT = """
            #define GLSLIFY 1
            varying vec2 vUv;varying vec4 vColor;uniform sampler2D map;uniform int materialType;void main(void){vec4 tColor=texture2D(map,vUv);if(materialType==0){if(tColor.a<0.5)discard;tColor.a=1.0;}else if(materialType==1){tColor.a=1.0;}else{tColor.a=tColor.a*vColor.a;}gl_FragColor=vec4(tColor.rgb*vColor.rgb,tColor.a);}""";
}
