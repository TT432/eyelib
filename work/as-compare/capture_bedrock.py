"""后台截取基岩版 MC 窗口（Windows Graphics Capture，窗口被遮挡也可，最小化不行）。
用法: python capture_bedrock.py out.png
"""
import sys, subprocess, ctypes, time
from windows_capture import WindowsCapture, Frame, InternalCaptureControl

def find_bedrock_hwnd() -> int:
    ps = ("(Get-Process -Name 'Minecraft.Windows' -ErrorAction SilentlyContinue | "
          "Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1).MainWindowHandle")
    r = subprocess.run(["powershell", "-NoProfile", "-Command", ps],
                       capture_output=True, text=True, timeout=15)
    hwnd = int((r.stdout.strip() or "0").split()[-1] or 0)
    if not hwnd:
        raise SystemExit("Bedrock window not found (Minecraft.Windows)")
    # 若最小化则还原（不置前台）
    SW_RESTORE = 9
    if ctypes.windll.user32.IsIconic(hwnd):
        ctypes.windll.user32.ShowWindow(hwnd, SW_RESTORE)
        time.sleep(1)
    return hwnd

def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "bedrock_bg.png"
    hwnd = find_bedrock_hwnd()
    print(f"hwnd={hwnd}")
    done = {"ok": False}
    cap = WindowsCapture(cursor_capture=False, draw_border=False,
                         window_name=None, monitor_index=None, window_hwnd=hwnd)

    @cap.event
    def on_frame_arrived(frame: Frame, control: InternalCaptureControl):
        if not done["ok"]:
            frame.save_as_image(out)
            done["ok"] = True
            print(f"saved {out} {frame.width}x{frame.height}")
            control.stop()

    @cap.event
    def on_closed():
        print("closed")

    try:
        cap.start()
    except Exception as e:
        # start() 阻塞至 stop；正常路径在上面已 return
        print("start exc:", e)
    if not done["ok"]:
        raise SystemExit("no frame captured")

if __name__ == "__main__":
    main()
