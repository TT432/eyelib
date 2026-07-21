# 会话状态恢复

mcmcp 拓展（`~/.omp/agent/extensions/mcmcp/index.ts`）**无状态机**：所有状态都从 AIDebugServer
端点实时查询（`readGameState`），不存在旧 Python MCP「状态只降级不恢复」的问题。

## 历史

旧 `eyelib-debug` Python MCP（`scripts/eyelib_debug_mcp.py`，已删除）维护内部状态机，
launch 超时中断后状态可能卡在 IDLE/ERROR，需要 `mcmcp_status()` 的 auto-recovery 修复。
omp 拓展化后该问题整体消失。

## 排查流程

当工具返回状态守卫错误（如 `❌ Game must be at title screen (loaded)`）时：

1. 调 `mcmcp_status(info="all")` 查看实时状态（每次都是新查询，无缓存）
2. 若与预期不符，用 curl 验证游戏实际状态（默认端口 25999）：
   ```bash
   curl -s --proxy http://127.0.0.1:10808 http://localhost:25999/ping
   curl -s --proxy http://127.0.0.1:10808 http://localhost:25999/loaded
   ```
3. ping 不通但端口被占用 → 僵尸进程，`mcmcp_close()` 后重新 launch
4. ping 不通且端口空闲 → 客户端未以 `ai_debug_port` 启动（AIDebugServer 默认不开启）
