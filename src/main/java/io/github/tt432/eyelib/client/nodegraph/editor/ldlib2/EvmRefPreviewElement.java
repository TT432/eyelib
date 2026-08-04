package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import io.github.tt432.eyelib.client.nodegraph.preview.NodeAssetPreview;
import io.github.tt432.eyelib.nodegraph.NodeType;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import org.jetbrains.annotations.Nullable;
//?}

/**
 * ref.geometry / ref.texture 节点的 LDLib2 预览元素（规格 §3.3）：
 * 节点内嵌 64×64 区域（挂在内建 {@code NodePreviewModel} 面板的内容容器），
 * 棋盘格底衬透明；每帧重读节点选项（path / identifier）解析引用，
 * 选项编辑后自动同步；引用缺失画「未找到」占位。
 *
 * <p>26.1.2：纹理预览照常（IGuiTexture 渲染注册表路径不经 GuiGraphics）；
 * 模型预览因 26.1 GUI 渲染路径（GuiGraphics → GuiGraphicsExtractor）未迁移
 * （{@code ModelPreviewScreen.renderModelInViewport} 同为 {@code //? if <26.1}，
 * {@link NodeAssetPreview#renderModel} 仅 <26.1 存在）降级为 identifier 文本 + 不支持提示。
 *
 * <p>模型预览交互（仅 <26.1 注册）：左键拖动 = 轨道球旋转、右键拖动 = 平移、
 * 滚轮 = 缩放、双击 = 重置视角；事件只在本元素悬停/为事件目标时处理并终止传播
 * （不拦的话 GraphView 的右键画布平移会顶掉预览拖手势），预览区外的画布交互不受影响。
 */
public final class EvmRefPreviewElement extends UIElement {
    private static final int PREVIEW_SIZE = 64;
    private static final SpriteTexture CHECKERBOARD =
            SpriteTexture.of("eyelib:textures/gui/nodegraph/checkerboard.png");
    private static final IGuiTexture NOT_FOUND = new TextTexture("未找到", 0xFFFF5555).setWidth(PREVIEW_SIZE);

    private final EvmNodeBase node;

    //? if <26.1 {
    /** 拖手势哨兵（DragHandler.draggingObject 身份判定，画布/其他元素的拖互不误判）。 */
    private static final Object DRAG_ROTATE = new Object();
    private static final Object DRAG_PAN = new Object();
    //?}

    public EvmRefPreviewElement(EvmNodeBase node) {
        this.node = node;
        Style.defaultPipeline(getLayout(), l -> l.width(PREVIEW_SIZE).height(PREVIEW_SIZE));
        //? if <26.1 {
        if (!isTexture()) {
            // 模型预览交互：左拖旋转 / 右拖平移 / 滚轮缩放 / 双击重置（Scene 同款事件体系）
            addEventListener(UIEvents.MOUSE_DOWN, this::onPreviewMouseDown);
            addEventListener(UIEvents.MOUSE_WHEEL, this::onPreviewMouseWheel);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onPreviewDragUpdate);
            addEventListener(UIEvents.DOUBLE_CLICK, this::onPreviewDoubleClick);
        }
        //?}
    }

    //? if <26.1 {
    private void onPreviewMouseDown(UIEvent event) {
        if (!isHover()) return;
        if (event.button == 0 || event.button == 1) {
            startDrag(event.button == 0 ? DRAG_ROTATE : DRAG_PAN, null);
            // 终止冒泡：nodegraphtoolkit GraphView 的 MOUSE_DOWN 对右键无 target 判定，
            // 不拦的话它随后 startDrag(DragOffset) 会顶掉预览的拖手势（左键同理防 DragMove）
            event.stopPropagation();
        }
    }

    private void onPreviewDragUpdate(UIEvent event) {
        if (event.target != this || event.dragHandler == null) return;
        // 屏幕增量 → 元素局部（画布视图）增量，平移/旋转手感不随画布缩放变化
        var delta = getLocalMouseNormal(event.deltaX, event.deltaY);
        if (event.dragHandler.getDraggingObject() == DRAG_ROTATE) {
            node.previewViewState().rotateBy(delta.x, delta.y);
        } else if (event.dragHandler.getDraggingObject() == DRAG_PAN) {
            node.previewViewState().panBy(delta.x, delta.y);
        }
    }

    private void onPreviewMouseWheel(UIEvent event) {
        if (event.target != this) return;
        node.previewViewState().zoomBy(event.deltaY);
        event.stopPropagation();
    }

    private void onPreviewDoubleClick(UIEvent event) {
        if (event.target == this) {
            node.previewViewState().reset();
        }
    }
    //?}

    private boolean isTexture() {
        return node.type().kind() == NodeType.Kind.REF_TEXTURE;
    }

    /** 当前引用值（path / identifier），含选项默认值；每帧调用，编辑后即时生效。 */
    private String refValue() {
        var option = node.getNodeOptionById(isTexture() ? "path" : "identifier");
        if (option == null) return "";
        // tryGetValue(Type) 的 T 在 26.1 推断为 Object，map(toString) 兼容两版本（DataResult.result() → Optional）
        return option.tryGetValue(String.class).result().map(Object::toString).orElse("");
    }

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
        guiContext.drawTexture(CHECKERBOARD, x, y, w, h);
        if (isTexture()) {
            var texture = NodeAssetPreview.resolveTexture(refValue());
            guiContext.drawTexture(texture == null ? NOT_FOUND : SpriteTexture.of(texture), x, y, w, h);
        } else {
            var handle = NodeAssetPreview.resolveModel(refValue());
            if (handle == null) {
                guiContext.drawTexture(NOT_FOUND, x, y, w, h);
            } else {
                // guiTextured 无深度测试，画家算法后画者覆盖——先把棋盘格 flush 落盘，
                // 否则模型顶点在屏幕末 flush 时被棋盘格覆盖（同 ldlib1 EvmNode 处理）。
                guiContext.graphics.flush();
                NodeAssetPreview.renderModel(handle, guiContext.graphics,
                        (int) x, (int) y, (int) w, (int) h, guiContext.partialTick, node.previewViewState());
            }
        }
    }
    //?} else {
    private @Nullable String lastDegradeValue;
    private @Nullable IGuiTexture degradeTexture;

    @Override
    protected void drawBackgroundAdditional(IGUIContext context) {
        float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
        context.drawTexture(CHECKERBOARD, x, y, w, h);
        if (isTexture()) {
            var texture = NodeAssetPreview.resolveTexture(refValue());
            context.drawTexture(texture == null ? NOT_FOUND : SpriteTexture.of(texture), x, y, w, h);
        } else {
            String value = refValue();
            if (NodeAssetPreview.resolveModel(value) == null) {
                context.drawTexture(NOT_FOUND, x, y, w, h);
            } else {
                if (!value.equals(lastDegradeValue)) {
                    lastDegradeValue = value;
                    degradeTexture = new TextTexture(value + "\n(26.1 不支持模型预览)", 0xFFAAAAAA)
                            .setWidth(PREVIEW_SIZE);
                }
                context.drawTexture(degradeTexture, x, y, w, h);
            }
        }
    }
    //?}
}
//?}
