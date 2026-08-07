package io.github.tt432.eyelib.nodegraph;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 molang 函数注册表（规格 nodegraph-emolang-functions §3）。
 * 加载器（client 侧扫描 config/emolang）经 {@link #replaceAll} 整体替换；
 * codegen（EmitSession）与编辑器（端口签名/下拉候选）经 {@link #find}/{@link #all} 消费。
 * 注册时同步 {@link MolangFunctionSignatures} 的 customs 槽。
 *
 * @author TT432
 */
public final class EmolangRegistry {
    private EmolangRegistry() {
    }

    private static final Map<String, EmolangFunction> FUNCTIONS = new ConcurrentHashMap<>();

    /** 查函数（裸名）；未注册 → null。 */
    public static @Nullable EmolangFunction find(String name) {
        return FUNCTIONS.get(name);
    }

    /** 全部已注册函数（无序）。 */
    public static Collection<EmolangFunction> all() {
        return List.copyOf(FUNCTIONS.values());
    }

    /** 整体替换注册表（重扫加载）；同步函数签名表 customs 槽。 */
    public static void replaceAll(Collection<EmolangFunction> functions) {
        FUNCTIONS.clear();
        MolangFunctionSignatures.clearCustoms();
        for (EmolangFunction fn : functions) {
            FUNCTIONS.put(fn.name(), fn);
            MolangFunctionSignatures.registerCustom(fn.name(),
                    new MolangFunctionSignatures.Signature(
                            fn.params().stream()
                                    .map(p -> new MolangFunctionSignatures.Arg(p.name(), p.kind()))
                                    .toList(),
                            null));
        }
    }
}
