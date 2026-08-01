//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 编辑器会话上下文（当前编辑的库 + 当前图的子图接口）。
 *
 * <p>{@link EvmNode} 的动态端口（subgraph.call 实参、subgraph.input/output 锚点）需要
 * 子图接口解析器；节点经节点面板 creator / 复制粘贴（createFromTag）创建时无法传参，
 * 故经此静态持有者取「当前会话」的解析器。编辑器同一时间只有一个屏幕，打开/潜入/返回时更新。
 */
public final class Ldlib1EditorSession {
    private static @Nullable GraphLibrary library;
    private static Optional<GraphInterface> self = Optional.empty();

    private Ldlib1EditorSession() {
    }

    /** 进入某张图的编辑（打开主图或潜入/返回子图时调用）。 */
    public static void enter(GraphLibrary lib, Optional<GraphInterface> selfInterface) {
        library = lib;
        self = selfInterface;
    }

    /** 当前会话的子图解析器（无会话 = 空解析器，全部解析失败）。 */
    public static NodeType.SubgraphResolver currentResolver() {
        GraphLibrary lib = library;
        if (lib == null) {
            return name -> Optional.empty();
        }
        return NodeType.SubgraphResolver.of(lib, self);
    }
}
//?}
