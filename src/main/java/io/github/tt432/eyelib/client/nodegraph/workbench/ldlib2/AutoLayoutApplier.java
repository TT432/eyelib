package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmDiagnostics;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraph;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraphTranslator;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.decompile.GraphLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector2f;

/**
 * 「自动布局」按钮动作（规格 nodegraph-workbench §W2 的实机入口）：
 * 当前显示图（root 或潜入的子图）域化 → {@link GraphLayout} 重排 → 写回活模型坐标。
 * 自动布局此前只在导入时跑，打开已保存库沿用存档坐标（用户实机截图里的长线即修复前存档）。
 * setPosition 走 ChangeHint.LAYOUT 变更轨道，NodeElement 随帧同步；动作后标脏，Ctrl+S 落盘。
 */
final class AutoLayoutApplier {
    private AutoLayoutApplier() {
    }

    static void apply(GraphEditorView editorView) {
        GraphView view = editorView.getCurrentView();
        Graph graph = view.getGraph();
        Graph rootGraph = editorView.getGraph();
        if (graph == null || rootGraph == null) return;
        GraphModel model = graph.graphModel;
        EvmGraph.LibraryContext ctx = rootGraph instanceof EvmGraph evm ? evm.context() : null;
        List<Diagnostic> diags = new ArrayList<>();
        GraphData data = EvmGraphTranslator.graphDataOf(model, model != rootGraph.graphModel, ctx, diags);
        List<NodeInstance> laidOut = GraphLayout.layout(data.nodes(), data.wires());
        List<StickyNote> notes = GraphLayout.placeStickyNotes(data.stickyNotes(), laidOut);

        Map<String, NodeInstance> byUid = new HashMap<>();
        for (NodeInstance n : laidOut) {
            byUid.put(n.uid(), n);
        }
        int moved = 0;
        for (AbstractNodeModel nm : model.getNodeModels()) {
            if (nm == null) continue;
            NodeInstance target = byUid.get(nm.getUid().toString());
            if (target == null) continue;
            Vector2f pos = new Vector2f(target.x(), target.y());
            if (!pos.equals(nm.getPosition())) {
                nm.setPosition(pos);
                moved++;
            }
        }
        Map<String, StickyNote> noteByUid = new HashMap<>();
        for (StickyNote n : notes) {
            noteByUid.put(n.uid(), n);
        }
        for (StickyNoteModel s : model.getStickyNoteModels()) {
            if (s == null) continue;
            StickyNote target = noteByUid.get(s.getUid().toString());
            if (target != null) {
                s.setPosition(new Vector2f(target.x(), target.y()));
            }
        }
        editorView.markAsDirty();
        view.fitGraphChildren(15f);
        EvmDiagnostics.info("自动布局完成：" + moved + "/" + laidOut.size() + " 节点重排（Ctrl+S 落盘）");
    }
}
//?}
