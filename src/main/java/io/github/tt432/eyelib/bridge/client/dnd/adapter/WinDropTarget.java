package io.github.tt432.eyelib.bridge.client.dnd.adapter;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.github.tt432.eyelib.bridge.client.dnd.adapter.WinNative.*;

/**
 * 手写的 Windows COM {@code IDropTarget} 实现（JNA 版）。
 * <p>内存布局：对象块 = 一个指向 vtable 的指针；vtable = 7 个 stdcall 函数指针
 * （QueryInterface/AddRef/Release/DragEnter/DragOver/DragLeave/Drop）。
 * <p>所有 vtable Memory 与 7 个 Callback 必须保持强引用（实例字段），否则 GC 后 OLE 回调会崩溃。
 * 回调在主线程（glfwPollEvents 分发 OLE 消息）触发，委托 {@link DragDropManager} 更新状态。
 */
final class WinDropTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger("eyelib/DragDrop");

    private final int ps = Native.POINTER_SIZE;
    private final Memory vtable = new Memory((long) ps * 7);
    private final Memory object = new Memory(ps);

    // 强引用所有 callback，防止函数指针被 GC 回收。
    private final QueryInterfaceCb qi;
    private final AddRefCb addRef;
    private final ReleaseCb release;
    private final DragEnterCb dragEnter;
    private final DragOverCb dragOver;
    private final DragLeaveCb dragLeave;
    private final DropCb drop;

    WinDropTarget(DragDropManager manager) {
        // IUnknown
        qi = (self, riid, ppv) -> {
            if (ppv == null) return E_INVALIDARG;
            if (refEquals(riid, IID_IUNKNOWN) || refEquals(riid, IID_IDROPTARGET)) {
                ppv.setValue(self);
                return S_OK;
            }
            ppv.setValue(null);
            return E_NOINTERFACE;
        };
        // Spike 阶段引用计数简化：返回 1，不 delete-this（对象随 DragDropManager 常驻）。
        addRef = self -> 1;
        release = self -> 1;

        dragEnter = (self, dataObj, keyState, pt, pdwEffect) -> {
            if (pdwEffect == null) return E_INVALIDARG;
            if (!queryHasHdrop(dataObj)) {
                pdwEffect.setValue(DROPEFFECT_NONE);
                return S_OK;
            }
            int[] xy = screenToClient(pt.x, pt.y);
            List<String> paths = extractPaths(dataObj);
            String first = paths.isEmpty() ? null : paths.get(0);
            manager.onDragEnter(xy[0], xy[1], first);
            pdwEffect.setValue(DROPEFFECT_COPY);
            return S_OK;
        };
        dragOver = (self, keyState, pt, pdwEffect) -> {
            if (pdwEffect == null) return E_INVALIDARG;
            int[] xy = screenToClient(pt.x, pt.y);
            manager.onDragOver(xy[0], xy[1]);
            pdwEffect.setValue(DROPEFFECT_COPY);
            return S_OK;
        };
        dragLeave = self -> {
            manager.onDragLeave();
            return S_OK;
        };
        drop = (self, dataObj, keyState, pt, pdwEffect) -> {
            int[] xy = screenToClient(pt.x, pt.y);
            List<String> paths = extractPaths(dataObj);
            manager.onDrop(xy[0], xy[1], paths);
            if (pdwEffect != null) pdwEffect.setValue(paths.isEmpty() ? DROPEFFECT_NONE : DROPEFFECT_COPY);
            return S_OK;
        };

        vtable.setPointer(0L, CallbackReference.getFunctionPointer(qi));
        vtable.setPointer((long) ps, CallbackReference.getFunctionPointer(addRef));
        vtable.setPointer(2L * ps, CallbackReference.getFunctionPointer(release));
        vtable.setPointer(3L * ps, CallbackReference.getFunctionPointer(dragEnter));
        vtable.setPointer(4L * ps, CallbackReference.getFunctionPointer(dragOver));
        vtable.setPointer(5L * ps, CallbackReference.getFunctionPointer(dragLeave));
        vtable.setPointer(6L * ps, CallbackReference.getFunctionPointer(drop));
        object.setPointer(0L, vtable);
    }

    /** 返回 COM 对象指针，用于 RegisterDragDrop。 */
    Pointer handle() {
        return object;
    }

    /** Windows POINTL 结构体（按值传递，用于 IDropTarget 的 pt 参数）。 */
    @FieldOrder({"x", "y"})
    public static class POINTL extends Structure implements Structure.ByValue {
        public int x;
        public int y;
    }

    // ---- COM callback 接口（stdcall）----

    @FunctionalInterface
    interface QueryInterfaceCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, Pointer riid, PointerByReference ppv);
    }

    @FunctionalInterface
    interface AddRefCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self);
    }

    @FunctionalInterface
    interface ReleaseCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self);
    }

    @FunctionalInterface
    interface DragEnterCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, Pointer dataObj, int grfKeyState, POINTL pt, IntByReference pdwEffect);
    }

    @FunctionalInterface
    interface DragOverCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, int grfKeyState, POINTL pt, IntByReference pdwEffect);
    }

    @FunctionalInterface
    interface DragLeaveCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self);
    }

    @FunctionalInterface
    interface DropCb extends StdCallLibrary.StdCallCallback {
        int invoke(Pointer self, Pointer dataObj, int grfKeyState, POINTL pt, IntByReference pdwEffect);
    }

    // ---- 辅助 ----

    /** POINTL(屏幕坐标) → 客户区坐标，返回 [x, y]。 */
    private int[] screenToClient(int sx, int sy) {
        Memory p = new Memory(8);
        p.setInt(0, sx);
        p.setInt(4, sy);
        try {
            User32Lib.INSTANCE.ScreenToClient(java.util.Objects.requireNonNull(DragDropManager.INSTANCE.getHwnd()), p);
        } catch (Throwable t) {
            LOGGER.warn("ScreenToClient failed", t);
        }
        return new int[]{p.getInt(0), p.getInt(4)};
    }

    /** IDataObject::QueryGetData(CF_HDROP)，判断是否含文件。 */
    private boolean queryHasHdrop(Pointer dataObj) {
        if (dataObj == null) return false;
        try {
            Memory fmt = formatetcHdrop();
            Pointer vtbl = dataObj.getPointer(0);
            // QueryGetData = vtable 索引 5
            Pointer fn = vtbl.getPointer(5L * ps);
            Function f = Function.getFunction(fn, Function.ALT_CONVENTION);
            int hr = f.invokeInt(new Object[]{dataObj, fmt});
            LOGGER.debug("QueryGetData hr=0x{}", Integer.toHexString(hr));
            return hr == S_OK;
        } catch (Throwable t) {
            LOGGER.warn("QueryGetData failed", t);
            return false;
        }
    }

    /** IDataObject::GetData(CF_HDROP) → 解析 HDROP 文件路径。 */
    private List<String> extractPaths(Pointer dataObj) {
        if (dataObj == null) return List.of();
        Memory fmt = formatetcHdrop();
        Memory medium = new Memory(24); // STGMEDIUM（64 位：tymed+pad+hGlobal+pUnk）
        try {
            Pointer vtbl = dataObj.getPointer(0);
            Pointer fn = vtbl.getPointer(3L * ps); // GetData = 索引 3
            Function f = Function.getFunction(fn, Function.ALT_CONVENTION);
            int hr = f.invokeInt(new Object[]{dataObj, fmt, medium});
            if (hr != S_OK) {
                LOGGER.debug("GetData returned hr=0x{}", Integer.toHexString(hr));
                return List.of();
            }
            Pointer hGlobal = medium.getPointer(8);
            if (hGlobal == null) return List.of();
            Pointer hDrop = Kernel32Lib.INSTANCE.GlobalLock(hGlobal);
            if (hDrop == null) return List.of();
            try {
                return readHdropPaths(hDrop);
            } finally {
                Kernel32Lib.INSTANCE.GlobalUnlock(hGlobal);
            }
        } catch (Throwable t) {
            LOGGER.warn("extractPaths failed", t);
            return List.of();
        } finally {
            try {
                Ole32.INSTANCE.ReleaseStgMedium(medium);
            } catch (Throwable ignored) {
                // ReleaseStgMedium 失败不致命
            }
        }
    }

    @SuppressWarnings("NullAway") // JNA 惯例：null buffer = 查询长度（Win32 契约），null 参数均在契约内
    private List<String> readHdropPaths(Pointer hDrop) {
        List<String> paths = new ArrayList<>();
        int count = Shell32Lib.INSTANCE.DragQueryFileW(hDrop, 0xFFFFFFFF, (char[]) null, 0);
        for (int i = 0; i < count; i++) {
            int len = Shell32Lib.INSTANCE.DragQueryFileW(hDrop, i, (char[]) null, 0);
            if (len <= 0) continue;
            char[] wbuf = new char[len + 1];
            Shell32Lib.INSTANCE.DragQueryFileW(hDrop, i, wbuf, wbuf.length);
            int utf8Len = Kernel32Lib.INSTANCE.WideCharToMultiByte(
                    CP_UTF8, 0, wbuf, len, (byte[]) null, 0, null, null);
            if (utf8Len <= 0) continue;
            byte[] out = new byte[utf8Len];
            Kernel32Lib.INSTANCE.WideCharToMultiByte(
                    CP_UTF8, 0, wbuf, len, out, out.length, null, null);
            paths.add(new String(out, 0, utf8Len, StandardCharsets.UTF_8));
        }
        return paths;
    }

    private Memory formatetcHdrop() {
        // FORMATETC（64 位 32 字节，含对齐 padding；32 位 20 字节也兼容此布局范围）
        Memory fmt = new Memory(32);
        fmt.clear();
        fmt.setShort(0, (short) CF_HDROP); // cfFormat
        // ptd @ 8 = null（已 clear）
        fmt.setInt(16, DVASPECT_CONTENT); // dwAspect
        fmt.setInt(20, -1); // lindex
        fmt.setInt(24, TYMED_HGLOBAL); // tymed
        return fmt;
    }
}
