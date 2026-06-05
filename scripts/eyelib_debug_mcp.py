"""
eyelib-debug MCP Server

Wraps AIDebugServer HTTP API + RenderDoc capture for MC client debugging.
State machine ensures correct sequencing across tool calls.

States: idle → launching → loaded → entering_world → in_world → (execute/capture) → closing → idle
"""

import asyncio
import json
import os
import shutil
import subprocess
import time
import urllib.request
import urllib.error
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Callable

from mcp.server.fastmcp import FastMCP


# ── State Machine ──────────────────────────────────────────────

class S(str, Enum):
    IDLE = "idle"
    LAUNCHING = "launching"
    LOADED = "loaded"
    ENTERING_WORLD = "entering_world"
    IN_WORLD = "in_world"
    CLOSING = "closing"
    ERROR = "error"


@dataclass
class Session:
    state: S = S.IDLE
    error: str = ""
    dimension: str = "N/A"
    capture_prefix: str = "eyelib_capture"

    # Windows paths (hardcoded for this project)
    PROJECT_DIR: str = r"E:\_ideaProjects\qylEyelib"
    RENDERDOC_CMD: str = r"E:\RenderDoc\renderdoccmd.exe"
    RUN_CLIENT_CMD: str = r"E:\_ideaProjects\qylEyelib\build\moddev\runClient.cmd"

    DEBUG_URL: str = "http://localhost:25999"

    _proc = None
    _launch_time: float = 0.0

    def summary(self) -> str:
        parts = [f"[state: {self.state.value}]"]
        if self.error:
            parts.append(f"error: {self.error}")
        if self.dimension != "N/A":
            parts.append(f"dimension: {self.dimension}")
        if self._launch_time:
            elapsed = time.time() - self._launch_time
            parts.append(f"uptime: {elapsed:.0f}s")
        return " | ".join(parts)


session = Session()

mcp = FastMCP(
    "eyelib-debug",
    instructions="""# Eyelib Debug MCP Server

Controls a Minecraft Forge client running under RenderDoc capture,
for debugging eyelib rendering.

## State machine
- **idle** — No client running. Call `eyelib_debug_launch()`.
- **launching** — Client starting up, polling /ping → /loaded.
- **loaded** — Client at title screen. Call `eyelib_debug_enter_world()`.
- **entering_world** — Creating/entering world, polling /enterdworld.
- **in_world** — In-game. Execute code, capture RenderDoc frames.
- **closing** — Client shutting down.
- **error** — Something went wrong. Call `eyelib_debug_close()` to reset.

## Notes
- World auto-pauses after enter. Server auto-unpauses.
- Each tool return includes current state. Call `eyelib_debug_status()` any time.
- Zombie detection: if `eyelib_debug_status` can't reach the client, state → error.
""",
)


# ── HTTP helpers (sync wrapped in threads) ─────────────────────

async def _http_get(path: str) -> Optional[dict]:
    def _do() -> Optional[dict]:
        try:
            req = urllib.request.Request(f"{session.DEBUG_URL}{path}")
            with urllib.request.urlopen(req, timeout=5) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except (urllib.error.URLError, OSError, json.JSONDecodeError):
            return None
    return await asyncio.to_thread(_do)


async def _http_post(path: str, body: str = "") -> Optional[dict]:
    def _do() -> Optional[dict]:
        try:
            data = body.encode("utf-8") if body else b""
            req = urllib.request.Request(
                f"{session.DEBUG_URL}{path}",
                data=data, method="POST",
                headers={"Content-Type": "text/plain; charset=utf-8"},
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except (urllib.error.URLError, OSError, json.JSONDecodeError):
            return None
    return await asyncio.to_thread(_do)


async def _poll_until(
    path: str,
    check_fn: Callable[[dict], bool],
    timeout: float = 60.0,
    interval: float = 2.0,
    desc: str = "waiting",
) -> tuple[bool, str]:
    start = time.time()
    while time.time() - start < timeout:
        data = await _http_get(path)
        if data is not None and check_fn(data):
            elapsed = time.time() - start
            return True, f"{desc} succeeded after {elapsed:.0f}s"
        await asyncio.sleep(interval)
    return False, f"{desc} timed out after {timeout}s"


# ── Guard helper ───────────────────────────────────────────────

def _guard(*allowed: S) -> Optional[str]:
    if session.state not in allowed:
        return f"❌ Not allowed in state '{session.state.value}'. {session.summary()}"
    return None


# ── Tools ──────────────────────────────────────────────────────

@mcp.tool()
async def eyelib_debug_launch(timeout: int = 120) -> str:
    """
    Launch MC client under RenderDoc capture mode.
    Internally: kill zombie → renderdoccmd capture → poll /ping → poll /loaded.
    Guard: only in idle or error state.

    Args:
        timeout: Max seconds to wait for client to load (default 120).
    """
    g = _guard(S.IDLE, S.ERROR)
    if g:
        return g

    # Check if game is already running (MCP may have restarted independently)
    ping = await _http_get("/ping")
    if ping is not None:
        return f"❌ Game is already running (port 25999 responds). Close it first with eyelib_debug_close. {session.summary()}"

    # ── Pre-launch: rebuild via Windows-side gradlew to include source changes ──
    def _rebuild_moddev():
        subprocess.run(
            ["cmd.exe", "/c",
             "cd /d E:\\_ideaProjects\\qylEyelib && "
             "gradlew.bat :compileJava createLaunchScripts --no-configuration-cache"],
            cwd="/mnt/e/_ideaProjects/qylEyelib",
            capture_output=True, text=True, timeout=300,
        )
    await asyncio.to_thread(_rebuild_moddev)

    # Kill any zombie on port 25999
    def _kill_zombie():
        subprocess.run(
            ["cmd.exe", "/c",
             'for /f "tokens=5" %i in (\'netstat -ano ^| findstr 25999\')'
             " do (taskkill /F /PID %i >nul 2>&1)"],
            capture_output=True, text=True, timeout=10,
            cwd="/mnt/e",
        )
    await asyncio.to_thread(_kill_zombie)
    await asyncio.sleep(1)

    # Launch renderdoccmd capture
    async def _do_launch():
        return await asyncio.create_subprocess_exec(
            "/mnt/e/RenderDoc/renderdoccmd.exe", "capture",
            "-c", session.capture_prefix,
            "--opt-hook-children",
            "E:\\\\_ideaProjects\\\\qylEyelib\\\\build\\\\moddev\\\\runClient.cmd",
            cwd="/mnt/e/_ideaProjects/qylEyelib",
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.PIPE,
        )

    try:
        proc = await _do_launch()
        session._proc = proc  # type: ignore[assignment]
        session.state = S.LAUNCHING
        session._launch_time = time.time()
        session.error = ""
    except Exception as e:
        session.state = S.ERROR
        session.error = str(e)
        return f"❌ Launch failed: {e} {session.summary()}"

    # Poll /ping
    ok, msg = await _poll_until(
        "/ping", lambda d: d.get("status") == "ok",
        timeout=timeout, interval=2, desc="Client startup",
    )
    if not ok:
        # Check for fmlloader resolution error in stderr
        stderr_text = ""
        try:
            if proc.stderr:
                stderr_bytes = await asyncio.wait_for(proc.stderr.read(), timeout=3)
                stderr_text = stderr_bytes.decode("utf-8", errors="replace")
        except (asyncio.TimeoutError, OSError):
            pass

        if "Module fmlloader reads another module named fmlloader" in stderr_text:
            # Auto-fix: rebuild launch scripts with consistent classpath
            session.state = S.CLOSING
            if proc.returncode is None:
                proc.kill()
                await asyncio.sleep(2)

            def _fix():
                moddev = "/mnt/e/_ideaProjects/qylEyelib/build/moddev"
                if os.path.exists(moddev):
                    shutil.rmtree(moddev)
                subprocess.run(
                    ["/mnt/e/_ideaProjects/qylEyelib/gradlew", "createLaunchScripts",
                     "--no-configuration-cache"],
                    cwd="/mnt/e/_ideaProjects/qylEyelib",
                    capture_output=True, text=True, timeout=120,
                )
            await asyncio.to_thread(_fix)

            # Retry
            try:
                proc = await _do_launch()
                session._proc = proc  # type: ignore[assignment]
                session.state = S.LAUNCHING
                session._launch_time = time.time()
                session.error = ""
            except Exception as e:
                session.state = S.ERROR
                session.error = str(e)
                return f"❌ Retry failed: {e} {session.summary()}"

            ok, msg = await _poll_until(
                "/ping", lambda d: d.get("status") == "ok",
                timeout=timeout, interval=2, desc="Client startup (retry after fix)",
            )
            if not ok:
                session.state = S.ERROR
                session.error = msg
                return f"❌ {msg} (retry after fmlloader fix) {session.summary()}"
        else:
            session.state = S.ERROR
            session.error = msg
            return f"❌ {msg} {session.summary()}"

    # Poll /loaded
    ok, msg = await _poll_until(
        "/loaded", lambda d: d.get("loaded") is True,
        timeout=timeout - (time.time() - session._launch_time),
        interval=3, desc="Mod loading",
    )
    if not ok:
        session.state = S.ERROR
        session.error = msg
        return f"❌ {msg} {session.summary()}"

    session.state = S.LOADED
    return f"✅ Client ready at title screen. Enter world to start working. {session.summary()}"


@mcp.tool()
async def eyelib_debug_enter_world(world_name: str = "Debug World", timeout: int = 60) -> str:
    """
    Enter (or create) a singleplayer world.
    Calls /enterworld → polls /enterdworld → auto-unpauses.
    Guard: only in loaded state.

    Args:
        world_name: World name (default "Debug World").
        timeout: Max seconds to wait for world to load (default 60).
    """
    g = _guard(S.LOADED)
    if g:
        return g

    session.state = S.ENTERING_WORLD

    result = await _http_post("/enterworld", world_name)
    if result is None:
        session.state = S.ERROR
        session.error = "HTTP call to /enterworld failed"
        return f"❌ Failed to call /enterworld {session.summary()}"
    if not result.get("success", False):
        session.state = S.ERROR
        session.error = result.get("error", "unknown error")
        return f"❌ /enterworld failed: {session.error} {session.summary()}"

    ok, msg = await _poll_until(
        "/enterdworld", lambda d: d.get("inWorld") is True,
        timeout=timeout, interval=2, desc="World enter",
    )
    if not ok:
        session.state = S.ERROR
        session.error = msg
        return f"❌ {msg} {session.summary()}"

    # Read dimension
    data = await _http_get("/enterdworld")
    if data:
        session.dimension = data.get("dimension", "N/A")

    # Auto-unpause (mc.setScreen(null))
    await _http_post("/eval",
        'net.minecraft.client.Minecraft.getInstance().setScreen(null); return "unpaused";')

    session.state = S.IN_WORLD
    return f"✅ In world ({session.dimension}). Ready for code execution and captures. {session.summary()}"


@mcp.tool()
async def eyelib_debug_execute(code: str) -> str:
    """
    Execute Java code in the running MC client via /eval.
    Code is a Java method body (no class wrapper needed).
    
    Examples:
      'return \"hello\";'
      'net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();\nreturn mc.player.getName().getString();'
    
    Guard: only in in_world state.

    Args:
        code: Java method body to execute.
    """
    g = _guard(S.IN_WORLD)
    if g:
        return g

    result = await _http_post("/eval", code)
    if result is None:
        return f"❌ /eval HTTP failed — client may have crashed. {session.summary()}"

    if result.get("success"):
        return f"✅ {result.get('result', 'ok')} {session.summary()}"
    else:
        return f"❌ {result.get('error', 'unknown error')} {session.summary()}"


@mcp.tool()
async def eyelib_debug_capture_frame() -> str:
    """
    Capture a single RenderDoc frame.
    Calls RenderDocCapturer.startCapture() + endCapture() via /eval.
    Capture file saved as {capture_prefix}_N.rdc in run/ directory.
    Guard: only in in_world state.
    """
    g = _guard(S.IN_WORLD)
    if g:
        return g

    r = await _http_post("/eval",
        'io.github.tt432.eyelib.common.debug.RenderDocCapturer.startCapture(); return "started";')
    if r is None or not r.get("success"):
        return f"❌ startCapture failed — RenderDoc not available. {session.summary()}"

    await asyncio.sleep(0.5)

    r2 = await _http_post("/eval",
        'boolean _cr = io.github.tt432.eyelib.common.debug.RenderDocCapturer.endCapture();'
        ' return _cr ? "captured=true" : "captured=false";')
    if r2 is None or not r2.get("success"):
        return f"❌ endCapture failed. {session.summary()}"

    if "captured=true" in (r2.get("result", "")):
        return f"✅ Frame captured. File: run/{session.capture_prefix}_*.rdc {session.summary()}"
    else:
        return f"❌ Capture returned false. {session.summary()}"


@mcp.tool()
async def eyelib_debug_status(info: str = "summary") -> str:
    """
    Get current debug session state.
    Re-checks /ping and /loaded to detect client state changes.

    Args:
        info: "summary" (default) or "all" for full detail.
    """
    ping = await _http_get("/ping")
    if ping is None and session.state not in (S.IDLE, S.ERROR):
        session.state = S.ERROR
        session.error = "Client unreachable (port 25999)"

    if session.state == S.IN_WORLD:
        data = await _http_get("/enterdworld")
        if data:
            session.dimension = data.get("dimension", session.dimension)

    if info == "all":
        loaded_data = await _http_get("/loaded")
        enter_data = await _http_get("/enterdworld")
        lines = [
            f"State: {session.state.value}",
            f"Ping: {'✅' if ping else '❌'}",
            f"Loaded: {loaded_data.get('loaded', 'N/A') if loaded_data else 'N/A'}",
            f"In World: {enter_data.get('inWorld', 'N/A') if enter_data else 'N/A'}",
            f"Dimension: {session.dimension}",
        ]
        if session.error:
            lines.append(f"Error: {session.error}")
        return "\n".join(lines)

    return session.summary()


@mcp.tool()
async def eyelib_debug_close() -> str:
    """
    Close the MC client via /close.
    Resets state to idle. Safe to call from any non-idle state.
    """
    if session.state == S.IDLE:
        return f"Already idle. {session.summary()}"

    session.state = S.CLOSING
    await _http_post("/close")
    await asyncio.sleep(3)

    # Kill renderdoc process if still running
    if session._proc and session._proc.returncode is None:
        session._proc.kill()
        try:
            await asyncio.wait_for(session._proc.wait(), timeout=5)
        except asyncio.TimeoutError:
            pass
    session._proc = None

    # Reset
    session.state = S.IDLE
    session.error = ""
    session.dimension = "N/A"
    session._launch_time = 0.0

    return f"✅ Client stopped. {session.summary()}"


# ── Resource ───────────────────────────────────────────────────

@mcp.resource(uri="debug://session", name="DebugSession")
async def session_resource() -> str:
    """Current debug session state (JSON)."""
    ping = await _http_get("/ping")
    loaded_data = await _http_get("/loaded") if ping else None
    enter_data = await _http_get("/enterdworld") if loaded_data and loaded_data.get("loaded") else None

    return json.dumps({
        "state": session.state.value,
        "error": session.error or None,
        "recorded_dimension": session.dimension if session.dimension != "N/A" else None,
        "client_reachable": ping is not None,
        "client_loaded": loaded_data.get("loaded") if loaded_data else None,
        "client_in_world": enter_data.get("inWorld") if enter_data else None,
        "client_dimension": enter_data.get("dimension") if enter_data else None,
    }, indent=2)


# ── Entry ──────────────────────────────────────────────────────

if __name__ == "__main__":
    mcp.run()
