package io.github.tt432.eyelib.client.nodegraph.preview;

/**
 * 节点资源预览的交互视角状态（per 预览控件实例；由节点/元素持有，widget 重建不丢）。
 *
 * <p>手势约定：左键拖动 = 轨道球旋转（yaw 绕 Y / pitch 绕 X，pitch 钳制 ±89°）、
 * 右键拖动 = 屏幕平面平移、滚轮 = 缩放（钳制 [{@value #MIN_ZOOM}, {@value #MAX_ZOOM}]，
 * 锚 = 控件中心 + 当前平移，平移后的内容在缩放下保持不动）、双击 = 重置。
 * {@link #interacted} = false 时渲染侧保留包围盒自动取景 + 默认 30° 俯视；
 * 一旦用户交互则以用户视角为准。
 *
 * <p>纯数据类，不依赖 MC/LDLib，ldlib1 / ldlib2 两版编辑器共用。
 */
public final class PreviewViewState {
    public static final float MIN_ZOOM = 0.2f;
    public static final float MAX_ZOOM = 8f;
    private static final float PITCH_LIMIT = 89f;

    /** 左拖：绕 Y 旋转（度）。 */
    public float yaw;
    /** 左拖：绕 X 旋转（度，钳制 ±{@value #PITCH_LIMIT}）。 */
    public float pitch;
    /** 右拖：屏幕平面平移（预览局部坐标 px，随画布缩放逐帧换算，手感与光标 1:1）。 */
    public float panX;
    public float panY;
    /** 滚轮：相对包围盒自动取景基准的缩放倍率。 */
    public float zoom = 1f;
    /** 用户是否已接管视角（false = 默认 30° 俯视自动取景）。 */
    public boolean interacted;

    public void rotateBy(float dx, float dy) {
        yaw += dx;
        pitch = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, pitch + dy));
        interacted = true;
    }

    public void panBy(float dx, float dy) {
        panX += dx;
        panY += dy;
        interacted = true;
    }

    /** 指数缩放（滚轮每格 ≈ ×1.15，与画布缩放手感一致）。 */
    public void zoomBy(float wheelDelta) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (float) Math.pow(1.15, wheelDelta)));
        interacted = true;
    }

    public void reset() {
        yaw = pitch = panX = panY = 0;
        zoom = 1f;
        interacted = false;
    }
}
