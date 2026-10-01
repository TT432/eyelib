package io.github.tt432.eyelib.bridge.client.dnd.adapter;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Windows 原生拖放相关 Win32 API 声明（仅 Windows 加载，由 {@link DragDropManager} 在非 Windows 平台不调用）。
 * <p>使用 JNA StdCallLibrary（COM/Win32 为 stdcall 约定），仅依赖 JNA 核心 jar。
 */
final class WinNative {
    private WinNative() {
    }

    static final int S_OK = 0;
    static final int E_INVALIDARG = 0x80070057;
    static final int E_NOINTERFACE = 0x80004002;

    static final int CF_HDROP = 15;
    static final int DVASPECT_CONTENT = 1;
    static final int TYMED_HGLOBAL = 1;
    static final int DROPEFFECT_NONE = 0;
    static final int DROPEFFECT_COPY = 1;
    static final int CP_UTF8 = 65001;

    /**
     * IID 字节序（小端 Data1/Data2/Data3，Data4 原序）。
     * <li>IID_IUnknown = 00000000-0000-0000-C000-000000000046
     * <li>IID_IDropTarget = 00000122-0000-0000-C000-000000000046
     */
    static final byte[] IID_IUNKNOWN = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46
    };
    static final byte[] IID_IDROPTARGET = {
            0x22, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46
    };

    /** 比较 REFIID(Pointer, 16 字节) 与目标 byte[16]。 */
    static boolean refEquals(Pointer riid, byte[] target) {
        if (riid == null) return false;
        byte[] got = riid.getByteArray(0, 16);
        if (got.length != target.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (got[i] != target[i]) return false;
        }
        return true;
    }

    public interface Ole32 extends StdCallLibrary {
        Ole32 INSTANCE = Native.load("ole32", Ole32.class);

        int OleInitialize(Pointer reserved);

        int RegisterDragDrop(Pointer hwnd, Pointer dropTarget);

        int RevokeDragDrop(Pointer hwnd);

        void ReleaseStgMedium(Pointer medium);
    }

    public interface User32Lib extends StdCallLibrary {
        User32Lib INSTANCE = Native.load("user32", User32Lib.class);

        boolean ScreenToClient(Pointer hwnd, Pointer point);
    }

    public interface Kernel32Lib extends StdCallLibrary {
        Kernel32Lib INSTANCE = Native.load("kernel32", Kernel32Lib.class);

        Pointer GlobalLock(Pointer hMem);

        boolean GlobalUnlock(Pointer hMem);

        int WideCharToMultiByte(int codePage, int dwFlags,
                                char[] lpWideCharStr, int cchWideChar,
                                byte[] lpMultiByteStr, int cbMultiByte,
                                Pointer lpDefaultChar, Pointer lpUsedDefaultChar);
    }

    public interface Shell32Lib extends StdCallLibrary {
        Shell32Lib INSTANCE = Native.load("shell32", Shell32Lib.class);

        int DragQueryFileW(Pointer hDrop, int iFile, char[] lpszFile, int cch);
    }
}
