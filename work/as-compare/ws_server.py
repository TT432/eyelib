"""基岩版 /wsserver 控制桥。
MC Bedrock 作为 WS 客户端连入本服务。stdin 每行一条斜杠命令（不带 /），
执行结果以单行 JSON 打到 stdout（CMD_RESULT/EVENT/STATUS 前缀）。
用法: python -u ws_server.py [port]
"""
import asyncio, json, sys, uuid

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 19199
ws_conn = None
pending = {}  # requestId -> Future

def out(tag, obj):
    print(f"{tag} {json.dumps(obj, ensure_ascii=False)}", flush=True)

async def handle(ws):
    global ws_conn
    ws_conn = ws
    out("STATUS", {"connected": True, "remote": str(ws.remote_address)})
    try:
        async for raw in ws:
            try:
                msg = json.loads(raw)
            except Exception:
                out("RAW", raw[:500]); continue
            hdr = msg.get("header", {})
            purpose = hdr.get("messagePurpose")
            rid = hdr.get("requestId")
            if purpose == "commandResponse" and rid in pending:
                fut = pending.pop(rid)
                if not fut.done():
                    fut.set_result(msg.get("body", {}))
            elif purpose == "event":
                out("EVENT", msg.get("body", {}))
            elif purpose == "error":
                out("ERROR", msg.get("body", {}))
                if rid in pending:
                    fut = pending.pop(rid)
                    if not fut.done():
                        fut.set_result({"error": msg.get("body", {})})
            else:
                out("MSG", msg)
    finally:
        ws_conn = None
        out("STATUS", {"connected": False})

async def send_command(line):
    if ws_conn is None:
        out("CMD_RESULT", {"cmd": line, "ok": False, "error": "not connected"})
        return {"ok": False, "error": "not connected"}
    rid = str(uuid.uuid4())
    fut = asyncio.get_event_loop().create_future()
    pending[rid] = fut
    payload = {
        "body": {"origin": {"type": "player"}, "commandLine": line, "version": 1},
        "header": {"requestId": rid, "messagePurpose": "commandRequest",
                   "version": 1, "messageType": "commandRequest"},
    }
    await ws_conn.send(json.dumps(payload))
    try:
        body = await asyncio.wait_for(fut, timeout=10)
    except asyncio.TimeoutError:
        pending.pop(rid, None)
        body = {"ok": False, "error": "timeout"}
    body = body or {}
    body.setdefault("ok", body.get("statusCode", -1) == 0)
    body["cmd"] = line
    out("CMD_RESULT", body)
    return body

import asyncio, json, sys, uuid, threading

def stdin_thread(loop):
    """Windows Proactor 无法对 hub 管道 stdin 用 connect_read_pipe，用阻塞读线程转发。"""
    while True:
        line = sys.stdin.readline()
        if not line:
            import time; time.sleep(1); continue
        line = line.strip().lstrip("/")
        if line:
            asyncio.run_coroutine_threadsafe(send_command(line), loop)

def http_thread(loop, port=19200):
    """HTTP 控制口：POST /cmd (text/plain) → 同步返回 MC 执行结果 JSON；GET /status。"""
    from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

    class H(BaseHTTPRequestHandler):
        def log_message(self, *a):
            pass

        def do_GET(self):
            if self.path == "/status":
                self._json({"connected": ws_conn is not None})
            else:
                self.send_error(404)

        def do_POST(self):
            if self.path != "/cmd":
                self.send_error(404); return
            n = int(self.headers.get("Content-Length", 0))
            cmd = self.rfile.read(n).decode("utf-8", "replace").strip().lstrip("/")
            if not cmd:
                self._json({"ok": False, "error": "empty"}); return
            f = asyncio.run_coroutine_threadsafe(send_command(cmd), loop)
            try:
                body = f.result(timeout=15)
            except Exception as e:
                body = {"ok": False, "error": str(e)}
            self._json(body)

        def _json(self, obj):
            data = json.dumps(obj, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    ThreadingHTTPServer(("127.0.0.1", port), H).serve_forever()

async def main():
    import websockets
    out("STATUS", {"listening": PORT})
    loop = asyncio.get_event_loop()
    threading.Thread(target=stdin_thread, args=(loop,), daemon=True).start()
    threading.Thread(target=http_thread, args=(loop,), daemon=True).start()
    async with websockets.serve(handle, "localhost", PORT):
        await asyncio.Event().wait()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
