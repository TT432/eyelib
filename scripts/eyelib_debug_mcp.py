"""
eyelib-debug MCP Server

Wraps AIDebugServer HTTP API for MC client debugging.
Stateless — all game state is read from the server in real time, not tracked internally.

Stonecutter 多版本支持：所有工具接受 version 参数（默认 "1.20.1"）。
Run artifacts 位于 versions/{version}/build/moddev/。
Gradle tasks 需要 :{version}: 前缀。
"""

import asyncio
import json
import os
import subprocess
import time
import urllib.request
import urllib.error
from typing import Optional, Callable

from mcp.server.fastmcp import FastMCP


MCP_VERSION = "1.6.0"


# ── Configuration ──────────────────────────────────────────────

DEBUG_URL = "http://localhost:25999"
PROJECT_DIR = r"E:\_ideaProjects\qylEyelib"

SUPPORTED_VERSIONS = ["1.20.1", "1.21.1", "26.1.2"]

# runtime state (minimal, just process handle for cleanup)
_proc: Optional[asyncio.subprocess.Process] = None
_proc_version: Optional[str] = None


# ── Path helpers ───────────────────────────────────────────────

def _moddev_dir(version: str) -> str:
    return os.path.join(PROJECT_DIR, "versions", version, "build", "moddev")


def _run_artifacts(version: str) -> list[str]:
    d = _moddev_dir(version)
    artifacts = [
        os.path.join(d, "runClient.cmd"),
        os.path.join(d, "clientRunClasspath.txt"),
        os.path.join(d, "clientRunVmArgs.txt"),
        os.path.join(d, "clientRunProgramArgs.txt"),
    ]
    # clientLegacyClasspath.txt only exists for legacy forge (1.20.1)
    if version == "1.20.1":
        artifacts.append(os.path.join(d, "clientLegacyClasspath.txt"))
    return artifacts


def _smoke_artifacts(version: str) -> list[str]:
    d = _moddev_dir(version)
    artifacts = [
        os.path.join(d, "runClientSmoke.cmd"),
        os.path.join(d, "clientSmokeRunClasspath.txt"),
        os.path.join(d, "clientSmokeRunVmArgs.txt"),
        os.path.join(d, "clientSmokeRunProgramArgs.txt"),
    ]
    if version == "1.20.1":
        artifacts.append(os.path.join(d, "clientSmokeLegacyClasspath.txt"))
    return artifacts


def _validate_version(version: str) -> Optional[str]:
    if version not in SUPPORTED_VERSIONS:
        return f"Unsupported version '{version}'. Supported: {SUPPORTED_VERSIONS}"
    return None


def _format_artifact_status(version: str, smoke: bool = False) -> str:
    artifacts = _smoke_artifacts(version) if smoke else _run_artifacts(version)
    lines = []
    for path in artifacts:
        if not os.path.exists(path):
            lines.append(f"missing  {path}")
            continue
        mtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(os.path.getmtime(path)))
        lines.append(f"{mtime}  {os.path.getsize(path):>8}  {path}")
    return "\n".join(lines)


def _validate_run_artifacts(version: str, smoke: bool = False) -> Optional[str]:
    artifacts = _smoke_artifacts(version) if smoke else _run_artifacts(version)
    missing = [path for path in artifacts if not os.path.exists(path)]
    if missing:
        return (
            f"Missing ModDev run artifact(s) for {version}. "
            f"Run eyelib_debug_build or :{version}:createLaunchScripts first.\n"
            + "\n".join(f"  {path}" for path in missing)
        )
    return None


def _run_cmd_path(version: str, smoke: bool = False) -> str:
    d = _moddev_dir(version)
    if smoke:
        return os.path.join(d, "runClientSmoke.cmd")
    return os.path.join(d, "runClient.cmd")


def _gradle_task(version: str, task: str) -> str:
    """Build a version-prefixed Gradle task path."""
    return f":{version}:{task}"


# ── Gradle runner ──────────────────────────────────────────────

def _run_gradle_sync(tasks: list[str], timeout: int = 900) -> subprocess.CompletedProcess:
    task_str = " ".join(tasks)
    stdout_path = os.path.join(PROJECT_DIR, "build", "_mcp_gradle_out.txt")
    stderr_path = os.path.join(PROJECT_DIR, "build", "_mcp_gradle_err.txt")
    log_path = os.path.join(PROJECT_DIR, "build", "_mcp_gradle.log")
    os.makedirs(os.path.dirname(stdout_path), exist_ok=True)

    def _log(msg):
        with open(log_path, "a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%H:%M:%S')} {msg}\n")

    cmd = f'cd /d {PROJECT_DIR} && gradlew.bat {task_str} --stacktrace'

    _log(f"START subprocess.run: {task_str}")
    t0 = time.time()
    result = subprocess.run(
        cmd, shell=True, cwd=PROJECT_DIR, timeout=timeout,
        capture_output=True, text=True,
        stdin=subprocess.DEVNULL,
    )
    elapsed = time.time() - t0
    _log(f"END subprocess.run: {elapsed:.1f}s rc={result.returncode} stdout={len(result.stdout)}B")

    with open(stdout_path, "w", encoding="utf-8") as f:
        f.write(result.stdout)
    with open(stderr_path, "w", encoding="utf-8") as f:
        f.write(result.stderr)

    return result


async def _run_gradle(tasks: list[str], timeout: int = 900) -> subprocess.CompletedProcess:
    return await asyncio.to_thread(_run_gradle_sync, tasks, timeout)


# ── Network helpers ────────────────────────────────────────────

def _port_25999_occupied() -> bool:
    result = subprocess.run(
        ["cmd.exe", "/c", "netstat -ano | findstr 25999"],
        capture_output=True, text=True, timeout=10,
    )
    return any("LISTENING" in line for line in result.stdout.splitlines())


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
                f"{DEBUG_URL}{path}", data=data, method="POST",
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
        tail = stderr[-3000:] if len(stderr) > 3000 else stderr
        return f"{desc} exited early (exit={_proc.returncode}). stderr tail:\n{tail}"
    return f"{desc} exited early (exit={_proc.returncode}). No stderr captured."


async def _poll_ping_until_ready_or_exit(timeout: float, interval: float = 2.0) -> tuple[bool, str]:
    start = time.time()
    early_exit_msg = None
    while time.time() - start < timeout:
        if early_exit_msg is None:
            early_exit_msg = await _process_exit_message("Client process")
        ping = await _http_get("/ping")
        if ping is not None and ping.get("status") == "ok":
            elapsed = time.time() - start
            suffix = f" ({early_exit_msg}; client kept running)" if early_exit_msg else ""
            return True, f"Client startup succeeded after {elapsed:.0f}s{suffix}"
        await asyncio.sleep(interval)
    if early_exit_msg is not None:
        return False, early_exit_msg
    return False, f"Client startup timed out after {timeout}s"


async def _poll_until_loaded(
    timeout: float = 120.0,
    interval: float = 3.0,
) -> tuple[Optional[str], Optional[str]]:
    start = time.time()
    consecutive_ping_fails = 0
    consecutive_loaded_fails = 0
    max_ping_fails = 3
    last_ping = None
    last_loaded = None
    while time.time() - start < timeout:
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
            screen = loaded.get("screen")
            if screen and ("Error" in screen or "Crash" in screen or "Fatal" in screen):
                return "broken", f"Broken mod state: screen={screen}"
            return None, f"Loaded after {elapsed:.0f}s (screen={screen})"
        if loaded is None:
            consecutive_loaded_fails += 1
            if consecutive_loaded_fails >= 2:
                crash = _check_crash_report(_proc_version)
                if crash:
                    return "crash", crash
        else:
            consecutive_loaded_fails = 0
        await asyncio.sleep(interval)
    return "timeout", f"Loading timed out after {timeout}s (last ping={last_ping}, last loaded={last_loaded})"


async def _poll_until_gone(timeout: float = 30.0) -> bool:
    start = time.time()
    while time.time() - start < timeout:
        ping = await _http_get("/ping")
        if ping is None:
            return True
        await asyncio.sleep(0.5)
    return False


# ── State helpers ──────────────────────────────────────────────

async def _proc_alive() -> bool:
    if _proc is None:
        return False
    try:
        await asyncio.wait_for(_proc.wait(), timeout=0.01)
        return False
    except asyncio.TimeoutError:
        return True


def _check_crash_report(version: Optional[str]) -> Optional[str]:
    """Check for recent crash report. Returns crash summary if found within 10 min."""
    versions = [version] if version else list(SUPPORTED_VERSIONS)
    for ver in versions:
        if not ver:
            continue
        crash_dir = os.path.join(PROJECT_DIR, "versions", ver, "run", "crash-reports")
        if not os.path.isdir(crash_dir):
            continue
        try:
            files = sorted(
                [f for f in os.listdir(crash_dir) if f.endswith(".txt")],
                key=lambda f: os.path.getmtime(os.path.join(crash_dir, f)),
                reverse=True,
            )
        except OSError:
            continue
        if not files:
            continue
        latest = os.path.join(crash_dir, files[0])
        if time.time() - os.path.getmtime(latest) > 600:
            continue
        try:
            with open(latest, "r", encoding="utf-8", errors="replace") as f:
                lines = [f.readline() for _ in range(15)]
            return f"[{ver}/{files[0]}]\n" + "".join(lines).strip()
        except OSError:
            continue
    return None


async def _read_game_state() -> dict:
    ping = await _http_get("/ping")
    if ping is None:
        if _proc is not None and await _proc_alive():
            return {"state": "loading", "version": _proc_version}
        return {"state": "idle", "version": _proc_version}

    version_data = await _http_get("/version")
    http_version = version_data.get("version") if version_data else None

    loaded_data = await _http_get("/loaded")
    if loaded_data is None:
        crash = _check_crash_report(http_version or _proc_version)
        if crash:
            return {"state": "crashed", "version": http_version or _proc_version, "crash": crash}
        return {"state": "loading", "version": http_version or _proc_version}

    loaded = loaded_data.get("loaded") is True
    screen = loaded_data.get("screen")

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
    else:
        state = "loading"

    result = {"state": state, "version": _proc_version, "dimension": dimension}
    if screen:
        result["screen"] = screen
    return result


def _state_summary(gs: dict) -> str:
    parts = [f"state={gs['state']}"]
    if gs.get("version"):
        parts.append(f"version={gs['version']}")
    if gs.get("dimension"):
        parts.append(f"dimension={gs['dimension']}")
    if gs.get("screen"):
        parts.append(f"screen={gs['screen']}")
    summary = " | ".join(parts)
    if gs.get("crash"):
        summary += f"\n{gs['crash']}"
    return summary


# ── MCP Server ─────────────────────────────────────────────────

mcp = FastMCP(
    "eyelib-debug",
    instructions="""# Eyelib Debug MCP Server

Controls a Minecraft client for debugging via AIDebugServer HTTP API.
Supports Stonecutter multi-version (1.20.1 / 1.21.1 / 26.1.2).
""",
)


@mcp.tool()
async def eyelib_debug_build(version: str = "1.20.1") -> str:
    """
    Build eyelib and refresh client run artifacts.
    Compiles sources via Gradle then generates launch scripts + classpath files.
    After build, refreshes client run artifacts generated by ModDevGradle.

    Args:
        version: MC version node: "1.20.1", "1.21.1", or "26.1.2".
    """
    err = _validate_version(version)
    if err:
        return f"❌ {err}"

    desc = f"Build ({version})"
    tasks = [
        _gradle_task(version, "compileJava"),
        _gradle_task(version, "prepareClientRun"),
    ]
    if version == "1.20.1":
        tasks.append(_gradle_task(version, "writeClientLegacyClasspath"))
    tasks.append(_gradle_task(version, "createClientLaunchScript"))

    try:
        result = await _run_gradle(tasks, 900)
    except subprocess.TimeoutExpired:
        return f"⏰ {desc} timed out after 900s"

    if result.returncode != 0:
        error_lines = [
            line.strip() for line in (result.stdout + result.stderr).split("\n")
            if line.strip() and ("error:" in line.lower() or "BUILD FAILED" in line or "FAILURE" in line)
        ]
        return f"❌ {desc} — BUILD FAILED\n" + "\n".join(error_lines[:20])

    artifact_error = _validate_run_artifacts(version)
    status = _format_artifact_status(version)
    if artifact_error:
        return f"⚠️ {desc} — BUILD SUCCESSFUL, but artifacts missing:\n{artifact_error}\n\n{status}"
    return f"✅ {desc} — BUILD SUCCESSFUL\n\n{status}"


@mcp.tool()
async def eyelib_debug_test(version: str = "1.20.1", test_filter: str = "") -> str:
    """
    Run eyelib unit tests for a Stonecutter version node.
    Executes Gradle :{version}:test and reports pass/fail summary.
    Full stdout/stderr persisted to build/_mcp_gradle_out.txt and build/_mcp_gradle_err.txt.

    Args:
        version: MC version node: "1.20.1", "1.21.1", or "26.1.2".
        test_filter: Optional Gradle --tests filter (e.g. "io.github.tt432.eyelib.architecture.*").
                     Empty string runs all tests.
    """
    err = _validate_version(version)
    if err:
        return f"❌ {err}"

    desc = f"Test ({version}" + (f", filter={test_filter}" if test_filter else "") + ")"
    tasks = [_gradle_task(version, "test")]
    if test_filter:
        tasks.append(f'--tests "{test_filter}"')

    try:
        result = await _run_gradle(tasks, 900)
    except subprocess.TimeoutExpired:
        return f"⏰ {desc} timed out after 900s"

    # Gradle test 报告目录：versions/{version}/build/reports/tests/test/index.html
    report_dir = os.path.join(
        PROJECT_DIR, "versions", version, "build", "reports", "tests", "test"
    )

    if result.returncode != 0:
        error_lines = [
            line.strip() for line in (result.stdout + result.stderr).split("\n")
            if line.strip() and (
                "error:" in line.lower()
                or "BUILD FAILED" in line
                or "FAILURE:" in line
                or "FAILED" in line
            )
        ]
        return (
            f"❌ {desc} — BUILD/TEST FAILED\n"
            + "\n".join(error_lines[:30])
            + f"\n\nFull output: build/_mcp_gradle_out.txt\nHTML report: {report_dir}/index.html"
        )

    summary_lines = [
        line.strip() for line in result.stdout.split("\n")
        if line.strip() and (
            "tests completed" in line.lower()
            or "tests:" in line.lower()
            or "BUILD SUCCESSFUL" in line
        )
    ]
    return (
        f"✅ {desc} — BUILD/TEST SUCCESSFUL\n"
        + "\n".join(summary_lines[:10])
        + f"\n\nHTML report: {report_dir}/index.html"
    )


@mcp.tool()
async def eyelib_debug_launch(timeout: int = 120, version: str = "1.20.1") -> str:
    """
    Launch MC client.
    Starts MC → polls /ping and /loaded until game is ready.

    Args:
        timeout: Max seconds to wait for client to load (default 120).
        version: MC version node: "1.20.1", "1.21.1", or "26.1.2".
    """
    global _proc, _proc_version

    err = _validate_version(version)
    if err:
        return f"❌ {err}"

    ping = await _http_get("/ping")
    if ping is not None:
        return "❌ Game is already running (port 25999 responds). Use eyelib_debug_close first."

    if _proc is not None and not await _proc_alive():
        _proc = None
        _proc_version = None

    run_cmd = _run_cmd_path(version)
    if not os.path.exists(run_cmd):
        return f"❌ {run_cmd} is missing. Run eyelib_debug_build(version='{version}') first."

    artifact_error = _validate_run_artifacts(version)
    if artifact_error:
        return f"❌ {artifact_error}\n\n{_format_artifact_status(version)}"

    if await asyncio.to_thread(_port_25999_occupied):
        return "❌ Port 25999 is occupied but /ping does not respond."

    try:
        _proc = await asyncio.create_subprocess_exec(
            "cmd.exe", "/c", run_cmd,
            cwd=PROJECT_DIR,
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.PIPE,
        )
        _proc_version = version
    except Exception as e:
        return f"❌ Launch failed: {e}"

    ok, msg = await _poll_ping_until_ready_or_exit(timeout=timeout, interval=2)
    if not ok:
        return f"❌ {msg}"

    err_type, err_msg = await _poll_until_loaded(timeout=max(timeout - 5, 30), interval=3)
    if err_type:
        label = {"crash": "crashed", "timeout": "timed out", "broken": "in broken state"}.get(err_type, err_type)
        return f"❌ Game {label} during loading. {err_msg}"

    ping = await _http_get("/ping")
    if ping is None:
        return "❌ Client became unreachable right after loading (crashed)."

    return f"✅ Client ready at title screen ({version}). Use eyelib_debug_enter_world to start."


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
        return f"❌ Game must be at title screen (loaded) to enter world. Currently: {_state_summary(gs)}"

    result = await _http_post("/enterworld", world_name)
    if result is None or not result.get("success", False):
        return f"❌ /enterworld failed. {_state_summary(gs)}"

    ok, msg = await _poll_until(
        "/enterdworld", lambda d: d.get("inWorld") is True,
        timeout=timeout, interval=2, desc="World enter",
    )
    if not ok:
        return f"❌ {msg}"

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
        return f"❌ Game must be in a world to execute code. Currently: {_state_summary(gs)}"

    result = await _http_post("/eval", code)
    if result is None:
        return "❌ /eval HTTP failed — client may have crashed."

    if result.get("success"):
        return f"✅ {result.get('result', 'ok')}"
    else:
        return f"❌ {result.get('error', 'unknown error')}"


@mcp.tool()
async def eyelib_debug_send_command(side: str, command_text: str) -> str:
    """
    Send a slash command to the running MC client/world.
    side="client" sends it as the local player (goes through the network packet layer);
    side="server" executes it directly on the integrated server (singleplayer only, bypasses network).

    Command feedback (e.g. "Set the time to...") appears in-game chat, not in the return value.
    The command_text may include or omit a leading '/'.

    Args:
        side: "client" or "server".
        command_text: The command text (e.g. "time set day" or "/gamemode creative").
    """
    side_l = side.strip().lower()
    if side_l not in ("client", "server"):
        return f"❌ Invalid side '{side}'. Must be 'client' or 'server'."

    gs = await _read_game_state()
    if gs["state"] != "in_world":
        return f"❌ Game must be in a world to send commands. Currently: {_state_summary(gs)}"

    body = json.dumps({"side": side_l, "command": command_text})
    result = await _http_post("/command", body)
    if result is None:
        return "❌ /command HTTP failed — client may have crashed."

    if result.get("success"):
        return f"✅ [{side_l}] {result.get('result', 'ok')}"
    else:
        return f"❌ [{side_l}] {result.get('error', 'unknown error')}"


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
        parts = [
            f"MCP version: {MCP_VERSION}",
            f"State: {gs['state']}",
            f"Version: {gs.get('version', 'N/A')}",
            f"Screen: {gs.get('screen', 'N/A')}",
            f"Dimension: {gs.get('dimension') or 'N/A'}",
            f"Tracked process alive: {await _proc_alive()}",
        ]
        if gs.get("crash"):
            parts.append(f"\n--- CRASH ---\n{gs['crash']}")
        return "\n".join(parts)
    return f"mcp={MCP_VERSION} | {_state_summary(gs)}"


@mcp.tool()
async def eyelib_debug_close() -> str:
    """
    Close the MC client via /close.
    Waits for port 25999 to become unreachable before returning.
    Falls back to killing the process if /close doesn't work (e.g. MC crashed).
    """
    ping = await _http_get("/ping")
    if ping is None:
        return "Already idle (no game running)."

    await _http_post("/close")

    gone = await _poll_until_gone(timeout=5)
    if not gone:
        subprocess.run(
            'powershell -Command "(Get-NetTCPConnection -LocalPort 25999 -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique | ForEach-Object { taskkill /F /T /PID $_ }"',
            shell=True, capture_output=True, timeout=10, stdin=subprocess.DEVNULL,
        )
        gone = await _poll_until_gone(timeout=5)

    return "✅ Client stopped." if gone else "⚠️ Client force-stopped."


@mcp.tool()
async def eyelib_debug_clientsmoke(timeout: int = 120, version: str = "1.20.1") -> str:
    """
    Run clientsmoke tests in a plain MC client.
    Rebuilds → starts MC with clientsmoke.enabled=true → waits for tests → parses report.
    The client auto-exits after tests finish.

    Args:
        timeout: Max seconds to wait for tests to complete (default 120).
        version: MC version node (default "1.20.1").
    """
    global _proc, _proc_version

    err = _validate_version(version)
    if err:
        return f"❌ {err}"

    ping = await _http_get("/ping")
    if ping is not None:
        return "❌ Game is already running. Use eyelib_debug_close first."

    # Build + generate smoke run artifacts
    build_tasks = [
        _gradle_task(version, "compileJava"),
        _gradle_task(version, "prepareClientSmokeRun"),
    ]
    if version == "1.20.1":
        build_tasks.append(_gradle_task(version, "writeClientSmokeLegacyClasspath"))
    build_tasks.append(_gradle_task(version, "createClientSmokeLaunchScript"))
    try:
        result = await _run_gradle(build_tasks, 900)
    except subprocess.TimeoutExpired:
        return "⏰ Build timed out after 900s"
    if result.returncode != 0:
        error_tail = result.stdout[-2000:] if result.stdout else ""
        return f"❌ Build failed:\n{error_tail}"

    artifact_error = _validate_run_artifacts(version, smoke=True)
    if artifact_error:
        return f"❌ {artifact_error}\n\n{_format_artifact_status(version, smoke=True)}"

    if await asyncio.to_thread(_port_25999_occupied):
        return "❌ Port 25999 is occupied but /ping does not respond."

    # Inject clientsmoke JVM args
    vm_args_path = os.path.join(_moddev_dir(version), "clientSmokeRunVmArgs.txt")
    def _inject():
        with open(vm_args_path, "a", encoding="utf-8") as f:
            f.write("\n-Dclientsmoke.enabled=true\n-Dclientsmoke.autoExit=true\n")
    await asyncio.to_thread(_inject)

    smoke_cmd = _run_cmd_path(version, smoke=True)
    try:
        _proc = await asyncio.create_subprocess_exec(
            "cmd.exe", "/c", smoke_cmd,
            cwd=PROJECT_DIR,
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.PIPE,
        )
        _proc_version = version
    except Exception as e:
        return f"❌ Launch failed: {e}"

    # Wait for startup
    ok, msg = await _poll_until(
        "/ping", lambda d: d.get("status") == "ok",
        timeout=min(timeout, 60), interval=2, desc="Client startup",
    )
    if not ok:
        exit_msg = await _process_exit_message("Client")
        return f"❌ {msg}\n{exit_msg or ''}"

    # Wait for a report newer than this invocation (or process exit).
    # Polling must reject stale reports left over from previous runs — otherwise
    # the loop sees an old mtime and breaks immediately without running tests.
    report_dir = os.path.join(PROJECT_DIR, "run", "clientsmoke-reports")
    invocation_start = time.time()
    while time.time() - invocation_start < timeout:
        exit_msg = await _process_exit_message("Client")
        if exit_msg:
            break
        try:
            json_files = sorted(
                [f for f in os.listdir(report_dir) if f.startswith("report-") and f.endswith(".json")],
                key=lambda f: os.path.getmtime(os.path.join(report_dir, f)),
                reverse=True)
            if json_files:
                rp = os.path.join(report_dir, json_files[0])
                rp_mtime = os.path.getmtime(rp)
                if rp_mtime > invocation_start and time.time() - rp_mtime > 2:
                    await asyncio.sleep(3)
                    break
        except (FileNotFoundError, OSError):
            pass
        await asyncio.sleep(2)

    # Cleanup
    ping = await _http_get("/ping")
    if ping is not None:
        await _http_post("/close")
        await _poll_until_gone(timeout=15)
    _proc = None
    _proc_version = None

    # Parse report
    try:
        json_files = sorted(
            [f for f in os.listdir(report_dir) if f.startswith("report-") and f.endswith(".json")],
            reverse=True)
        if not json_files:
            return "⚠️ No report file found."
        with open(os.path.join(report_dir, json_files[0]), "r", encoding="utf-8") as f:
            report = json.load(f)
        total = report.get("totalTests", 0)
        passed = report.get("passed", 0)
        failed = report.get("failed", 0)
        ts = report.get("timestamp", "?")
        lines = [f"📋 Clientsmoke Report ({ts}) [{version}]",
                 f"   Total: {total}  |  ✅ Passed: {passed}  |  ❌ Failed: {failed}"]
        for e in report.get("entries", []):
            icon = "✅" if e.get("status") == "passed" else "❌"
            name = e.get("className", "?").split(".")[-1]
            lines.append(f"   {icon} {name} ({e.get('durationMs', 0)}ms) — {e.get('description', '')}")
            if e.get("status") == "failed" and e.get("error"):
                lines.append(f"      ↳ {e['error'].get('message', '?')}")
        lines.append(f"\n{'❌' if failed else '✅'} {failed} FAILED" if failed else f"\n✅ All {passed} PASSED")
        return "\n".join(lines)
    except (FileNotFoundError, OSError, json.JSONDecodeError) as e:
        return f"⚠️ Failed to read report: {e}"


@mcp.tool()
async def eyelib_debug_nullaway(module: str = "", version: str = "1.20.1") -> str:
    """
    Run NullAway/Error Prone nullness checks on eyelib sources.

    Args:
        module: Specific module to check (omit for all main sources).
        version: MC version node (default "1.20.1").
    """
    err = _validate_version(version)
    if err:
        return f"❌ {err}"

    target = _gradle_task(version, f"{module}:nullawayMain") if module else _gradle_task(version, "nullawayMain")
    desc = f"NullAway ({target})"

    try:
        result = await _run_gradle([target], 300)
    except subprocess.TimeoutExpired:
        return f"⏰ {desc} timed out after 300s"

    violations = []
    for line in (result.stdout + "\n" + result.stderr).split("\n"):
        stripped = line.strip()
        if "error:" in stripped and ("NullAway" in stripped or "[NullAway]" in stripped):
            violations.append(stripped)

    if result.returncode == 0:
        return f"✅ {desc} — no violations"
    elif violations:
        lines = [f"❌ {desc} — {len(violations)} violation(s):", ""]
        for v in violations[:30]:
            lines.append(f"   {v}")
        if len(violations) > 30:
            lines.append(f"   ... and {len(violations) - 30} more")
        return "\n".join(lines)
    else:
        error_lines = [
            line.strip() for line in (result.stdout + result.stderr).split("\n")
            if "error:" in line.strip() or "BUILD FAILED" in line.strip()
        ]
        return f"❌ {desc} — build failed:\n" + "\n".join(error_lines[:20])


# ── Resource ───────────────────────────────────────────────────

@mcp.resource(uri="debug://session", name="DebugSession")
async def session_resource() -> str:
    gs = await _read_game_state()
    return json.dumps({
        "state": gs["state"],
        "version": gs.get("version"),
        "dimension": gs.get("dimension"),
    }, indent=2)


if __name__ == "__main__":
    mcp.run()
