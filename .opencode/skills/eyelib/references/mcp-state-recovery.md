# MCP 状态机自动恢复

`eyelib-debug` MCP server (`scripts/eyelib_debug_mcp.py`) 维护一个状态机，但存在一个已知问题：`eyelib_debug_status()` **只降级不恢复**。

## 问题

```python
# 原逻辑：ping 不通 → ERROR，但 ping 通时不从 IDLE/ERROR 恢复到 LOADED/IN_WORLD
if ping is None and session.state not in (S.IDLE, S.ERROR):
    session.state = S.ERROR
```

结果：`eyelib_debug_launch` 超时被中断后，session.state 可能卡在 IDLE/ERROR，即使游戏实际已运行。后续 `eyelib_debug_enter_world` 等工具被 guard 拒绝。

## 修复（2026-06-05）

在 `eyelib_debug_status()` 开头添加 auto-recovery 逻辑：

```python
# Auto-recovery: if client is reachable but state is pessimistic, re-detect
if ping is not None and session.state in (S.IDLE, S.ERROR, S.CLOSING):
    loaded_data = await _http_get("/loaded")
    if loaded_data and loaded_data.get("loaded") is True:
        enter_data = await _http_get("/enterdworld")
        if enter_data and enter_data.get("inWorld") is True:
            session.state = S.IN_WORLD
            session.dimension = enter_data.get("dimension", session.dimension)
            session.error = ""
        else:
            session.state = S.LOADED
            session.error = ""
```

## 排查流程

当 MCP 工具返回状态守卫错误（如 `❌ Not allowed in state 'idle'`）时：

1. 先调 `eyelib_debug_status(info="all")` 触发 auto-recovery
2. 检查状态是否恢复
3. 若恢复失败，用 curl 验证游戏实际状态：
   ```bash
   curl -s --proxy http://127.0.0.1:10808 http://localhost:25999/ping
   curl -s --proxy http://127.0.0.1:10808 http://localhost:25999/loaded
   ```
4. 若游戏实际不存在（ping 不通），先 `eyelib_debug_close()` 再 launch
