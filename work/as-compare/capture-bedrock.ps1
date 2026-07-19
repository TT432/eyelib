param([string]$Out = "bedrock.png")
Add-Type -AssemblyName System.Drawing
$proc = Get-Process -Name "Minecraft.Windows" -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if (-not $proc) { Write-Error "Minecraft.Windows window not found"; exit 1 }
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public struct POINT { public int X, Y; }
}
"@
$h = $proc.MainWindowHandle
[Win32]::ShowWindow($h, 9) | Out-Null  # SW_RESTORE
[Win32]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Milliseconds 500
if ([Win32]::IsIconic($h)) { Write-Error "window minimized"; exit 1 }
$cr = New-Object Win32+RECT
[Win32]::GetClientRect($h, [ref]$cr) | Out-Null
$pt = New-Object Win32+POINT
[Win32]::ClientToScreen($h, [ref]$pt) | Out-Null
$w = $cr.Right - $cr.Left; $hgt = $cr.Bottom - $cr.Top
Write-Output "window client: x=$($pt.X) y=$($pt.Y) w=$w h=$hgt title='$($proc.MainWindowTitle)'"
$bmp = New-Object System.Drawing.Bitmap $w, $hgt
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($pt.X, $pt.Y, 0, 0, (New-Object System.Drawing.Size $w, $hgt))
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output "saved: $Out"
