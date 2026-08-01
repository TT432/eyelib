//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;

import java.util.List;

/**
 * EVM 编辑会话用的 BaseGraph：在纯数据图之上携带「正在编辑哪座库的哪张图」上下文，
 * 供保存/潜入回写时定位。
 *
 * @param libraryName 库名（{@code GraphLibraryManager} 键 / 落盘文件名）
 * @param graphName   当前图名（库内 graphs 键）
 * @param library     当前库快照（导航/保存后由编辑器替换）
 */
public class EvmBaseGraph extends BaseGraph {
    private final String libraryName;
    private final String graphName;
    private final GraphLibrary library;

    public EvmBaseGraph(String libraryName, String graphName, GraphLibrary library,
                        List<ExposedParameter<?>> exposedParameters) {
        super(exposedParameters);
        this.libraryName = libraryName;
        this.graphName = graphName;
        this.library = library;
    }

    public String libraryName() {
        return libraryName;
    }

    public String graphName() {
        return graphName;
    }

    public GraphLibrary library() {
        return library;
    }
}
//?}
