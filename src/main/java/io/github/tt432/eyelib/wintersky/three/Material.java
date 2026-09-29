package io.github.tt432.eyelib.wintersky.three;

import java.util.HashMap;
import java.util.Map;

/**
 * three.js r134 Material 移植（构造默认字段 + setValues 语义）。
 *
 * <p>混合/深度/模板等字段仅作状态记录，本移植层不含 GL 后端。
 */
public class Material extends EventDispatcher {

    private static int materialId = 0;

    public final int id = materialId++;

    public String name = "";
    public String type = "Material";

    public boolean fog = true;

    public int blending = Constants.NormalBlending;
    public int side = Constants.FrontSide;
    public boolean vertexColors = false;

    public double opacity = 1;
    public boolean transparent = false;

    public int blendSrc = 204 /* SrcAlphaFactor */;
    public int blendDst = 205 /* OneMinusSrcAlphaFactor */;
    public int blendEquation = 100 /* AddEquation */;
    public Integer blendSrcAlpha = null;
    public Integer blendDstAlpha = null;
    public Integer blendEquationAlpha = null;

    public int depthFunc = 3 /* LessEqualDepth */;
    public boolean depthTest = true;
    public boolean depthWrite = true;

    public int stencilWriteMask = 0xff;
    public int stencilFunc = 519 /* AlwaysStencilFunc */;
    public int stencilRef = 0;
    public int stencilFuncMask = 0xff;
    public int stencilFail = 7680 /* KeepStencilOp */;
    public int stencilZFail = 7680;
    public int stencilZPass = 7680;
    public boolean stencilWrite = false;

    public boolean colorWrite = true;

    public String precision = null;

    public boolean polygonOffset = false;
    public double polygonOffsetFactor = 0;
    public double polygonOffsetUnits = 0;

    public boolean dithering = false;

    public boolean alphaToCoverage = false;
    public boolean premultipliedAlpha = false;

    public boolean visible = true;

    public boolean toneMapped = true;

    public final Map<String, Object> userData = new HashMap<>();

    public int version = 0;

    private double _alphaTest = 0;

    public double getAlphaTest() {
        return _alphaTest;
    }

    /** JS {@code set alphaTest(value)}：0 与非 0 之间切换时 version++。 */
    public void setAlphaTest(double value) {
        if ((this._alphaTest > 0) != (value > 0)) {
            this.version++;
        }
        this._alphaTest = value;
    }

    /**
     * three setValues(values) 移植：按 key 分发到已知字段，未知 key 打印警告（同 three）。
     * 支持的 key 为 wintersky 实际传入的子集。
     */
    public void setValues(Map<String, Object> values) {
        if (values == null) return;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();

            if (newValue == null) {
                System.err.println("THREE.Material: '" + key + "' parameter is undefined.");
                continue;
            }

            switch (key) {
                case "uniforms", "defines", "vertexShader", "fragmentShader",
                     "linewidth", "wireframe", "wireframeLinewidth", "glslVersion",
                     "index0AttributeName", "uniformsNeedUpdate" -> {
                    // ShaderMaterial 覆盖处理
                    setShaderValue(key, newValue);
                }
                case "vertexColors" -> this.vertexColors = (Boolean) newValue;
                case "transparent" -> this.transparent = (Boolean) newValue;
                case "alphaTest" -> setAlphaTest(((Number) newValue).doubleValue());
                case "side" -> this.side = ((Number) newValue).intValue();
                case "blending" -> this.blending = ((Number) newValue).intValue();
                case "opacity" -> this.opacity = ((Number) newValue).doubleValue();
                case "visible" -> this.visible = (Boolean) newValue;
                case "fog" -> this.fog = (Boolean) newValue;
                case "depthTest" -> this.depthTest = (Boolean) newValue;
                case "depthWrite" -> this.depthWrite = (Boolean) newValue;
                default -> System.err.println("THREE." + this.type + ": '" + key + "' is not a property of this material.");
            }
        }
    }

    /** ShaderMaterial 专属字段分发；基类中按未知属性警告（同 three 行为）。 */
    protected void setShaderValue(String key, Object newValue) {
        System.err.println("THREE." + this.type + ": '" + key + "' is not a property of this material.");
    }

    /** three dispose()：仅派发 dispose 事件。 */
    public void dispose() {
        this.dispatchEvent(new Event("dispose"));
    }
}
