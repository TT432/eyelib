"""
eyelib-debug MCP Server

Wraps AIDebugServer HTTP API + RenderDoc capture for MC client debugging.
Stateless — all game state is read from the server in real time, not tracked internally.

Game lifecycle: start → loading → loaded → in_world → (execute/capture) → close
"""

import asyncio
import json
import os
import shutil
import subprocess
import time
import urllib.request
import urllib.error
from typing import Optional, Callable

from mcp.server.fastmcp import FastMCP


# ── Configuration ──────────────────────────────────────────────

DEBUG_URL = "http://localhost:25999"
PROJECT_DIR = r"E:\_ideaProjects\qylEyelib"
RENDERDOC_CMD = r"E:\RenderDoc\renderdoccmd.exe"
RUN_CLIENT_CMD = r"E:\_ideaProjects\qylEyelib\build\moddev\runClient.cmd"
CAPTURE_PREFIX = "eyelib_capture"
CLIENT_RUN_TASKS = "prepareClientRun writeClientLegacyClasspath createClientLaunchScript"

# runtime state (minimal, just process handle for cleanup)
_proc = None
_launch_time: float = 0.0


def _tail_text(text: str, max_chars: int = 3000) -> str:
    if len(text) <= max_chars:
        return text
    return text[-max_chars:]


def _read_argfile(path: str) -> tuple[str, str]:
    with open(path, "r", encoding="utf-8") as f:
        lines = f.read().splitlines()
    if len(lines) < 2:
        raise ValueError(f"Invalid argfile: {path}")
    return lines[0], lines[1]


def _run_artifacts() -> list[str]:
    moddev_dir = os.path.join(PROJECT_DIR, "build", "moddev")
    return [
        os.path.join(moddev_dir, "runClient.cmd"),
        os.path.join(moddev_dir, "clientRunClasspath.txt"),
        os.path.join(moddev_dir, "clientRunVmArgs.txt"),
        os.path.join(moddev_dir, "clientRunProgramArgs.txt"),
        os.path.join(moddev_dir, "clientLegacyClasspath.txt"),
    ]


def _format_artifact_status() -> str:
    lines = []
    for path in _run_artifacts():
        if not os.path.exists(path):
            lines.append(f"missing  {path}")
            continue
        mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(os.path.getmtime(path)))
        lines.append(f"{mtime}  {os.path.getsize(path):>8}  {path}")
    return "\n".join(lines)


def _validate_run_artifacts() -> Optional[str]:
    missing = [path for path in _run_artifacts() if not os.path.exists(path)]
    if missing:
        return (
            "Missing ModDev run artifact(s). Run eyelib_debug_build first.\n"
            + "\n".join(f"  {path}" for path in missing)
        )

    classpath_header, classpath_value = _read_argfile(
        os.path.join(PROJECT_DIR, "build", "moddev", "clientRunClasspath.txt")
    )
    if classpath_header != "-classpath" or not classpath_value.strip():
        return f"Invalid clientRunClasspath.txt header/value: {classpath_header!r}"

    vm_args_path = os.path.join(PROJECT_DIR, "build", "moddev", "clientRunVmArgs.txt")
    with open(vm_args_path, "r", encoding="utf-8") as f:
        vm_args = f.read()
    if "legacyClassPath.file" not in vm_args:
        return "clientRunVmArgs.txt does not contain legacyClassPath.file"

    return None


def _port_25999_occupied() -> bool:
    result = subprocess.run(
        ["cmd.exe", "/c", "netstat -ano | findstr 25999"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=10,
    )
    return result.returncode == 0 and bool(result.stdout.strip())


def _classpath_duplicate_report() -> str:
    run_cp_path = os.path.join(PROJECT_DIR, "build", "moddev", "clientRunClasspath.txt")
    legacy_cp_path = os.path.join(PROJECT_DIR, "build", "moddev", "clientLegacyClasspath.txt")

    header, classpath = _read_argfile(run_cp_path)
    if header != "-classpath":
        raise ValueError(f"Unsupported classpath argfile header: {header}")

    run_entries = [p.strip() for p in classpath.split(os.pathsep) if p.strip()]
    legacy_names = {
        os.path.basename(p.strip()).lower()
        for p in open(legacy_cp_path, "r", encoding="utf-8").read().splitlines()
        if p.strip() and os.path.basename(p.strip()).lower().endswith(".jar")
    }
    duplicates = [
        p for p in run_entries
        if os.path.basename(p).lower() in legacy_names
    ]

    sample = ", ".join(os.path.basename(p) for p in duplicates[:12])
    if len(duplicates) > 12:
        sample += f", ... +{len(duplicates) - 12}"
    report = (
        f"Run classpath diagnostic: {len(run_entries)} entries; "
        f"{len(duplicates)} jar basename(s) also appear in legacy classpath"
    )
    if sample:
        report += f": {sample}"
    return report


mcp = FastMCP(
    "eyelib-debug",
    instructions="""# Eyelib Debug MCP Server

Controls a Minecraft Forge client running under RenderDoc capture.
All state is read from the AIDebugServer endpoints in real time — no cached state.
""",
)


# ── HTTP helpers ────────────────────────────────────────────────

async def _http_get(path: str) -> Optional[dict]:
    def _do() -> Optional[dict]:
        try:
            req = urllib.request.Request(f"{DEBUG_URL}{path}")
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
                f"{DEBUG_URL}{path}",
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
    """Poll path until check_fn returns True. Also checks /ping to detect crashes."""
    start = time.time()
    while time.time() - start < timeout:
        data = await _http_get(path)
        if data is not None and check_fn(data):
            elapsed = time.time() - start
            return True, f"{desc} succeeded after {elapsed:.0f}s"
        await asyncio.sleep(interval)
    return False, f"{desc} timed out after {timeout}s"


async def _process_exit_message(desc: str) -> Optional[str]:
    if _proc is None:
        return None
    try:
        await asyncio.wait_for(_proc.wait(), timeout=0.01)
    except asyncio.TimeoutError:
        return None

    stderr = ""
    if _proc.stderr is not None:
        try:
            data = await asyncio.wait_for(_proc.stderr.read(), timeout=1.0)
            stderr = data.decode("utf-8", errors="replace").strip()
        except (asyncio.TimeoutError, OSError):
            stderr = "<failed to read stderr>"
    if stderr:
        return f"{desc} exited early (exit={_proc.returncode}). stderr tail:\n{_tail_text(stderr)}"
    return f"{desc} exited early (exit={_proc.returncode}). No stderr captured."


async def _poll_ping_until_ready_or_exit(timeout: float, interval: float = 2.0) -> tuple[bool, str]:
    start = time.time()
    early_exit_msg = None
    while time.time() - start < timeout:
        if early_exit_msg is None:
            early_exit_msg = await _process_exit_message("RenderDoc wrapper")

        ping = await _http_get("/ping")
        if ping is not None and ping.get("status") == "ok":
            elapsed = time.time() - start
            if early_exit_msg is not None:
                return True, f"Client startup succeeded after {elapsed:.0f}s ({early_exit_msg}; client kept running)"
            return True, f"Client startup succeeded after {elapsed:.0f}s"

        await asyncio.sleep(interval)

    if early_exit_msg is not None:
        return False, early_exit_msg
    return False, f"Client startup timed out after {timeout}s"


async def _poll_until_loaded(
    timeout: float = 120.0,
    interval: float = 3.0,
    check_process_exit: bool = False,
) -> tuple[Optional[str], Optional[str]]:
    """
    Poll /loaded until true, periodically checking /ping for crashes.
    Tolerates brief ping failures (up to 10s) before declaring crash.
    Returns (None, None) on success, (error_type, error_msg) on failure.
    """
    start = time.time()
    consecutive_ping_fails = 0
    max_ping_fails = 3  # ~10s at 3s interval before declaring crash
    last_ping = None
    last_loaded = None

    while time.time() - start < timeout:
        if check_process_exit:
            exit_msg = await _process_exit_message("Client process")
            if exit_msg is not None:
                return "crash", exit_msg

        ping = await _http_get("/ping")
        last_ping = ping
        if ping is None:
            consecutive_ping_fails += 1
            if consecutive_ping_fails >= max_ping_fails:
                return "crash", f"Client unreachable for {consecutive_ping_fails * interval:.0f}s"
            await asyncio.sleep(interval)
            continue
        else:
            consecutive_ping_fails = 0

        loaded = await _http_get("/loaded")
        last_loaded = loaded
        if loaded and loaded.get("loaded") is True:
            elapsed = time.time() - start
            return None, f"Loaded after {elapsed:.0f}s"
        await asyncio.sleep(interval)

    return "timeout", f"Loading timed out after {timeout}s (last ping={last_ping}, last loaded={last_loaded})"


async def _poll_until_gone(timeout: float = 30.0) -> bool:
    """Poll until port 25999 becomes unreachable (game shut down)."""
    start = time.time()
    while time.time() - start < timeout:
        ping = await _http_get("/ping")
        if ping is None:
            return True
        await asyncio.sleep(0.5)
    return False


# ── Server state queries (stateless) ────────────────────────────

async def _read_game_state() -> dict:
    """Read the full game state from AIDebugServer endpoints."""
    ping = await _http_get("/ping")
    if ping is None:
        # Game was started but is unreachable → likely crashed
        if _proc is not None:
            return {"state": "crashed", "ping": False, "loaded": False, "in_world": False, "dimension": None}
        return {"state": "idle", "ping": False, "loaded": False, "in_world": False, "dimension": None}

    loaded_data = await _http_get("/loaded")
    loaded = loaded_data.get("loaded") is True if loaded_data else False

    if loaded:
        enter_data = await _http_get("/enterdworld")
        in_world = enter_data.get("inWorld") is True if enter_data else False
        dimension = enter_data.get("dimension") if enter_data else None
    else:
        in_world = False
        dimension = None

    if in_world:
        state = "in_world"
    elif loaded:
        state = "loaded"
    elif ping:
        state = "loading"
    else:
        state = "idle"

    return {
        "state": state,
        "ping": ping is not None,
        "loaded": loaded,
        "in_world": in_world,
        "dimension": dimension,
    }


def _state_summary(gs: dict) -> str:
    parts = [f"state={gs['state']}"]
    if gs["dimension"]:
        parts.append(f"dimension={gs['dimension']}")
    return " | ".join(parts)


# ── Tools ──────────────────────────────────────────────────────

@mcp.tool()
async def eyelib_debug_launch(timeout: int = 120) -> str:
    """
    Launch MC client under RenderDoc capture mode.
    Starts RenderDoc capture → polls /ping and /loaded until game is ready.

    Args:
        timeout: Max seconds to wait for client to load AFTER rebuild (default 120).
    """
    global _proc, _launch_time

    # Check if already running
    ping = await _http_get("/ping")
    if ping is not None:
        return "❌ Game is already running (port 25999 responds). Use eyelib_debug_close first."

    if not os.path.exists(RUN_CLIENT_CMD):
        return "❌ runClient.cmd is missing. Run eyelib_debug_build first."

    artifact_error = await asyncio.to_thread(_validate_run_artifacts)
    if artifact_error is not None:
        status = await asyncio.to_thread(_format_artifact_status)
        return f"❌ {artifact_error}\n\nRun artifacts:\n{status}"

    occupied = await asyncio.to_thread(_port_25999_occupied)
    if occupied:
        return "❌ Port 25999 is occupied but /ping does not respond. Close the old process or restart it outside MCP."

    # Launch
    try:
        classpath_report = await asyncio.to_thread(_classpath_duplicate_report)
        _proc = await asyncio.create_subprocess_exec(
            RENDERDOC_CMD, "capture",
            "-c", CAPTURE_PREFIX, "--opt-hook-children",
            RUN_CLIENT_CMD,
            cwd=PROJECT_DIR,
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.PIPE,
        )
        _launch_time = time.time()
    except Exception as e:
        return f"❌ Launch failed: {e}"

    # Wait for /ping
    ok, msg = await _poll_ping_until_ready_or_exit(timeout=timeout, interval=2)
    if not ok:
        return f"❌ {msg}"

    # Wait for /loaded, checking /ping for crashes
    err_type, err_msg = await _poll_until_loaded(
        timeout=timeout - (time.time() - _launch_time),
        interval=3,
    )
    if err_type == "crash":
        return f"❌ Game crashed during loading. {err_msg}"
    elif err_type == "timeout":
        return f"❌ Loading timed out. {err_msg}"

    # Final ping sanity check
    ping = await _http_get("/ping")
    if ping is None:
        return "❌ Client became unreachable right after loading (crashed)."

    return f"✅ Client ready at title screen. Use eyelib_debug_enter_world to start.\n{classpath_report}"


@mcp.tool()
async def eyelib_debug_enter_world(world_name: str = "Debug World", timeout: int = 60) -> str:
    """
    Enter (or create) a singleplayer world.
    Calls /enterworld → polls /enterdworld → auto-unpauses.

    Args:
        world_name: World name (default "Debug World").
        timeout: Max seconds to wait for world to load (default 60).
    """
    gs = await _read_game_state()
    if gs["state"] != "loaded":
        return f"❌ Game must be at title screen (loaded) to enter world. Currently: {gs['state']}. {_state_summary(gs)}"

    result = await _http_post("/enterworld", world_name)
    if result is None or not result.get("success", False):
        return f"❌ /enterworld failed. {_state_summary(gs)}"

    ok, msg = await _poll_until(
        "/enterdworld", lambda d: d.get("inWorld") is True,
        timeout=timeout, interval=2, desc="World enter",
    )
    if not ok:
        return f"❌ {msg}"

    # Read dimension and unpause
    gs = await _read_game_state()
    await _http_post("/eval",
        'net.minecraft.client.Minecraft.getInstance().setScreen(null); return "unpaused";')

    return f"✅ In world ({gs.get('dimension', 'N/A')}). Ready for code execution."


@mcp.tool()
async def eyelib_debug_execute(code: str) -> str:
    """
    Execute Java code in the running MC client via /eval.
    Code is a Java method body (no class wrapper needed).

    Args:
        code: Java method body to execute.
    """
    gs = await _read_game_state()
    if gs["state"] != "in_world":
        return f"❌ Game must be in a world to execute code. Currently: {gs['state']}. {_state_summary(gs)}"

    result = await _http_post("/eval", code)
    if result is None:
        return f"❌ /eval HTTP failed — client may have crashed."

    if result.get("success"):
        return f"✅ {result.get('result', 'ok')}"
    else:
        return f"❌ {result.get('error', 'unknown error')}"


@mcp.tool()
async def eyelib_debug_capture_frame() -> str:
    """Capture a single RenderDoc frame. Requires in_world state."""
    gs = await _read_game_state()
    if gs["state"] != "in_world":
        return f"❌ Game must be in a world to capture. Currently: {gs['state']}. {_state_summary(gs)}"

    r = await _http_post("/eval",
        'io.github.tt432.eyelib.common.debug.RenderDocCapturer.startCapture(); return "started";')
    if r is None or not r.get("success"):
        return "❌ startCapture failed — RenderDoc not available."

    await asyncio.sleep(0.5)

    r2 = await _http_post("/eval",
        'boolean _cr = io.github.tt432.eyelib.common.debug.RenderDocCapturer.endCapture();'
        ' return _cr ? "captured=true" : "captured=false";')
    if r2 is None or not r2.get("success"):
        return "❌ endCapture failed."

    if "captured=true" in (r2.get("result", "")):
        return f"✅ Frame captured. File: run/{CAPTURE_PREFIX}_*.rdc"
    else:
        return "❌ Capture returned false."


@mcp.tool()
async def eyelib_debug_status(info: str = "summary") -> str:
    """
    Get current debug session state from AIDebugServer.
    All state is read fresh — no cached values.

    Args:
        info: "summary" (default) or "all" for full detail.
    """
    gs = await _read_game_state()

    if info == "all":
        return (
            f"State: {gs['state']}\n"
            f"Ping: {'✅' if gs['ping'] else '❌'}\n"
            f"Loaded: {gs['loaded']}\n"
            f"In World: {gs['in_world']}\n"
            f"Dimension: {gs['dimension'] or 'N/A'}"
        )

    return _state_summary(gs)


@mcp.tool()
async def eyelib_debug_close() -> str:
    """
    Close the MC client via /close.
    Waits for port 25999 to become unreachable before returning.
    """
    global _proc, _launch_time

    ping = await _http_get("/ping")
    if ping is None:
        return "Already idle (no game running)."

    await _http_post("/close")

    # Wait for shutdown, then force kill if needed
    gone = await _poll_until_gone(timeout=30)
    if not gone and _proc and _proc.returncode is None:
        _proc.kill()
        try:
            await asyncio.wait_for(_proc.wait(), timeout=5)
        except asyncio.TimeoutError:
            pass

    _proc = None
    _launch_time = 0.0

    if gone:
        return "✅ Client stopped."
    else:
        return "⚠️ Client force-stopped (did not shut down within 30s)."


@mcp.tool()
async def eyelib_debug_clientsmoke(timeout: int = 120) -> str:
    """
    Run clientsmoke tests in a plain MC client (no RenderDoc).
    Rebuilds → starts MC with clientsmoke.enabled=true → waits for tests → parses report.

    The client auto-exits after tests finish (clientsmoke.autoExit=true).
    Args:
        timeout: Max seconds to wait for tests to complete (default 120).
    """
    global _proc, _launch_time

    ping = await _http_get("/ping")
    if ping is not None:
        return "❌ Game is already running (port 25999 responds). Use eyelib_debug_close first."

    # rebuild
    def _rebuild():
        return subprocess.run(
            ["cmd.exe", "/c",
             f"cd /d {PROJECT_DIR} && "
             f"gradlew.bat :compileJava {CLIENT_RUN_TASKS} --no-configuration-cache"],
            cwd=PROJECT_DIR,
            capture_output=True, text=True, timeout=300,
        )
    rebuild_result = await asyncio.to_thread(_rebuild)
    if rebuild_result.returncode != 0:
        return "❌ Clientsmoke build failed:\n" + _tail_text(rebuild_result.stdout + "\n" + rebuild_result.stderr)

    artifact_error = await asyncio.to_thread(_validate_run_artifacts)
    if artifact_error is not None:
        status = await asyncio.to_thread(_format_artifact_status)
        return f"❌ {artifact_error}\n\nRun artifacts:\n{status}"

    occupied = await asyncio.to_thread(_port_25999_occupied)
    if occupied:
        return "❌ Port 25999 is occupied but /ping does not respond. Close the old process or restart it outside MCP."

    # inject clientsmoke JVM args
    vm_args_path = os.path.join(PROJECT_DIR, "build", "moddev", "clientRunVmArgs.txt")
    def _inject():
        with open(vm_args_path, "a", encoding="utf-8") as f:
            f.write("\n-Dclientsmoke.enabled=true\n-Dclientsmoke.autoExit=true\n")
    await asyncio.to_thread(_inject)

    # launch (no RenderDoc)
    try:
        _proc = await asyncio.create_subprocess_exec(
            "cmd.exe", "/c", RUN_CLIENT_CMD,
            cwd=PROJECT_DIR,
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.PIPE,
        )
        _launch_time = time.time()
    except Exception as e:
        return f"❌ Launch failed: {e}"

    # wait for /ping
    ok, msg = await _poll_until(
        "/ping", lambda d: d.get("status") == "ok",
        timeout=min(timeout, 60), interval=2, desc="Client startup",
    )
    if not ok:
        return f"❌ Client failed to start: {msg}"

    # wait for clientsmoke completion (report file or port close)
    report_dir = os.path.join(PROJECT_DIR, "run", "clientsmoke-reports")
    start = time.time()
    consecutive_ping_fails = 0

    while time.time() - start < timeout:
        ping = await _http_get("/ping")
        if ping is None:
            consecutive_ping_fails += 1
            if consecutive_ping_fails >= 5:
                break
        else:
            consecutive_ping_fails = 0

        try:
            json_files = sorted(
                [f for f in os.listdir(report_dir) if f.startswith("report-") and f.endswith(".json")],
                reverse=True)
            if json_files:
                rp = os.path.join(report_dir, json_files[0])
                if time.time() - os.path.getmtime(rp) > 2:
                    await asyncio.sleep(3)
                    break
        except (FileNotFoundError, OSError):
            pass
        await asyncio.sleep(2)

    # stop if still running
    ping = await _http_get("/ping")
    if ping is not None:
        await _http_post("/close")
        await _poll_until_gone(timeout=15)

    _proc = None
    _launch_time = 0.0

    # parse report
    try:
        json_files = sorted(
            [f for f in os.listdir(report_dir) if f.startswith("report-") and f.endswith(".json")],
            reverse=True)
        if not json_files:
            return "⚠️ No report file found. Clientsmoke may not have run."

        with open(os.path.join(report_dir, json_files[0]), "r", encoding="utf-8") as f:
            report = json.load(f)

        total = report.get("totalTests", 0)
        passed = report.get("passed", 0)
        failed = report.get("failed", 0)
        ts = report.get("timestamp", "?")

        lines = [f"📋 Clientsmoke Report ({ts})",
                 f"   Total: {total}  |  ✅ Passed: {passed}  |  ❌ Failed: {failed}"]
        for e in report.get("entries", []):
            icon = "✅" if e.get("status") == "passed" else "❌"
            name = e.get("className", "?").split(".")[-1]
            lines.append(f"   {icon} {name} ({e.get('durationMs', 0)}ms) — {e.get('description', '')}")
            if e.get("status") == "failed" and e.get("error"):
                lines.append(f"      ↳ {e['error'].get('message', '?')}")

        if failed > 0:
            lines.append(f"\n❌ {failed} test(s) FAILED")
        else:
            lines.append(f"\n✅ All {passed} test(s) PASSED")
        return "\n".join(lines)
    except (FileNotFoundError, OSError, json.JSONDecodeError) as e:
        return f"⚠️ Failed to read report: {e}"


@mcp.tool()
async def eyelib_debug_nullaway(module: str = "") -> str:
    """
    Run NullAway/Error Prone nullness checks on eyelib sources.
    Compiles with NullAway enabled and returns violations.

    Args:
        module: Specific Gradle subproject to check (e.g. 'eyelib-material').
                Omit to run root :nullawayMain which checks all sources.
    """
    global _proc

    target = f":{module}:nullawayMain" if module else ":nullawayMain"
    desc = f"NullAway ({target})"

    def _run() -> subprocess.CompletedProcess:
        return subprocess.run(
            ["cmd.exe", "/c",
             f"cd /d E:\\\\_ideaProjects\\\\qylEyelib && "
             f"gradlew.bat {target} --no-configuration-cache"],
            cwd=PROJECT_DIR,
            capture_output=True, text=True, timeout=300,
        )

    try:
        result = await asyncio.to_thread(_run)
    except subprocess.TimeoutExpired:
        return f"⏰ {desc} timed out after 300s"

    stdout = result.stdout
    stderr = result.stderr

    # Extract NullAway violations from output (Error Prone format: file:line: error: ...)
    violations = []
    for line in (stdout + "\n" + stderr).split("\n"):
        stripped = line.strip()
        if "error:" in stripped and ("NullAway" in stripped or "[NullAway]" in stripped):
            violations.append(stripped)
        elif stripped.startswith("warning:") and "NullAway" in stripped:
            violations.append(stripped)

    if result.returncode == 0:
        return f"✅ {desc} — no NullAway violations"
    elif violations:
        lines = [f"❌ {desc} — {len(violations)} violation(s):", ""]
        for v in violations[:30]:
            lines.append(f"   {v}")
        if len(violations) > 30:
            lines.append(f"   ... and {len(violations) - 30} more")
        return "\n".join(lines)
    else:
        # Non-NullAway compile errors
        error_lines = []
        for line in (stdout + "\n" + stderr).split("\n"):
            if "error:" in line.strip() or "BUILD FAILED" in line.strip():
                error_lines.append(line.strip())
        return f"❌ {desc} — build failed (non-NullAway errors):\n" + "\n".join(error_lines[:20])


@mcp.tool()
async def eyelib_debug_build(module: str = "") -> str:
    """
    Build one or all eyelib modules using Windows gradlew.bat.
    After build, refreshes client run artifacts generated by ModDevGradle.

    Args:
        module: Subproject name (e.g. 'eyelib-bridge', 'eyelib-molang').
                Omit or use 'all' to build all modules.
    """
    if module and module.lower() != "all":
        target = f":{module}:jar {CLIENT_RUN_TASKS}"
        desc = f"Build {module}"
    else:
        target = f"jar {CLIENT_RUN_TASKS}"
        desc = "Build all modules"

    def _run() -> subprocess.CompletedProcess:
        return subprocess.run(
            ["cmd.exe", "/c",
             "cd /d E:\\\\_ideaProjects\\\\qylEyelib && "
             f"gradlew.bat {target} --no-configuration-cache"],
            cwd=PROJECT_DIR,
            capture_output=True, text=True, timeout=300,
        )

    try:
        result = await asyncio.to_thread(_run)
    except subprocess.TimeoutExpired:
        return f"⏰ {desc} timed out after 300s"

    if result.returncode == 0:
        # Count tasks
        executed = sum(1 for line in result.stdout.split("\n") if " executed" in line and "up-to-date" not in line.lower())
        uptodate = sum(1 for line in result.stdout.split("\n") if "up-to-date" in line.lower())
        artifact_error = _validate_run_artifacts()
        status = _format_artifact_status()
        if artifact_error is not None:
            return (
                f"⚠️ {desc} — BUILD SUCCESSFUL ({executed} executed, {uptodate} up-to-date), "
                f"but run artifact validation failed: {artifact_error}\n\nRun artifacts:\n{status}"
            )
        return (
            f"✅ {desc} — BUILD SUCCESSFUL ({executed} executed, {uptodate} up-to-date)\n"
            f"Refreshed tasks: {CLIENT_RUN_TASKS}\n\nRun artifacts:\n{status}"
        )
    else:
        error_lines = []
        for line in (result.stdout + "\n" + result.stderr).split("\n"):
            stripped = line.strip()
            if stripped and ("error:" in stripped.lower() or "BUILD FAILED" in stripped or "FAILURE" in stripped):
                error_lines.append(stripped)
        return f"❌ {desc} — BUILD FAILED\n" + "\n".join(error_lines[:20])


# ── Resource ───────────────────────────────────────────────────

@mcp.resource(uri="debug://session", name="DebugSession")
async def session_resource() -> str:
    """Current debug session state (JSON)."""
    gs = await _read_game_state()
    return json.dumps({
        "state": gs["state"],
        "client_reachable": gs["ping"],
        "client_loaded": gs["loaded"],
        "client_in_world": gs["in_world"],
        "client_dimension": gs["dimension"],
    }, indent=2)


# ── Entry ──────────────────────────────────────────────────────

if __name__ == "__main__":
    mcp.run()
