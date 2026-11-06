//? if >=26.1 {
package io.github.tt432.eyelib.bridge.client.compat.iris;

import io.github.tt432.eyelib.material.render.BrRenderState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Iris 26.1 起的 {@code RenderPipeline} 程序匹配桥。
 *
 * <p>1.21.x 的 Iris 通过 {@code GameRenderer.getRendertypeEntityCutoutShader()} 一类 getter 接管所有
 * vanilla {@code ShaderInstance}，所以模组自定义 {@code RenderType} 只要复用 vanilla shader 就自动兼容。
 * 26.1 改成 {@code RenderPipeline} 对象身份匹配（{@code IrisPipelines} 的
 * {@code Object2ObjectArrayMap<RenderPipeline, ShaderKey>}）：只有 vanilla {@code RenderPipelines.*}
 * 常量与经 {@code IrisApi.assignPipeline} 显式登记的 pipeline 才会被替换成光影包程序。未登记的
 * 自定义 pipeline 走 {@code GlDevice.getOrCompilePipeline} 的原版程序分支，日志报
 * {@code Missing program <loc> in override list ... could lead to weird rendering}，
 * 表现是光影打开后这些材质件不写 G-buffer：整片发黑、不投影、近景大字面变黑色平面。
 *
 * <p>对策：构建 pipeline 时登记主通道与阴影通道程序。反射调用，无编译期依赖；Iris/Oculus 不在时静默跳过。
 *
 * <p>登记走 Iris 内部的 {@code IrisPipelines.assignPipeline(RenderPipeline, ShaderKey)} 直接指定键，
 * 不经官方 {@code IrisApi.assignPipeline}：后者内部调 {@code ShaderKey.findBestMatch}，其「okay」分支按
 * 枚举声明顺序返回<b>同 program 的第一个键</b>，而 {@code ENTITIES_ALPHA}（{@code AlphaTests.VERTEX_ALPHA}
 * = {@code NEVER}/{@code MAX_VALUE}）声明在 {@code ENTITIES_SOLID} 之前。于是任何不带 {@code ALPHA_CUTOUT}
 * define 的不透明实体 pipeline 都会被匹配成顶点 alpha 测试 → 片元全被丢弃 → 光影开启后不透明材质实体
 * 整体不渲染（阴影通道单独绘制，故只剩影子）。cospack 用材质 {@code entity}（无 defines/states →
 * SOLID）的鸡/猪/牛/哞菇即此形态（ADR-0041）。{@code ALPHA_CUTOUT} 存在时才会命中
 * {@code ENTITIES_CUTOUT}，所以同包内 cutout/translucent 材质表现正常。
 *
 * <p>精确键不可用时（Iris 内部类改名/缺失）退回 {@code IrisApi} 路径，保持旧行为不崩。
 *
 * @author TT432
 */
public interface IrisPipelineCompat {
    Logger LOGGER = LoggerFactory.getLogger(IrisPipelineCompat.class);

    /** {@code IrisApi} 实例；Iris 未安装时为 null。 */
    Object API = resolveApi();
    Method ASSIGN_PIPELINE = resolveMethod("assignPipeline");
    Method ASSIGN_PIPELINE_SHADOW = resolveMethod("assignPipelineShadow");
    Class<?> PROGRAM_TYPE = resolveType("net.irisshaders.iris.api.v0.IrisProgram");
    Class<?> SHADOW_PROGRAM_TYPE = resolveType("net.irisshaders.iris.api.v0.IrisShadowProgram");
    /** 阴影通道键：实体几何在 Iris 里只有这一个 {@code ShadowEntities} 键。 */
    String SHADOW_ENTITIES_KEY = "SHADOW_ENTITIES_CUTOUT";

    /**
     * 精确登记所需的 Iris 内部句柄。
     *
     * <p>必须在<b>首次真正需要</b>时才解析，且解析结果可重试：接口静态字段一锤定音会锁死
     * 「首次解析时机不对」（例如 `ShaderKey` 的静态初始化在 Iris 管线就绪前被触发）导致的
     * 永久回退——实机出现过登记静默走回 {@code IrisApi} 的情况（此时日志只有
     * {@code Found okay program match ... ENTITIES_ALPHA}，画面即不显示）。
     */
    final class ExactKeys {
        private static Class<?> shaderKeyType;
        private static Method assignMain;
        private static Method assignShadow;
        private static boolean resolveWarned;
    }

    /** 登记自定义 pipeline 的光影主通道与阴影通道程序；Iris 缺席时为空操作。 */
    static void assign(RenderPipeline pipeline, BrRenderState state) {
        if (API == null) {
            return;
        }
        if (ExactKeys.shaderKeyType == null) {
            resolveExactKeys();
        }
        if (!assignExact(ExactKeys.assignMain, pipeline, mainShaderKey(state))) {
            assign(ASSIGN_PIPELINE, PROGRAM_TYPE, pipeline, mainProgram(state));
        }
        if (!assignExact(ExactKeys.assignShadow, pipeline, SHADOW_ENTITIES_KEY)) {
            assign(ASSIGN_PIPELINE_SHADOW, SHADOW_PROGRAM_TYPE, pipeline, shadowProgram(state));
        }
    }

    private static void resolveExactKeys() {
        try {
            // 不初始化：ShaderKey 的静态初始化会拉入 Iris 顶点格式，留给 Enum.valueOf 触发
            ExactKeys.shaderKeyType = Class.forName(
                    "net.irisshaders.iris.pipeline.programs.ShaderKey", false,
                    IrisPipelineCompat.class.getClassLoader());
            Class<?> pipelines = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
            ExactKeys.assignMain = pipelines.getMethod("assignPipeline", RenderPipeline.class, ExactKeys.shaderKeyType);
            ExactKeys.assignShadow = pipelines.getMethod("assignPipelineShadow", RenderPipeline.class, ExactKeys.shaderKeyType);
        } catch (Throwable t) {
            if (!ExactKeys.resolveWarned) {
                ExactKeys.resolveWarned = true;
                LOGGER.warn("[compat] Iris 内部 ShaderKey 句柄解析失败（{}），本次会话退化为 IrisApi 程序匹配", t.toString());
            }
        }
    }

    /**
     * 主通道精确程序键（{@code ShaderKey} 常量名）。
     *
     * <p>键与 Iris 给 vanilla pipeline 的映射对齐（{@code IrisPipelines.getSolid/getCutout/getTranslucent}），
     * 使自定义 pipeline 与「同一材质的 vanilla RenderType」在光影里走同一条程序，观感一致。
     */
    private static String mainShaderKey(BrRenderState state) {
        return switch (state.surfaceClass()) {
            case GLINT -> "GLINT";
            case EMISSIVE_CUTOUT, TRANSLUCENT_EMISSIVE -> "ENTITIES_EYES_TRANS";
            case EMISSIVE -> "ENTITIES_EYES";
            case TRANSLUCENT, ADDITIVE -> "ENTITIES_TRANSLUCENT";
            case CUTOUT -> "ENTITIES_CUTOUT";
            default -> "ENTITIES_SOLID";
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean assignExact(Method method, RenderPipeline pipeline, String keyName) {
        if (method == null || ExactKeys.shaderKeyType == null) {
            return false;
        }
        try {
            method.invoke(null, pipeline, Enum.valueOf((Class<? extends Enum>) ExactKeys.shaderKeyType, keyName));
            return true;
        } catch (IllegalStateException alreadyAssigned) {
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[compat] Iris ShaderKey 登记失败（{} / {}），退回 IrisApi 路径：{}",
                    pipeline.getLocation(), keyName, t.toString());
            return false;
        }
    }

    /** 回退路径的主通道程序名（{@code IrisProgram} 常量名）；仅在精确 {@code ShaderKey} 不可用时使用。 */
    private static String mainProgram(BrRenderState state) {
        if (state.lighting() == BrRenderState.LightingModel.AMBIENT
                || state.lighting() == BrRenderState.LightingModel.NONE) {
            // 无方向光（粒子）也登记 ENTITIES：这些 pipeline 的 vertex format 是 ENTITY，
            // 而 Iris 的 PARTICLES 键要求 DefaultVertexFormat.PARTICLE，匹配过去会让属性布局错位；
            // 光影包本来接管光照，牺牲"自发光不受光"只影响光影下的亮度语义，不会画错/画黑。
            // LIGHTMAP_ONLY（实体，ADR-0040）不走此分支：其几何仍是实体语义，按 surfaceClass 映射。
            return "ENTITIES";
        }
        return switch (state.surfaceClass()) {
            case GLINT -> "ARMOR_GLINT";
            case EMISSIVE_CUTOUT -> "EMISSIVE_ENTITIES";
            case TRANSLUCENT, TRANSLUCENT_EMISSIVE, ADDITIVE -> "ENTITIES_TRANSLUCENT";
            default -> "ENTITIES";
        };
    }

    /** 阴影通道程序名（{@code IrisShadowProgram} 常量名）；实体几何一律用 {@code SHADOW_ENTITIES}。 */
    private static String shadowProgram(BrRenderState state) {
        return "SHADOW_ENTITIES";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void assign(Method method, Class<?> enumType, RenderPipeline pipeline, String programName) {
        if (method == null || enumType == null) {
            return;
        }
        try {
            method.invoke(API, pipeline, Enum.valueOf((Class<? extends Enum>) enumType, programName));
        } catch (IllegalStateException alreadyAssigned) {
            // 同一 pipeline 对象重复登记：Iris 抛 IllegalStateException，语义上已达成目标
        } catch (Throwable t) {
            LOGGER.warn("[compat] Iris pipeline 登记失败（{} / {}）：{}",
                    pipeline.getLocation(), programName, t.toString());
        }
    }

    private static Object resolveApi() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return apiClass.getMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException absent) {
            // 未安装 Iris/Oculus：不登记，也不打日志
            return null;
        } catch (Throwable t) {
            LOGGER.warn("[compat] IrisApi.getInstance() 不可用（{}），自定义 pipeline 不做光影程序登记", t.toString());
            return null;
        }
    }

    private static Method resolveMethod(String name) {
        if (API == null) {
            return null;
        }
        try {
            String enumName = name.equals("assignPipeline")
                    ? "net.irisshaders.iris.api.v0.IrisProgram"
                    : "net.irisshaders.iris.api.v0.IrisShadowProgram";
            return API.getClass().getMethod(name, RenderPipeline.class, Class.forName(enumName));
        } catch (Throwable t) {
            // API v0.3/v0.4 之前的 Iris：没有该端点，保持老行为（未登记）
            LOGGER.warn("[compat] IrisApi.{} 不可用（{}），该通道不做光影程序登记", name, t.toString());
            return null;
        }
    }

    private static Class<?> resolveType(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }
}
//?}
