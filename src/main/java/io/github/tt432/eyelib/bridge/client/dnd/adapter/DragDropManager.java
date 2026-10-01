package io.github.tt432.eyelib.bridge.client.dnd.adapter;

import io.github.tt432.eyelib.bridge.client.dnd.PositionedDropTarget;

import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端拖放悬停管理器（单例）。
 * <p>仅 Windows：用 OLE {@code IDropTarget} 接管 GLFW 窗口拖放，提供 hover 回调；
 * 其它平台/JNA 不可用时 {@link #isAvailable()} 返回 false，编辑器退回松手后按文件名导入（fallback）。
 * <p>OLE 回调在主线程（{@code glfwPollEvents} 分发），hover 状态用 volatile 供 render 同线程读。
 */
public final class DragDropManager {
    public static final DragDropManager INSTANCE = new DragDropManager();

    private static final Logger LOGGER = LoggerFactory.getLogger("eyelib/DragDrop");

    private volatile boolean available = false;
    private volatile boolean installed = false;

    // hover 状态（render 在主线程读；Drop 也更新）
    private volatile boolean dragging = false;
    private volatile int hoverX = 0;
    private volatile int hoverY = 0;
    private volatile @org.jspecify.annotations.Nullable String hoveredFirstPath;

    private volatile @org.jspecify.annotations.Nullable Pointer hwnd;

    private DragDropManager() {
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isDragging() {
        return available && dragging;
    }

    public int getHoverX() {
        return hoverX;
    }

    public int getHoverY() {
        return hoverY;
    }

    @org.jspecify.annotations.Nullable Pointer getHwnd() {
        return hwnd;
    }

    public @org.jspecify.annotations.Nullable String getHoveredFirstPath() {
        return hoveredFirstPath;
    }

    private static int toGui(int clientPx) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        return scale <= 0 ? clientPx : (int) (clientPx / scale);
    }

    /**
     * 在 FMLClientSetupEvent（window 已创建）调用。失败 fail-soft：置 disabled，不影响游戏。
     */
    public void install() {
        if (installed) return;
        if (!isWindows()) {
            LOGGER.info("DragDrop hover: 非 Windows 平台，跳过 OLE 注册（使用 fallback 松手导入）");
            installed = true;
            return;
        }
        try {
            doInstallWindows();
            available = true;
            installed = true;
            LOGGER.info("DragDrop hover: OLE IDropTarget 注册成功，hover 向导可用");
        } catch (Throwable t) {
            LOGGER.warn("DragDrop hover: OLE 注册失败，退回 fallback 松手导入", t);
            available = false;
            installed = true;
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private void doInstallWindows() {
        //? if <26.1 {
        long glfwHandle = Minecraft.getInstance().getWindow().getWindow();
        //?} else {
        // 26.1：Window.getWindow() → handle()
        long glfwHandle = Minecraft.getInstance().getWindow().handle();
        //?}
        long hwndVal = GLFWNativeWin32.glfwGetWin32Window(glfwHandle);
        if (hwndVal == 0L) {
            throw new IllegalStateException("glfwGetWin32Window 返回 0");
        }
        this.hwnd = new Pointer(hwndVal);

        int hr = WinNative.Ole32.INSTANCE.OleInitialize(Pointer.NULL);
        // S_OK(0) 或 S_FALSE(1=已初始化) 都可接受
        LOGGER.debug("OleInitialize hr=0x{}", Integer.toHexString(hr));

        // 撤销 GLFW 已注册的 OLE drop target，否则 RegisterDragDrop 会 ALREADYREGISTERED
        int hrRevoke = WinNative.Ole32.INSTANCE.RevokeDragDrop(hwnd);
        LOGGER.debug("RevokeDragDrop(GLFW) hr=0x{}", Integer.toHexString(hrRevoke));

        WinDropTarget target = new WinDropTarget(this);
        int hrReg = WinNative.Ole32.INSTANCE.RegisterDragDrop(hwnd, target.handle());
        if (hrReg != WinNative.S_OK) {
            throw new IllegalStateException("RegisterDragDrop 失败 hr=0x" + Integer.toHexString(hrReg));
        }
        // target 必须常驻（vtable + callback 强引用），随 INSTANCE 生命周期
        keepAlive(target);
    }

    // ---- OLE 回调（主线程）----

    void onDragEnter(int clientX, int clientY, @org.jspecify.annotations.Nullable String firstPath) {
        dragging = true;
        hoverX = toGui(clientX);
        hoverY = toGui(clientY);
        hoveredFirstPath = firstPath;
        LOGGER.info("DragEnter gui=({}, {}) first={}", hoverX, hoverY, firstPath);
    }

    void onDragOver(int clientX, int clientY) {
        hoverX = toGui(clientX);
        hoverY = toGui(clientY);
    }

    void onDragLeave() {
        dragging = false;
        LOGGER.info("DragLeave");
    }

    void onDrop(int clientX, int clientY, List<String> paths) {
        dragging = false;
        int gx = toGui(clientX);
        int gy = toGui(clientY);
        LOGGER.info("Drop gui=({}, {}) paths={}", gx, gy, paths);
        dispatchDrop(gx, gy, paths);
    }

    /**
     * 松手后把文件路径分发到当前 Screen：
     * <p>若 Screen 实现 {@link PositionedDropTarget}（如判定编辑器），传落点坐标由其按位置决定类型；
     * 否则透明转发 {@link Screen#onFilesDrop} 复刻 MC 原行为。
     * <p>注意：Windows 上接管 OLE 后，MC 原 WM_DROPFILES→onFilesDrop 链路不再触发，
     * 因此所有 Screen 的拖入都由这里转发，否则其它界面拖入功能会失效。
     */
    private void dispatchDrop(int x, int y, List<String> paths) {
        if (paths == null || paths.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return;
        List<Path> pathList = new ArrayList<>();
        for (String p : paths) {
            if (p != null && !p.isEmpty()) pathList.add(Paths.get(p));
        }
        if (pathList.isEmpty()) return;
        if (screen instanceof PositionedDropTarget pdt) {
            mc.execute(() -> pdt.onFilesDropWithPosition(pathList, x, y));
        } else {
            mc.execute(() -> screen.onFilesDrop(pathList));
        }
    }

    // target 的强引用持有处（与 INSTANCE 同生命周期）
    @SuppressWarnings("unused")
    private volatile @org.jspecify.annotations.Nullable Object aliveRef;

    private void keepAlive(Object target) {
        this.aliveRef = target;
    }
}
