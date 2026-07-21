/**
 * mcmcp 拓展 harness：以 mock ExtensionAPI 加载工厂，驱动注册的工具。
 * 用法：bun run work/mcmcp-refactor/harness.ts [unit|e2e]
 */
import factory from "C:/Users/q2437/.omp/agent/extensions/mcmcp/index.ts";

// 链式 zod stub：任意方法调用返回自身
const zodStub: unknown = new Proxy(function () {}, {
  get: (_t, _p) => zodStub,
  apply: (_t, _this, _args) => zodStub,
});

interface ToolDef {
  name: string;
  description: string;
  execute: (
    id: string,
    params: Record<string, unknown>,
    signal: undefined,
    onUpdate: undefined,
    ctx: unknown,
  ) => Promise<{ content: { type: string; text: string }[] }>;
}

const tools = new Map<string, ToolDef>();
const pi = {
  zod: zodStub,
  logger: { info: () => {}, warn: () => {}, error: () => {} },
  setLabel: () => {},
  registerTool: (d: ToolDef) => tools.set(d.name, d),
  on: () => {},
};
// @ts-expect-error mock 不完整，仅覆盖工厂用到的面
factory(pi);

const ctx = {
  cwd: "E:\\_ideaProjects\\qylEyelib",
  ui: { notify: () => {} },
  hasUI: false,
  isIdle: () => true,
};

async function call(name: string, params: Record<string, unknown> = {}): Promise<string> {
  const tool = tools.get(name);
  if (!tool) throw new Error(`tool not registered: ${name}`);
  const result = await tool.execute("t1", params, undefined, undefined, ctx);
  return result.content.map((c) => c.text).join("\n");
}

const mode = process.argv[2] ?? "unit";

if (mode === "unit") {
  console.log("registered tools:", [...tools.keys()].join(", "));
  const expected = [
    "mcmcp_launch", "mcmcp_close", "mcmcp_status", "mcmcp_enter_world",
    "mcmcp_execute", "mcmcp_send_command", "mcmcp_build", "mcmcp_test",
    "mcmcp_nullaway", "mcmcp_clientsmoke",
  ];
  const missing = expected.filter((t) => !tools.has(t));
  console.log(missing.length === 0 ? "✅ all 10 tools registered" : `❌ missing: ${missing}`);

  console.log("\n--- status (no server expected) ---");
  console.log(await call("mcmcp_status", { info: "all" }));

  console.log("\n--- launch with unknown version ---");
  console.log(await call("mcmcp_launch", { version: "nonexistent", timeout: 5 }));

  console.log("\n--- execute without world (guard) ---");
  console.log(await call("mcmcp_execute", { code: 'return "x";' }));
} else if (mode === "close") {
  console.log(await call("mcmcp_close", {}));
} else if (mode === "e2e") {
  console.log("--- launch ---");
  console.log(await call("mcmcp_launch", { timeout: 180 }));
  console.log("--- status ---");
  console.log(await call("mcmcp_status", { info: "all" }));
  console.log("--- enter_world ---");
  console.log(await call("mcmcp_enter_world", { timeout: 90 }));
  console.log("--- execute ---");
  console.log(await call("mcmcp_execute", { code: 'return "hello from mcmcp";' }));
  console.log("--- send_command ---");
  console.log(await call("mcmcp_send_command", { side: "server", command_text: "time set day" }));
  console.log("--- close ---");
  console.log(await call("mcmcp_close", {}));
}
