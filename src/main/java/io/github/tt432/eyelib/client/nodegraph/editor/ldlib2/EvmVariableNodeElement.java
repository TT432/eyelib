package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.SinglePortContainerElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.VariableNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import org.jspecify.annotations.Nullable;

/**
 * EVM 变量节点元素：LDLib2 的 {@link VariableNodeElement} 只渲染主口
 * （{@code ISingleInput/OutputPortNodeModel} 各一口），v9 左读右写补的写入入口
 * {@link EvmGraph.EvmVariableNodeModel#WRITE_IN_PORT_ID in} 不在其列——本类在标题左侧
 * 补出该端口的容器，否则写入边渲染时找不到落点 PortElement，端点漂到画布远端
 * （2026-08-10 用户截图实证：ref write: → variable 的边横跨全图）。
 */
public final class EvmVariableNodeElement extends VariableNodeElement {

    private @Nullable SinglePortContainerElement writeInContainer;

    public EvmVariableNodeElement(VariableNodeModel variableNodeModel) {
        super(variableNodeModel);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        PortModel writeIn = getModel().getInputsById()
                .get(EvmGraph.EvmVariableNodeModel.WRITE_IN_PORT_ID);
        if (writeIn != null && writeInContainer == null) {
            // 与 CapsuleNodeElement 主口容器同款样式；插在 scopeImage(0) 之后、标题之前 = 左侧
            writeInContainer = new SinglePortContainerElement(writeIn);
            getParts().add(writeInContainer);
            writeInContainer.setGraphView(getGraphView());
            Style.defaultPipeline(writeInContainer.getPortContainer().getStyle(),
                    s -> s.background(IGuiTexture.EMPTY));
            Style.defaultPipeline(writeInContainer.getPortContainer().getLayout(), l -> l.paddingAll(0));
            addChildAt(writeInContainer, 1);
        } else if (writeIn == null && writeInContainer != null) {
            getParts().remove(writeInContainer);
            writeInContainer.setGraphView(null);
            writeInContainer.removeSelf();
            writeInContainer = null;
        }
        if (writeInContainer != null) {
            writeInContainer.updateUIFromModel(visitor);
        }
    }
}
//?}
