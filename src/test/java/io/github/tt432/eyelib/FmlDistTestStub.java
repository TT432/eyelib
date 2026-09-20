package io.github.tt432.eyelib;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

/**
 * FML Dist 测试桩（纯反射实现，三版本可编译）。
 *
 * <p>背景：LDLib2 26.1.x 的 {@code NodeModel.addNodeOption → IGuiTexture.createCodec →
 * Platform.isClient → FMLEnvironment.getDist} 在纯 JUnit 环境抛
 * {@code IllegalStateException: There is no current FML Loader}（无游戏引导）。
 * 本桩向 fancymodloader 11.x（26.1+）注入最小 {@code FMLLoader} 实例：
 * {@code Unsafe.allocateInstance} 跳过构造函数副作用链（ProgramArgs 解析、审计日志等），
 * 仅写 {@code dist = CLIENT}（{@code getDist()} 唯一读取字段），并装入 {@code current} 引用。
 *
 * <p>幂等；已有真实 FML 环境（junit-fml / 游戏内）或 loader 4.x（1.21.1，静态 dist 布局，
 * 其 LDLib2 2.2.x 不查询 Dist）时直接跳过。
 */
public final class FmlDistTestStub {
    private static boolean attempted;

    private FmlDistTestStub() {
    }

    /** 确保 {@code FMLEnvironment.getDist()} 可用（必要时注入 CLIENT 桩）。 */
    public static synchronized void ensureClientDist() {
        if (attempted) {
            return;
        }
        attempted = true;

        Class<?> loaderClass;
        try {
            loaderClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
        } catch (ClassNotFoundException e) {
            return; // 类路径无 FML：无需 stub
        }

        try {
            Object current = loaderClass.getMethod("getCurrentOrNull").invoke(null);
            if (current != null) {
                return; // 已有真实 FML 环境，不覆盖
            }
        } catch (NoSuchMethodException e) {
            // loader 4.x（1.21.1）：静态 dist 布局、无 current 实例模型；该版本 LDLib2 不查 Dist
            return;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("探测 FMLLoader current 失败", e);
        }

        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);

            Object loader = unsafe.allocateInstance(loaderClass);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object client = Enum.valueOf(
                    (Class) Class.forName("net.neoforged.api.distmarker.Dist"), "CLIENT");
            Field distField = loaderClass.getDeclaredField("dist");
            unsafe.putObject(loader, unsafe.objectFieldOffset(distField), client);

            Field currentField = loaderClass.getDeclaredField("current");
            currentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            AtomicReference<Object> current = (AtomicReference<Object>) currentField.get(null);
            current.set(loader);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FML Dist 测试桩注入失败", e);
        }
        // LDLib2 客户端注册表 clinit（AutoRegistry → ReflectionUtils.findAnnotationClasses）
        // 需要 ModList.get() 非空；注入空 ModList（scanData 为空 → autoRegister 无操作）。
        // ModList.of 自身回写 INSTANCE，无需反射。
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            if (modListClass.getMethod("get").invoke(null) == null) {
                modListClass.getMethod("of", List.class, List.class)
                        .invoke(null, List.of(), List.of());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("空 ModList 测试桩注入失败", e);
        }
    }
}
