package io.github.tt432.eyelib.wintersky.three;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * three.js r134 ShaderMaterial 移植。
 *
 * <p>uniforms 条目对应 JS 的 {@code {type, value}} 普通对象，见 {@link Uniform}。
 */
public class ShaderMaterial extends Material {

    /** three uniforms 条目：{@code {type: 't', value: ...}}。 */
    public static final class Uniform {
        public String type;
        public Object value;

        public Uniform(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public final Map<String, Object> defines = new LinkedHashMap<>();
    public final Map<String, Uniform> uniforms = new LinkedHashMap<>();

    public String vertexShader;
    public String fragmentShader;

    public double linewidth = 1;

    public boolean wireframe = false;
    public double wireframeLinewidth = 1;

    public String glslVersion = null;

    public String index0AttributeName = null;
    public boolean uniformsNeedUpdate = false;

    public ShaderMaterial() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public ShaderMaterial(Map<String, Object> parameters) {
        super();

        this.type = "ShaderMaterial";

        this.fog = false;

        if (parameters != null) {
            // three 对 attributes 参数报错；wintersky 不传，保持 setValues 语义
            this.setValues(parameters);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setShaderValue(String key, Object newValue) {
        switch (key) {
            case "uniforms" -> {
                this.uniforms.clear();
                ((Map<String, Uniform>) newValue).forEach(this.uniforms::put);
            }
            case "defines" -> {
                this.defines.clear();
                ((Map<String, Object>) newValue).forEach(this.defines::put);
            }
            case "vertexShader" -> this.vertexShader = (String) newValue;
            case "fragmentShader" -> this.fragmentShader = (String) newValue;
            case "linewidth" -> this.linewidth = ((Number) newValue).doubleValue();
            case "wireframe" -> this.wireframe = (Boolean) newValue;
            case "wireframeLinewidth" -> this.wireframeLinewidth = ((Number) newValue).doubleValue();
            case "glslVersion" -> this.glslVersion = (String) newValue;
            case "index0AttributeName" -> this.index0AttributeName = (String) newValue;
            case "uniformsNeedUpdate" -> this.uniformsNeedUpdate = (Boolean) newValue;
            default -> System.err.println("THREE." + this.type + ": '" + key + "' is not a property of this material.");
        }
    }
}
