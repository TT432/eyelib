//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;

import java.lang.annotation.Annotation;

/**
 * EVM 节点类型的显式注册（graphprocessor 节点目录）。
 *
 * <p>调研结论（证据见适配报告）：{@code AnnotationDetector.init()} 经 Forge
 * {@code ModList.get().getAllScanData()} 扫描全部已加载 mod 的 @LDLRegister 类，
 * eyelib 的类可以被自动发现——但 {@link EvmNode} 是一个类对应目录中全部节点类型
 * （每个类型需独立注册名/分组/creator），无法用单个静态注解表达，故不走自动扫描，
 * 直接操作公开注册表 {@code AnnotationDetector.REGISTER_GP_NODES}
 * （{@code Map<String, Wrapper<LDLRegister, ? extends BaseNode>>}，key = 注册名）。
 * init() 只 putAll 不清空，显式注册在任意时机均安全；注册在编辑器首次打开时进行（幂等）。
 *
 * <p>注册名 = 域节点类型 id（如 {@code "const.number"}），与 LDLib 内建短名
 * （如 {@code "number"}）不冲突；分组 = {@link #GROUP_PREFIX} + 域类别，
 * 画布经 {@code GraphViewWidget} 第 5 参 additionalGroups 放行该前缀。
 */
public final class EvmNodeRegistration {
    /** 节点面板分组前缀（与 GraphViewWidget additionalGroups 一致）。 */
    public static final String GROUP_PREFIX = "graph_processor.node.evm";

    private static boolean registered = false;

    private EvmNodeRegistration() {
    }

    /** 幂等注册全部域节点类型 + 端口类型适配器。 */
    public static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        EvmLinks.ensureTypeAdapters();
        for (NodeType type : NodeTypes.all()) {
            String name = type.id();
            LDLRegister annotation = annotation(name, GROUP_PREFIX + "." + type.category());
            AnnotationDetector.REGISTER_GP_NODES.put(name, new AnnotationDetector.Wrapper<>(
                    annotation, EvmNode.class,
                    () -> new EvmNode(name, Ldlib1EditorSession.currentResolver())));
        }
    }

    /** 运行期构造 LDLRegister 注解实例（显式注册的 Wrapper 需要注解载体）。 */
    private static LDLRegister annotation(String name, String group) {
        return new LDLRegister() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return LDLRegister.class;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String group() {
                return group;
            }

            @Override
            public String modID() {
                return "";
            }

            @Override
            public int priority() {
                return 0;
            }
        };
    }
}
//?}
