package io.github.tt432.eyelib.bridge.client.compat.oculus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BooleanSupplier;

//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}

/**
 * Oculus（1.20.1 Forge，modid "oculus"）/ Iris（1.21.1 NeoForge，modid "iris"）光影兼容检测。
 *
 * <p>不兼容机制（Oculus 1.20.1-new 与 Iris 1.21.1 源码实证，MixinShaderInstance）：
 * 光影包激活时，任何<b>非 Iris 自有</b>的 ShaderInstance 调 apply() 后会被
 * {@code DepthColorStorage.disableDepthColor()} 锁定——depthMask/colorMask 全关且锁定期间
 * 后续设置被 defer，直到 clear() 才解锁恢复。eyelib 的自定义蒙皮 shader（VS 与 compute 批次
 * 路径均经 drawWithShader → apply()）因此绘制零像素：基岩版模型整体不渲染。
 * vanilla RenderType 批次不受影响（Iris 用 ExtendedShader/FallbackShader 接管，豁免锁定）。
 *
 * <p>对策：光影激活期间 GPU 蒙皮会话不创建，回退经典 CPU 路径（vanilla 批次由 Iris 正常接管）。
 * 检测走官方 API {@code IrisApi.isShaderPackInUse()}（API 文档明示"需要为光影启用变通方案的
 * mod 应使用此方法"）；反射调用，无编译期依赖。mod 在但 API 反射失败时按"激活"处理
 * （保守方向：CPU 路径永远正确，GPU 路径在光影下必黑）。
 */
public interface OculusCompat {
    Logger LOGGER = LoggerFactory.getLogger(OculusCompat.class);

    /** 光影激活判定；启动期一次性解析，运行期零反射开销。 */
    BooleanSupplier SHADER_PACK_ACTIVE = resolve("isShaderPackInUse");
    BooleanSupplier SHADOW_PASS = resolve("isRenderingShadowPass");

    /** 光影包正在接管世界渲染时为 true：GPU 蒙皮必须回退经典 CPU 路径。 */
    static boolean shaderPackActive() {
        return SHADER_PACK_ACTIVE.getAsBoolean();
    }

    /** 原色通道不参与阴影贴图，也不能把阴影相机矩阵留到主视角提交。 */
    static boolean renderingShadowPass() {
        return SHADOW_PASS.getAsBoolean();
    }

    private static BooleanSupplier resolve(String method) {
        LoadingModList modList = LoadingModList.get();
        boolean present = modList != null
                && (modList.getModFileById("oculus") != null || modList.getModFileById("iris") != null);
        if (!present) {
            return () -> false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            MethodHandle inUse = MethodHandles.publicLookup()
                    .findVirtual(apiClass, method, MethodType.methodType(boolean.class))
                    .bindTo(api);
            return () -> {
                try {
                    return (boolean) inUse.invokeExact();
                } catch (Throwable t) {
                    return true; // API 调用失败按激活处理（保守方向）
                }
            };
        } catch (Throwable t) {
            LOGGER.warn("[compat] Oculus/Iris 已加载但 IrisApi.{} 解析失败（{}），该能力按激活处理",
                    method, t.toString());
            return () -> true;
        }
    }
}
