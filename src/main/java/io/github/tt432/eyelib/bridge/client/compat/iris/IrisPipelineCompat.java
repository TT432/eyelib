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
 * <p>对策：构建 pipeline 时调用官方 API（IrisApi v0.3/v0.4）登记主通道与阴影通道程序。
 * 反射调用，无编译期依赖；Iris/Oculus 不在时静默跳过。
 *
 * <p>程序选择约束：{@code IrisApi.assignPipeline} 内部用 {@code ShaderKey.findBestMatch} 匹配，
 * 只有带 {@code ALPHA_CUTOUT} define 的 pipeline 才可能命中实体细化键（{@code ENTITIES_CUTOUT} /
 * {@code ENTITIES_EYES_TRANS}）；无该 define 时退化为「同程序首个键」，而 {@code SpiderEyes} 的首个键
 * {@code SPS} 是 {@code POSITION_TEX_COLOR} 格式，与实体自带 vertex format 不符 → 会画出垃圾像素。
 * 因此非 cutout 的发光面登记为 {@code ENTITIES}（命中 {@code ENTITIES_ALPHA}，顶点 alpha 测试），
 * 只有 {@code EMISSIVE_CUTOUT}（带 ALPHA_CUTOUT）才登记 {@code EMISSIVE_ENTITIES}。
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

    /** 登记自定义 pipeline 的光影主通道与阴影通道程序；Iris 缺席或 API 不可用时为空操作。 */
    static void assign(RenderPipeline pipeline, BrRenderState state) {
        if (API == null) {
            return;
        }
        assign(ASSIGN_PIPELINE, PROGRAM_TYPE, pipeline, mainProgram(state));
        assign(ASSIGN_PIPELINE_SHADOW, SHADOW_PROGRAM_TYPE, pipeline, shadowProgram(state));
    }

    /** 主通道程序名（{@code IrisProgram} 常量名）。 */
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
