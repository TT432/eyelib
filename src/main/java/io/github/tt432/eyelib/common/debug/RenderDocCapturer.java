package io.github.tt432.eyelib.common.debug;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RenderDoc In-App API 封装。
 * 通过 JNA 调用 renderdoc.dll 的 StartFrameCapture/EndFrameCapture，
 * 允许在 /eval 中精确控制截帧时机。仅在 capture 模式启动时才可用。
 *
 * @author TT432
 */
@NullMarked
public final class RenderDocCapturer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderDocCapturer.class);

    // API 版本号
    private static final int API_VERSION = 10600;

    // RENDERDOC_API_1_7_0 struct 中的函数指针偏移量（x64，每个指针 8 字节）
    // 从 renderdoc_app.h 第 751 行的 struct 定义直接计数：
    //   0: GetAPIVersion
    //   8: SetCaptureOptionU32
    //  16: SetCaptureOptionF32
    //  24: GetCaptureOptionU32
    //  32: GetCaptureOptionF32
    //  40: SetFocusToggleKeys
    //  48: SetCaptureKeys
    //  56: GetOverlayBits
    //  64: MaskOverlayBits
    //  72: union { Shutdown / RemoveHooks }
    //  80: UnloadCrashHandler
    //  88: union { SetLogFilePathTemplate / SetCaptureFilePathTemplate }
    //  96: union { GetLogFilePathTemplate / GetCaptureFilePathTemplate }
    // 104: GetNumCaptures
    // 112: GetCapture
    // 120: TriggerCapture
    // 128: union { IsRemoteAccessConnected / IsTargetControlConnected }
    // 136: LaunchReplayUI
    // 144: SetActiveWindow
    // 152: StartFrameCapture          ← 第 19 个字段 (19*8)
    // 160: IsFrameCapturing
    // 168: EndFrameCapture            ← 第 21 个字段 (21*8)
    private static final int OFFSET_START_FRAME_CAPTURE = 152;
    private static final int OFFSET_END_FRAME_CAPTURE = 168;

    // SetCaptureFilePathTemplate 在 union 内（偏移 88），取 union 起始偏移
    private static final int OFFSET_SET_CAPTURE_PATH_TEMPLATE = 88;

    private static volatile boolean initialized;
    @Nullable
    private static Pointer apiStruct;

    private RenderDocCapturer() {
    }

    public static boolean isAvailable() {
        try {
            NativeLibrary.getInstance("renderdoc");
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static synchronized boolean init() {
        if (initialized) {
            return apiStruct != null;
        }
        initialized = true;

        try {
            NativeLibrary lib = NativeLibrary.getInstance("renderdoc");
            Function getApi = lib.getFunction("RENDERDOC_GetAPI");

            // 分配输出缓冲区，调用 RENDERDOC_GetAPI
            Memory buf = new Memory(Native.POINTER_SIZE);
            buf.setLong(0, 0);

            Object ret = getApi.invoke(int.class, new Object[]{API_VERSION, buf});
            int result = ((Number) ret).intValue();
            if (result != 1) {
                LOGGER.warn("RenderDoc GetAPI returned {}", result);
                return false;
            }

            long apiPtr = buf.getLong(0);
            if (apiPtr == 0) {
                LOGGER.warn("RenderDoc GetAPI returned null struct pointer");
                return false;
            }

            // 必须用 new Pointer(addr) 而非 createConstant，后者创建不透明指针无法解引用
            apiStruct = new Pointer(apiPtr);
            LOGGER.info("RenderDoc In-App API initialized (struct=0x{})", Long.toHexString(apiPtr));
            return true;
        } catch (UnsatisfiedLinkError e) {
            LOGGER.debug("RenderDoc not available");
            return false;
        } catch (Throwable t) {
            LOGGER.warn("Failed to init RenderDoc API: {}", t.toString());
            return false;
        }
    }

    /**
     * 设置捕获文件路径模板。
     *
     * @param template 路径模板，如 "E:\\captures\\my_frame"
     */
    public static void setCaptureFilePathTemplate(String template) {
        if (!init()) return;
        Pointer api = apiStruct;
        if (api == null) return;
        try {
            Pointer funcPtr = api.getPointer(OFFSET_SET_CAPTURE_PATH_TEMPLATE);
            if (funcPtr == null) return;
            Function fn = Function.getFunction(funcPtr, Function.ALT_CONVENTION);
            fn.invoke(void.class, new Object[]{template});
            LOGGER.info("RenderDoc capture path template set to: {}", template);
        } catch (Throwable t) {
            LOGGER.error("SetCaptureFilePathTemplate failed: {}", t.toString());
        }
    }

    public static void startCapture() {
        if (!init()) return;
        Pointer api = apiStruct;
        if (api == null) return;
        try {
            Pointer funcPtr = api.getPointer(OFFSET_START_FRAME_CAPTURE);
            if (funcPtr == null) return;
            Function fn = Function.getFunction(funcPtr, Function.ALT_CONVENTION);
            fn.invoke(void.class, new Object[]{Pointer.NULL, Pointer.NULL});
            LOGGER.info("RenderDoc frame capture started");
        } catch (Throwable t) {
            LOGGER.error("StartFrameCapture failed: {}", t.toString());
        }
    }

    public static boolean endCapture() {
        if (!init()) return false;
        Pointer api = apiStruct;
        if (api == null) return false;
        try {
            Pointer funcPtr = api.getPointer(OFFSET_END_FRAME_CAPTURE);
            if (funcPtr == null) return false;
            Function fn = Function.getFunction(funcPtr, Function.ALT_CONVENTION);
            Object ret = fn.invoke(int.class, new Object[]{Pointer.NULL, Pointer.NULL});
            int result = ((Number) ret).intValue();
            LOGGER.info("RenderDoc frame capture ended: result={}", result);
            return result == 1;
        } catch (Throwable t) {
            LOGGER.error("EndFrameCapture failed: {}", t.toString());
            return false;
        }
    }
}
