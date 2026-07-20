/**
 * mcmcp — Minecraft client debug extension for omp.
 *
 * Generic MC debug driver: launches a client via the project-local `.mcmcp`
 * file and talks to the AIDebugServer HTTP endpoints hosted by the
 * clientsmoke mod. Stateless — all game state is queried live from the
 * in-game HTTP server.
 *
 * Project contract:
 *   - `.mcmcp` at project root: JSON `{ "<name>": ["cmd", "arg", ...] }`.
 *     Example: `{ "1.20.1": ["cmd.exe", "/c", "versions\\1.20.1\\build\\moddev\\runClient.cmd"] }`
 *   - The launched client must contain the clientsmoke mod with AIDebugServer.
 *   - Port selection: this extension passes `AI_DEBUG_PORT` to the child
 *     process environment; `{port}` placeholders inside launch_cmd args are
 *     substituted as well. AIDebugServer stays off without a configured port.
 *
 * Gradle helper tools (build/test/nullaway/clientsmoke) additionally assume a
 * Stonecutter + ModDevGradle layout (gradlew at root, versions/<name>/ nodes)
 * and fail with a clear error elsewhere.
 */

import { spawn, type ChildProcess } from "node:child_process";
import * as fs from "node:fs";
import * as path from "node:path";
import * as net from "node:net";

import type {
  AgentToolResult,
  ExtensionAPI,
  ExtensionContext,
  ToolDefinition,
} from "./src/omp";

const EXT_VERSION = "1.0.0";
const DEFAULT_PORT = 25999;

// ── Runtime state (process handle only; game state is queried live) ──

let proc: ChildProcess | null = null;
let procExited: { code: number | null; stderrTail: string } | null = null;
let procVersion: string | null = null;
let activePort = DEFAULT_PORT;

// ── .mcmcp ──

type McmcpConfig = Record<string, string[]>;

function loadMcmcp(cwd: string): McmcpConfig | string {
  const file = path.join(cwd, ".mcmcp");
  if (!fs.existsSync(file)) {
    return `❌ No .mcmcp file in ${cwd}. Create one: {"<name>": ["launch", "cmd"]}`;
  }
  try {
    const parsed: unknown = JSON.parse(fs.readFileSync(file, "utf-8"));
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      return `❌ .mcmcp must be a JSON object {"<name>": ["cmd", ...]}`;
    }
    const out: McmcpConfig = {};
    for (const [k, v] of Object.entries(parsed)) {
      if (!Array.isArray(v) || v.length === 0 || !v.every((x) => typeof x === "string")) {
        return `❌ .mcmcp entry "${k}" must be a non-empty string array`;
      }
      out[k] = v as string[];
    }
    return out;
  } catch (e) {
    return `❌ Failed to parse .mcmcp: ${e instanceof Error ? e.message : String(e)}`;
  }
}

function resolveVersion(cfg: McmcpConfig, version?: string): string | null {
  if (version) return version in cfg ? version : null;
  const keys = Object.keys(cfg);
  return keys.length > 0 ? keys[0] : null;
}

// ── HTTP helpers (AIDebugServer endpoints) ──

type Json = Record<string, unknown>;

async function httpGet(port: number, p: string, timeoutMs = 5000): Promise<Json | null> {
  try {
    const resp = await fetch(`http://localhost:${port}${p}`, {
      signal: AbortSignal.timeout(timeoutMs),
    });
    return (await resp.json()) as Json;
  } catch {
    return null;
  }
}

async function httpPost(port: number, p: string, body = "", timeoutMs = 10000): Promise<Json | null> {
  try {
    const resp = await fetch(`http://localhost:${port}${p}`, {
      method: "POST",
      headers: { "Content-Type": "text/plain; charset=utf-8" },
      body,
      signal: AbortSignal.timeout(timeoutMs),
    });
    return (await resp.json()) as Json;
  } catch {
    return null;
  }
}

function sleep(ms: number): Promise<void> {
  const { promise, resolve } = Promise.withResolvers<void>();
  setTimeout(resolve, ms);
  return promise;
}

async function pollUntil(
  fn: () => Promise<Json | null>,
  check: (d: Json) => boolean,
  timeoutSec: number,
  intervalSec: number,
  desc: string,
): Promise<[boolean, string]> {
  const start = Date.now();
  while (Date.now() - start < timeoutSec * 1000) {
    const data = await fn();
    if (data !== null && check(data)) {
      return [true, `${desc} succeeded after ${Math.round((Date.now() - start) / 1000)}s`];
    }
    await sleep(intervalSec * 1000);
  }
  return [false, `${desc} timed out after ${timeoutSec}s`];
}

function tcpListens(port: number): Promise<boolean> {
  const { promise, resolve } = Promise.withResolvers<boolean>();
  const sock = net.connect({ port, host: "127.0.0.1" });
  sock.once("connect", () => {
    sock.destroy();
    resolve(true);
  });
  sock.once("error", () => resolve(false));
  sock.setTimeout(3000, () => {
    sock.destroy();
    resolve(false);
  });
  return promise;
}

// ── Process helpers ──

function trackProc(child: ChildProcess, version: string): void {
  proc = child;
  procVersion = version;
  procExited = null;
  let stderrTail = "";
  child.stderr?.on("data", (chunk: Buffer) => {
    stderrTail = (stderrTail + chunk.toString("utf-8")).slice(-3000);
  });
  child.on("exit", (code) => {
    procExited = { code, stderrTail: stderrTail.trim() };
  });
}

function processExitMessage(desc: string): string | null {
  if (procExited === null) return null;
  const { code, stderrTail } = procExited;
  if (stderrTail) {
    return `${desc} exited early (exit=${code}). stderr tail:\n${stderrTail}`;
  }
  return `${desc} exited early (exit=${code}). No stderr captured.`;
}

function procAlive(): boolean {
  return proc !== null && procExited === null;
}

async function pollPingUntilReadyOrExit(port: number, timeoutSec: number): Promise<[boolean, string]> {
  const start = Date.now();
  let earlyExit: string | null = null;
  while (Date.now() - start < timeoutSec * 1000) {
    if (earlyExit === null) earlyExit = processExitMessage("Client process");
    const ping = await httpGet(port, "/ping");
    if (ping !== null && ping["status"] === "ok") {
      const suffix = earlyExit ? ` (${earlyExit}; client kept running)` : "";
      return [true, `Client startup succeeded after ${Math.round((Date.now() - start) / 1000)}s${suffix}`];
    }
    await sleep(2000);
  }
  if (earlyExit !== null) return [false, earlyExit];
  return [false, `Client startup timed out after ${timeoutSec}s`];
}

async function pollUntilLoaded(
  cwd: string,
  port: number,
  timeoutSec: number,
): Promise<[string | null, string]> {
  const start = Date.now();
  let pingFails = 0;
  let loadedFails = 0;
  while (Date.now() - start < timeoutSec * 1000) {
    const exitMsg = processExitMessage("Client process");
    if (exitMsg !== null) return ["crash", exitMsg];
    const ping = await httpGet(port, "/ping");
    if (ping === null) {
      pingFails++;
      if (pingFails >= 3) return ["crash", `Client unreachable for ${pingFails * 3}s`];
      await sleep(3000);
      continue;
    }
    pingFails = 0;
    const loaded = await httpGet(port, "/loaded");
    if (loaded !== null && loaded["loaded"] === true) {
      const screen = typeof loaded["screen"] === "string" ? loaded["screen"] : null;
      if (screen && (screen.includes("Error") || screen.includes("Crash") || screen.includes("Fatal"))) {
        return ["broken", `Broken mod state: screen=${screen}`];
      }
      return [null, `Loaded after ${Math.round((Date.now() - start) / 1000)}s (screen=${screen})`];
    }
    if (loaded === null) {
      loadedFails++;
      if (loadedFails >= 2) {
        const crash = checkCrashReport(cwd, procVersion);
        if (crash) return ["crash", crash];
      }
    } else {
      loadedFails = 0;
    }
    await sleep(3000);
  }
  return ["timeout", `Loading timed out after ${timeoutSec}s`];
}

async function pollUntilGone(port: number, timeoutSec: number): Promise<boolean> {
  const start = Date.now();
  while (Date.now() - start < timeoutSec * 1000) {
    const ping = await httpGet(port, "/ping", 2000);
    if (ping === null) return true;
    await sleep(500);
  }
  return false;
}

function checkCrashReport(cwd: string, version: string | null): string | null {
  const versions = version ? [version] : [];
  for (const ver of versions) {
    const crashDir = path.join(cwd, "versions", ver, "run", "crash-reports");
    if (!fs.existsSync(crashDir)) continue;
    let files: string[];
    try {
      files = fs
        .readdirSync(crashDir)
        .filter((f) => f.endsWith(".txt"))
        .sort(
          (a, b) =>
            fs.statSync(path.join(crashDir, b)).mtimeMs - fs.statSync(path.join(crashDir, a)).mtimeMs,
        );
    } catch {
      continue;
    }
    if (files.length === 0) continue;
    const latest = path.join(crashDir, files[0]);
    if (Date.now() - fs.statSync(latest).mtimeMs > 600_000) continue;
    try {
      const head = fs.readFileSync(latest, "utf-8").split("\n").slice(0, 15).join("\n");
      return `[${ver}/${files[0]}]\n${head.trim()}`;
    } catch {
      continue;
    }
  }
  return null;
}

// ── Game state ──

interface GameState {
  state: "idle" | "loading" | "loaded" | "in_world" | "crashed";
  version?: string | null;
  dimension?: string | null;
  screen?: string | null;
  crash?: string;
}

async function readGameState(cwd: string, port: number): Promise<GameState> {
  const ping = await httpGet(port, "/ping");
  if (ping === null) {
    if (procAlive()) return { state: "loading", version: procVersion };
    return { state: "idle", version: procVersion };
  }
  const versionData = await httpGet(port, "/version");
  const httpVersion =
    versionData && typeof versionData["version"] === "string" ? versionData["version"] : null;
  const loadedData = await httpGet(port, "/loaded");
  if (loadedData === null) {
    const crash = checkCrashReport(cwd, httpVersion ?? procVersion);
    if (crash) return { state: "crashed", version: httpVersion ?? procVersion, crash };
    return { state: "loading", version: httpVersion ?? procVersion };
  }
  const loaded = loadedData["loaded"] === true;
  const screen = typeof loadedData["screen"] === "string" ? loadedData["screen"] : null;
  let inWorld = false;
  let dimension: string | null = null;
  if (loaded) {
    const enterData = await httpGet(port, "/enterdworld");
    if (enterData) {
      inWorld = enterData["inWorld"] === true;
      dimension = typeof enterData["dimension"] === "string" ? enterData["dimension"] : null;
    }
  }
  return {
    state: inWorld ? "in_world" : loaded ? "loaded" : "loading",
    version: procVersion,
    dimension,
    screen,
  };
}

function stateSummary(gs: GameState): string {
  const parts = [`state=${gs.state}`];
  if (gs.version) parts.push(`version=${gs.version}`);
  if (gs.dimension) parts.push(`dimension=${gs.dimension}`);
  if (gs.screen) parts.push(`screen=${gs.screen}`);
  let summary = parts.join(" | ");
  if (gs.crash) summary += `\n${gs.crash}`;
  return summary;
}

// ── Gradle helpers (Stonecutter + ModDevGradle layout only) ──

interface GradleResult {
  code: number;
  stdout: string;
  stderr: string;
  timedOut: boolean;
}

function runGradle(cwd: string, tasks: string[], timeoutSec: number): Promise<GradleResult> {
  const buildDir = path.join(cwd, "build");
  fs.mkdirSync(buildDir, { recursive: true });
  const logPath = path.join(buildDir, "_mcp_gradle.log");
  const cmdline = `gradlew.bat ${tasks.join(" ")} --console=plain --stacktrace`;
  fs.appendFileSync(logPath, `${new Date().toTimeString().slice(0, 8)} START ${cmdline}\n`);
  const { promise, resolve } = Promise.withResolvers<GradleResult>();
  const child = spawn("cmd.exe", ["/c", cmdline], { cwd, stdio: ["ignore", "pipe", "pipe"] });
  let stdout = "";
  let stderr = "";
  child.stdout.on("data", (c: Buffer) => (stdout += c.toString("utf-8")));
  child.stderr.on("data", (c: Buffer) => (stderr += c.toString("utf-8")));
  const timer = setTimeout(() => {
    child.kill();
    resolve({ code: -1, stdout, stderr, timedOut: true });
  }, timeoutSec * 1000);
  child.on("close", (code) => {
    clearTimeout(timer);
    fs.writeFileSync(path.join(buildDir, "_mcp_gradle_out.txt"), stdout);
    fs.writeFileSync(path.join(buildDir, "_mcp_gradle_err.txt"), stderr);
    fs.appendFileSync(logPath, `END rc=${code} stdout=${stdout.length}B\n`);
    resolve({ code: code ?? -1, stdout, stderr, timedOut: false });
  });
  child.on("error", (err) => {
    clearTimeout(timer);
    resolve({ code: -1, stdout, stderr: stderr + String(err), timedOut: false });
  });
  return promise;
}

function gradleLayoutError(cwd: string): string | null {
  if (!fs.existsSync(path.join(cwd, "gradlew.bat"))) {
    return "❌ This tool requires a Stonecutter + ModDevGradle project (gradlew.bat + versions/<name>/ layout).";
  }
  return null;
}

function moddevDir(cwd: string, version: string): string {
  return path.join(cwd, "versions", version, "build", "moddev");
}

function runArtifacts(cwd: string, version: string, smoke: boolean): string[] {
  const d = moddevDir(cwd, version);
  const prefix = smoke ? "clientSmoke" : "client";
  const script = smoke ? "runClientSmoke.cmd" : "runClient.cmd";
  const artifacts = [
    path.join(d, script),
    path.join(d, `${prefix}RunClasspath.txt`),
    path.join(d, `${prefix}RunVmArgs.txt`),
    path.join(d, `${prefix}RunProgramArgs.txt`),
  ];
  if (version === "1.20.1") artifacts.push(path.join(d, `${prefix}LegacyClasspath.txt`));
  return artifacts;
}

function validateRunArtifacts(cwd: string, version: string, smoke: boolean): string | null {
  const missing = runArtifacts(cwd, version, smoke).filter((p) => !fs.existsSync(p));
  if (missing.length === 0) return null;
  return (
    `Missing ModDev run artifact(s) for ${version}. Run mcmcp_build or :${version}:createLaunchScripts first.\n` +
    missing.map((p) => `  ${p}`).join("\n")
  );
}

function formatArtifactStatus(cwd: string, version: string, smoke: boolean): string {
  return runArtifacts(cwd, version, smoke)
    .map((p) => {
      if (!fs.existsSync(p)) return `missing  ${p}`;
      const st = fs.statSync(p);
      return `${st.mtime.toISOString().slice(0, 19).replace("T", " ")}  ${String(st.size).padStart(8)}  ${p}`;
    })
    .join("\n");
}

// ── Tool plumbing ──

function textResult(text: string): AgentToolResult {
  return { content: [{ type: "text", text }] };
}

function cwdOf(ctx: ExtensionContext): string {
  return ctx.cwd ?? process.cwd();
}

function portOf(params: { port?: number }): number {
  return params.port ?? activePort;
}

// ── Extension factory ──

export default function mcmcpExtension(pi: ExtensionAPI): void {
  const z = pi.zod;
  pi.setLabel("mcmcp (Minecraft client debug)");

  const register = <T>(def: ToolDefinition<T>): void => {
    // ToolDefinition<T> → ToolDefinition<never>: method-style execute is bivariant.
    pi.registerTool(def as ToolDefinition<never>);
  };

  // ── mcmcp_launch ──
  interface LaunchParams {
    version?: string;
    timeout?: number;
    port?: number;
  }
  register<LaunchParams>({
    name: "mcmcp_launch",
    label: "MC Launch",
    description:
      "Launch an MC client via the project .mcmcp launch command. Injects AI_DEBUG_PORT into the child environment and substitutes {port} placeholders in the command. Polls /ping and /loaded until ready.",
    parameters: z.object({
      version: z.string().optional().describe("Key in .mcmcp (default: first entry)"),
      timeout: z.number().int().optional().describe("Max seconds to wait for load (default 120)"),
      port: z.number().int().optional().describe(`Debug HTTP port (default ${DEFAULT_PORT})`),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const cwd = cwdOf(ctx);
      const cfg = loadMcmcp(cwd);
      if (typeof cfg === "string") return textResult(cfg);
      const version = resolveVersion(cfg, params.version);
      if (version === null) {
        return textResult(
          `❌ Unknown version '${params.version}'. Available: ${Object.keys(cfg).join(", ")}`,
        );
      }
      const port = params.port ?? DEFAULT_PORT;
      const timeout = params.timeout ?? 120;

      const ping = await httpGet(port, "/ping");
      if (ping !== null) {
        return textResult(`❌ Game is already running (port ${port} responds). Use mcmcp_close first.`);
      }
      if (proc !== null && !(procAlive())) {
        proc = null;
        procVersion = null;
        procExited = null;
      }
      if (await tcpListens(port)) {
        return textResult(`❌ Port ${port} is occupied but /ping does not respond.`);
      }

      const launchCmd = cfg[version].map((a) => a.replaceAll("{port}", String(port)));
      try {
        const child = spawn(launchCmd[0], launchCmd.slice(1), {
          cwd,
          env: { ...process.env, AI_DEBUG_PORT: String(port) },
          stdio: ["ignore", "ignore", "pipe"],
        });
        trackProc(child, version);
        activePort = port;
      } catch (e) {
        return textResult(`❌ Launch failed: ${e instanceof Error ? e.message : String(e)}`);
      }

      const [ok, msg] = await pollPingUntilReadyOrExit(port, timeout);
      if (!ok) return textResult(`❌ ${msg}`);

      const [errType, errMsg] = await pollUntilLoaded(cwd, port, Math.max(timeout - 5, 30));
      if (errType !== null) {
        const label = { crash: "crashed", timeout: "timed out", broken: "in broken state" }[errType] ?? errType;
        return textResult(`❌ Game ${label} during loading. ${errMsg}`);
      }
      const ping2 = await httpGet(port, "/ping");
      if (ping2 === null) return textResult("❌ Client became unreachable right after loading (crashed).");
      return textResult(`✅ Client ready at title screen (${version}, port ${port}). Use mcmcp_enter_world to start.`);
    },
  });

  // ── mcmcp_close ──
  interface PortParams {
    port?: number;
  }
  register<PortParams>({
    name: "mcmcp_close",
    label: "MC Close",
    description:
      "Close the MC client via /close. Waits for the debug port to become unreachable; falls back to killing the owning process.",
    parameters: z.object({
      port: z.number().int().optional().describe("Debug HTTP port (default: last launched port)"),
    }),
    async execute(_id, params, _signal, _onUpdate, _ctx) {
      const port = portOf(params);
      const ping = await httpGet(port, "/ping");
      if (ping === null) return textResult("Already idle (no game running).");
      await httpPost(port, "/close");
      let gone = await pollUntilGone(port, 5);
      if (!gone) {
        const { promise: killed, resolve: resolveKilled } = Promise.withResolvers<void>();
        const killer = spawn(
          "powershell",
          [
            "-Command",
            `(Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique | ForEach-Object { taskkill /F /T /PID $_ }`,
          ],
          { stdio: "ignore" },
        );
        killer.on("close", () => resolveKilled());
        killer.on("error", () => resolveKilled());
        await killed;
        gone = await pollUntilGone(port, 5);
      }
      proc = null;
      procVersion = null;
      procExited = null;
      return textResult(gone ? "✅ Client stopped." : "⚠️ Client force-stopped.");
    },
  });

  // ── mcmcp_status ──
  interface StatusParams {
    info?: string;
    port?: number;
  }
  register<StatusParams>({
    name: "mcmcp_status",
    label: "MC Status",
    description: "Get current debug session state from AIDebugServer. All state is read fresh.",
    parameters: z.object({
      info: z.enum(["summary", "all"]).optional().describe('"summary" (default) or "all"'),
      port: z.number().int().optional().describe("Debug HTTP port (default: last launched port)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const port = portOf(params);
      const gs = await readGameState(cwdOf(ctx), port);
      if (params.info === "all") {
        const parts = [
          `Extension version: ${EXT_VERSION}`,
          `Port: ${port}`,
          `State: ${gs.state}`,
          `Version: ${gs.version ?? "N/A"}`,
          `Screen: ${gs.screen ?? "N/A"}`,
          `Dimension: ${gs.dimension ?? "N/A"}`,
          `Tracked process alive: ${procAlive()}`,
        ];
        if (gs.crash) parts.push(`\n--- CRASH ---\n${gs.crash}`);
        return textResult(parts.join("\n"));
      }
      return textResult(`mcmcp=${EXT_VERSION} port=${port} | ${stateSummary(gs)}`);
    },
  });

  // ── mcmcp_enter_world ──
  interface EnterWorldParams {
    world_name?: string;
    timeout?: number;
    port?: number;
  }
  register<EnterWorldParams>({
    name: "mcmcp_enter_world",
    label: "MC Enter World",
    description:
      "Enter (or create) a singleplayer flat world. Calls /enterworld → polls /enterdworld → auto-unpauses.",
    parameters: z.object({
      world_name: z.string().optional().describe('World name (default "Debug World")'),
      timeout: z.number().int().optional().describe("Max seconds to wait (default 60)"),
      port: z.number().int().optional().describe("Debug HTTP port (default: last launched port)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const port = portOf(params);
      const cwd = cwdOf(ctx);
      const gs = await readGameState(cwd, port);
      if (gs.state !== "loaded") {
        return textResult(`❌ Game must be at title screen (loaded) to enter world. Currently: ${stateSummary(gs)}`);
      }
      const result = await httpPost(port, "/enterworld", params.world_name ?? "Debug World");
      if (result === null || result["success"] !== true) {
        return textResult(`❌ /enterworld failed. ${stateSummary(gs)}`);
      }
      const [ok, msg] = await pollUntil(
        () => httpGet(port, "/enterdworld"),
        (d) => d["inWorld"] === true,
        params.timeout ?? 60,
        2,
        "World enter",
      );
      if (!ok) return textResult(`❌ ${msg}`);
      const gs2 = await readGameState(cwd, port);
      await httpPost(
        port,
        "/eval",
        'net.minecraft.client.Minecraft.getInstance().setScreen(null); return "unpaused";',
      );
      return textResult(`✅ In world (${gs2.dimension ?? "N/A"}). Ready for code execution.`);
    },
  });

  // ── mcmcp_execute ──
  interface ExecuteParams {
    code: string;
    port?: number;
  }
  register<ExecuteParams>({
    name: "mcmcp_execute",
    label: "MC Execute",
    description:
      "Execute Java code in the running MC client via /eval. Code is a Java method body (no class wrapper); minecraft/player/level are injected.",
    parameters: z.object({
      code: z.string().describe("Java method body to execute"),
      port: z.number().int().optional().describe("Debug HTTP port (default: last launched port)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const port = portOf(params);
      const gs = await readGameState(cwdOf(ctx), port);
      if (gs.state !== "in_world") {
        return textResult(`❌ Game must be in a world to execute code. Currently: ${stateSummary(gs)}`);
      }
      const result = await httpPost(port, "/eval", params.code, 30000);
      if (result === null) return textResult("❌ /eval HTTP failed — client may have crashed.");
      if (result["success"] === true) return textResult(`✅ ${result["result"] ?? "ok"}`);
      return textResult(`❌ ${result["error"] ?? "unknown error"}`);
    },
  });

  // ── mcmcp_send_command ──
  interface SendCommandParams {
    side: string;
    command_text: string;
    port?: number;
  }
  register<SendCommandParams>({
    name: "mcmcp_send_command",
    label: "MC Send Command",
    description:
      'Send a slash command. side="client" sends as the local player (network packet layer); side="server" executes directly on the integrated server (singleplayer only). Command feedback appears in-game chat.',
    parameters: z.object({
      side: z.enum(["client", "server"]).describe('"client" or "server"'),
      command_text: z.string().describe('Command text (e.g. "time set day" or "/gamemode creative")'),
      port: z.number().int().optional().describe("Debug HTTP port (default: last launched port)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const port = portOf(params);
      const gs = await readGameState(cwdOf(ctx), port);
      if (gs.state !== "in_world") {
        return textResult(`❌ Game must be in a world to send commands. Currently: ${stateSummary(gs)}`);
      }
      const body = JSON.stringify({ side: params.side, command: params.command_text });
      const result = await httpPost(port, "/command", body);
      if (result === null) return textResult("❌ /command HTTP failed — client may have crashed.");
      if (result["success"] === true) return textResult(`✅ [${params.side}] ${result["result"] ?? "ok"}`);
      return textResult(`❌ [${params.side}] ${result["error"] ?? "unknown error"}`);
    },
  });

  // ── mcmcp_build ──
  interface BuildParams {
    version?: string;
  }
  register<BuildParams>({
    name: "mcmcp_build",
    label: "MC Build",
    description:
      "Build sources and refresh ModDevGradle client run artifacts (compileJava + prepareClientRun + createClientLaunchScript). Requires Stonecutter layout.",
    parameters: z.object({
      version: z.string().optional().describe("Stonecutter version node (default: first .mcmcp entry)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const cwd = cwdOf(ctx);
      const layoutErr = gradleLayoutError(cwd);
      if (layoutErr) return textResult(layoutErr);
      const cfg = loadMcmcp(cwd);
      if (typeof cfg === "string") return textResult(cfg);
      const version = resolveVersion(cfg, params.version);
      if (version === null) return textResult(`❌ Unknown version '${params.version}'. Available: ${Object.keys(cfg).join(", ")}`);

      const tasks = [`:${version}:compileJava`, `:${version}:prepareClientRun`];
      if (version === "1.20.1") tasks.push(`:${version}:writeClientLegacyClasspath`);
      tasks.push(`:${version}:createClientLaunchScript`);

      const result = await runGradle(cwd, tasks, 900);
      if (result.timedOut) return textResult(`⏰ Build (${version}) timed out after 900s`);
      if (result.code !== 0) {
        const errorLines = (result.stdout + "\n" + result.stderr)
          .split("\n")
          .map((l) => l.trim())
          .filter((l) => l && (l.toLowerCase().includes("error:") || l.includes("BUILD FAILED") || l.includes("FAILURE")));
        return textResult(`❌ Build (${version}) — BUILD FAILED\n${errorLines.slice(0, 20).join("\n")}`);
      }
      const artifactError = validateRunArtifacts(cwd, version, false);
      const status = formatArtifactStatus(cwd, version, false);
      if (artifactError) {
        return textResult(`⚠️ Build (${version}) — BUILD SUCCESSFUL, but artifacts missing:\n${artifactError}\n\n${status}`);
      }
      return textResult(`✅ Build (${version}) — BUILD SUCCESSFUL\n\n${status}`);
    },
  });

  // ── mcmcp_test ──
  interface TestParams {
    version?: string;
    test_filter?: string;
  }
  register<TestParams>({
    name: "mcmcp_test",
    label: "MC Test",
    description:
      "Run unit tests for a Stonecutter version node (:{version}:test). Full output persisted to build/_mcp_gradle_out.txt.",
    parameters: z.object({
      version: z.string().optional().describe("Stonecutter version node (default: first .mcmcp entry)"),
      test_filter: z.string().optional().describe('Gradle --tests filter (e.g. "io.github.*"). Empty = all.'),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const cwd = cwdOf(ctx);
      const layoutErr = gradleLayoutError(cwd);
      if (layoutErr) return textResult(layoutErr);
      const cfg = loadMcmcp(cwd);
      if (typeof cfg === "string") return textResult(cfg);
      const version = resolveVersion(cfg, params.version);
      if (version === null) return textResult(`❌ Unknown version '${params.version}'. Available: ${Object.keys(cfg).join(", ")}`);

      const filter = params.test_filter ?? "";
      const tasks = [`:${version}:test`];
      if (filter) tasks.push(`--tests "${filter}"`);
      const result = await runGradle(cwd, tasks, 900);
      const reportDir = path.join(cwd, "versions", version, "build", "reports", "tests", "test");
      if (result.timedOut) return textResult(`⏰ Test (${version}) timed out after 900s`);
      if (result.code !== 0) {
        const errorLines = (result.stdout + "\n" + result.stderr)
          .split("\n")
          .map((l) => l.trim())
          .filter(
            (l) =>
              l &&
              (l.toLowerCase().includes("error:") ||
                l.includes("BUILD FAILED") ||
                l.includes("FAILURE:") ||
                l.includes("FAILED")),
          );
        return textResult(
          `❌ Test (${version}${filter ? `, filter=${filter}` : ""}) — BUILD/TEST FAILED\n` +
            errorLines.slice(0, 30).join("\n") +
            `\n\nFull output: build/_mcp_gradle_out.txt\nHTML report: ${reportDir}/index.html`,
        );
      }
      const summaryLines = result.stdout
        .split("\n")
        .map((l) => l.trim())
        .filter((l) => l && (l.toLowerCase().includes("tests completed") || l.toLowerCase().includes("tests:") || l.includes("BUILD SUCCESSFUL")));
      return textResult(
        `✅ Test (${version}) — BUILD/TEST SUCCESSFUL\n${summaryLines.slice(0, 10).join("\n")}\n\nHTML report: ${reportDir}/index.html`,
      );
    },
  });

  // ── mcmcp_nullaway ──
  interface NullawayParams {
    module?: string;
    version?: string;
  }
  register<NullawayParams>({
    name: "mcmcp_nullaway",
    label: "MC NullAway",
    description: "Run NullAway/Error Prone nullness checks on sources. Requires Stonecutter layout.",
    parameters: z.object({
      module: z.string().optional().describe("Specific module to check (omit for all main sources)"),
      version: z.string().optional().describe("Stonecutter version node (default: first .mcmcp entry)"),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const cwd = cwdOf(ctx);
      const layoutErr = gradleLayoutError(cwd);
      if (layoutErr) return textResult(layoutErr);
      const cfg = loadMcmcp(cwd);
      if (typeof cfg === "string") return textResult(cfg);
      const version = resolveVersion(cfg, params.version);
      if (version === null) return textResult(`❌ Unknown version '${params.version}'. Available: ${Object.keys(cfg).join(", ")}`);

      const module = params.module ?? "";
      const target = module ? `:${version}:${module}:nullawayMain` : `:${version}:nullawayMain`;
      const result = await runGradle(cwd, [target], 300);
      if (result.timedOut) return textResult(`⏰ NullAway (${target}) timed out after 300s`);
      const violations = (result.stdout + "\n" + result.stderr)
        .split("\n")
        .map((l) => l.trim())
        .filter((l) => l.includes("error:") && l.includes("NullAway"));
      if (result.code === 0) return textResult(`✅ NullAway (${target}) — no violations`);
      if (violations.length > 0) {
        const lines = [`❌ NullAway (${target}) — ${violations.length} violation(s):`, ""];
        for (const v of violations.slice(0, 30)) lines.push(`   ${v}`);
        if (violations.length > 30) lines.push(`   ... and ${violations.length - 30} more`);
        return textResult(lines.join("\n"));
      }
      const errorLines = (result.stdout + "\n" + result.stderr)
        .split("\n")
        .map((l) => l.trim())
        .filter((l) => l.includes("error:") || l.includes("BUILD FAILED"));
      return textResult(`❌ NullAway (${target}) — build failed:\n${errorLines.slice(0, 20).join("\n")}`);
    },
  });

  // ── mcmcp_clientsmoke ──
  interface ClientSmokeParams {
    timeout?: number;
    version?: string;
    port?: number;
  }
  register<ClientSmokeParams>({
    name: "mcmcp_clientsmoke",
    label: "MC ClientSmoke",
    description:
      "Run clientsmoke tests in a plain MC client: rebuild → inject clientsmoke + debug-port JVM args → launch smoke run → wait for report → parse. The client auto-exits after tests finish.",
    parameters: z.object({
      timeout: z.number().int().optional().describe("Max seconds to wait for tests (default 120)"),
      version: z.string().optional().describe("Stonecutter version node (default: first .mcmcp entry)"),
      port: z.number().int().optional().describe(`Debug HTTP port (default ${DEFAULT_PORT})`),
    }),
    async execute(_id, params, _signal, _onUpdate, ctx) {
      const cwd = cwdOf(ctx);
      const layoutErr = gradleLayoutError(cwd);
      if (layoutErr) return textResult(layoutErr);
      const cfg = loadMcmcp(cwd);
      if (typeof cfg === "string") return textResult(cfg);
      const version = resolveVersion(cfg, params.version);
      if (version === null) return textResult(`❌ Unknown version '${params.version}'. Available: ${Object.keys(cfg).join(", ")}`);
      const port = params.port ?? DEFAULT_PORT;
      const timeout = params.timeout ?? 120;

      const ping = await httpGet(port, "/ping");
      if (ping !== null) return textResult("❌ Game is already running. Use mcmcp_close first.");

      const buildTasks = [`:${version}:compileJava`, `:${version}:prepareClientSmokeRun`];
      if (version === "1.20.1") buildTasks.push(`:${version}:writeClientSmokeLegacyClasspath`);
      buildTasks.push(`:${version}:createClientSmokeLaunchScript`);
      const buildResult = await runGradle(cwd, buildTasks, 900);
      if (buildResult.timedOut) return textResult("⏰ Build timed out after 900s");
      if (buildResult.code !== 0) {
        return textResult(`❌ Build failed:\n${buildResult.stdout.slice(-2000)}`);
      }
      const artifactError = validateRunArtifacts(cwd, version, true);
      if (artifactError) {
        return textResult(`❌ ${artifactError}\n\n${formatArtifactStatus(cwd, version, true)}`);
      }
      if (await tcpListens(port)) {
        return textResult(`❌ Port ${port} is occupied but /ping does not respond.`);
      }

      // Inject clientsmoke + debug-port JVM args into the smoke run's arg file
      const vmArgsPath = path.join(moddevDir(cwd, version), "clientSmokeRunVmArgs.txt");
      fs.appendFileSync(
        vmArgsPath,
        `\n-Dclientsmoke.enabled=true\n-Dclientsmoke.autoExit=true\n-Dai_debug_port=${port}\n`,
        "utf-8",
      );

      const smokeCmd = path.join(moddevDir(cwd, version), "runClientSmoke.cmd");
      try {
        const child = spawn("cmd.exe", ["/c", smokeCmd], {
          cwd,
          env: { ...process.env, AI_DEBUG_PORT: String(port) },
          stdio: ["ignore", "ignore", "pipe"],
        });
        trackProc(child, version);
        activePort = port;
      } catch (e) {
        return textResult(`❌ Launch failed: ${e instanceof Error ? e.message : String(e)}`);
      }

      const [started, startMsg] = await pollUntil(
        () => httpGet(port, "/ping"),
        (d) => d["status"] === "ok",
        Math.min(timeout, 60),
        2,
        "Client startup",
      );
      if (!started) {
        return textResult(`❌ ${startMsg}\n${processExitMessage("Client") ?? ""}`);
      }

      // Wait for a report newer than this invocation (or process exit).
      const reportDir = path.join(cwd, "versions", version, "run", "clientsmoke", "clientsmoke-reports");
      const invocationStart = Date.now();
      const listReports = (): string[] => {
        try {
          return fs
            .readdirSync(reportDir)
            .filter((f) => f.startsWith("report-") && f.endsWith(".json"))
            .sort(
              (a, b) =>
                fs.statSync(path.join(reportDir, b)).mtimeMs - fs.statSync(path.join(reportDir, a)).mtimeMs,
            );
        } catch {
          return [];
        }
      };
      while (Date.now() - invocationStart < timeout * 1000) {
        const exitMsg = processExitMessage("Client");
        if (exitMsg) break;
        const reports = listReports();
        if (reports.length > 0) {
          const newest = path.join(reportDir, reports[0]);
          const mtime = fs.statSync(newest).mtimeMs;
          if (mtime > invocationStart && Date.now() - mtime > 2000) {
            await sleep(3000);
            break;
          }
        }
        await sleep(2000);
      }

      // Cleanup
      const ping2 = await httpGet(port, "/ping");
      if (ping2 !== null) {
        await httpPost(port, "/close");
        await pollUntilGone(port, 15);
      }
      proc = null;
      procVersion = null;
      procExited = null;

      // Parse report
      const reports = listReports();
      if (reports.length === 0) return textResult("⚠️ No report file found.");
      try {
        const report = JSON.parse(fs.readFileSync(path.join(reportDir, reports[0]), "utf-8")) as {
          totalTests?: number;
          passed?: number;
          failed?: number;
          timestamp?: string;
          entries?: {
            status?: string;
            className?: string;
            durationMs?: number;
            description?: string;
            error?: { message?: string };
          }[];
        };
        const total = report.totalTests ?? 0;
        const passed = report.passed ?? 0;
        const failed = report.failed ?? 0;
        const lines = [
          `📋 Clientsmoke Report (${report.timestamp ?? "?"}) [${version}]`,
          `   Total: ${total}  |  ✅ Passed: ${passed}  |  ❌ Failed: ${failed}`,
        ];
        for (const e of report.entries ?? []) {
          const icon = e.status === "passed" ? "✅" : "❌";
          const name = (e.className ?? "?").split(".").pop() ?? "?";
          lines.push(`   ${icon} ${name} (${e.durationMs ?? 0}ms) — ${e.description ?? ""}`);
          if (e.status === "failed" && e.error) {
            lines.push(`      ↳ ${e.error.message ?? "?"}`);
          }
        }
        lines.push(failed ? `\n❌ ${failed} FAILED` : `\n✅ All ${passed} PASSED`);
        return textResult(lines.join("\n"));
      } catch (e) {
        return textResult(`⚠️ Failed to read report: ${e instanceof Error ? e.message : String(e)}`);
      }
    },
  });

  pi.logger.info("mcmcp extension initialized", { version: EXT_VERSION });
}
