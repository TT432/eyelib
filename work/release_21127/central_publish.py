#!/usr/bin/env python3
"""Publish eyelib nodes to Maven Central via the Central Portal bundle API.

Usage:
  python work/release_21126/central_publish.py [--dry-run] [--nodes-only a,b] [node ...]

Each node is uploaded as its own bundle (Maven repo layout zip, checksums generated
in-memory because mavenLocal publishing does not produce .md5/.sha1).
"""
import base64
import hashlib
import io
import sys
import time
import zipfile

import requests

# 本机代理会重置到 central.sonatype.com 的连接（ProxyError 10054）；强制直连
_SESSION = requests.Session()
_SESSION.trust_env = False

GROUP_PATH = "io/github/tt432/eyelib"
VERSION = "21.1.27"
NODES = [
    "21.1.27+1.20.1-forge",
    "21.1.27+1.21.1-neoforge",
    "21.1.27+26.1.2-neoforge",
]
M2 = "%s/.m2/repository/%s" % ("C:/Users/q2437", GROUP_PATH)
PORTAL = "https://central.sonatype.com/api/v1/publisher"
EXPECTED_PER_NODE = 10


def read_props(path):
    props = {}
    with open(path, encoding="utf-8") as fh:
        for raw in fh:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()  # last occurrence wins
    return props


def creds():
    # Gradle/Properties semantics: the last duplicate key wins.
    props = read_props("C:/Users/q2437/.gradle/gradle.properties")
    user = props.get("ossrhUsername")
    password = props.get("ossrhPassword")
    if not user or not password:
        raise SystemExit("ossrhUsername/ossrhPassword missing in ~/.gradle/gradle.properties")
    return user, password


def build_bundle(node):
    src = "%s/%s" % (M2, node)
    import os

    names = sorted(os.listdir(src))
    payload = [n for n in names if not n.endswith((".md5", ".sha1"))]
    if len(payload) != EXPECTED_PER_NODE:
        raise SystemExit("%s: expected %d files, found %d -> %s"
                         % (node, EXPECTED_PER_NODE, len(payload), names))
    missing_asc = [n for n in payload if not n.endswith(".asc")
                   and n + ".asc" not in payload]
    if missing_asc:
        raise SystemExit("%s: unsigned artifacts %s" % (node, missing_asc))

    buf = io.BytesIO()
    entry = "%s/%s" % (GROUP_PATH, node)
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        for name in payload:
            with open("%s/%s" % (src, name), "rb") as fh:
                data = fh.read()
            zf.writestr("%s/%s" % (entry, name), data)
            # Portal requires md5+sha1 sidecars for every published file.
            zf.writestr("%s/%s.md5" % (entry, name), hashlib.md5(data).hexdigest())
            zf.writestr("%s/%s.sha1" % (entry, name), hashlib.sha1(data).hexdigest())
    return buf.getvalue()


def upload(user, password, node, bundle):
    resp = _SESSION.post(
        PORTAL + "/upload",
        params={"name": "eyelib-" + node, "publishingType": "AUTOMATIC"},
        files={"bundle": ("eyelib-%s.zip" % node, bundle, "application/octet-stream")},
        auth=(user, password),
        timeout=600,
    )
    body = resp.text
    print("  upload HTTP %s body=%r" % (resp.status_code, body[:200]), flush=True)
    if resp.status_code not in (200, 201):
        raise SystemExit("%s upload HTTP %s: %s" % (node, resp.status_code, body[:500]))
    try:
        return resp.json()["deploymentId"]
    except ValueError:
        # Portal may answer with the bare deployment id, or with extra trailing data.
        import json as _json
        import re
        match = re.search(r'"deploymentId"\s*:\s*"([^"]+)"', body)
        if match:
            return match.group(1)
        stripped = body.strip().strip('"')
        if stripped and " " not in stripped and "<" not in stripped and len(stripped) < 200:
            return stripped
        raise SystemExit("%s: cannot parse deploymentId from %r" % (node, body[:300]))


def status(user, password, deployment_id):
    token = base64.b64encode(("%s:%s" % (user, password)).encode()).decode()
    resp = _SESSION.post(
        PORTAL + "/status",
        params={"id": deployment_id},
        headers={"Authorization": "Bearer " + token},
        timeout=60,
    )
    if resp.status_code != 200:
        return {"state": "HTTP_%s" % resp.status_code, "errors": [resp.text[:300]]}
    return resp.json()


def wait_published(user, password, node, deployment_id, timeout_min=45, interval=20):
    deadline = time.time() + timeout_min * 60
    last = None
    while time.time() < deadline:
        info = status(user, password, deployment_id)
        state = info.get("deploymentState", info.get("state"))
        if state != last:
            print("  [%s] %s" % (node, state), flush=True)
            last = state
        if state == "PUBLISHED":
            return True, info
        if state == "FAILED":
            return False, info
        time.sleep(interval)
    return False, {"deploymentState": "TIMEOUT", "last": last}


def main(argv):
    dry = "--dry-run" in argv
    if "--check" in argv:
        dep = argv[argv.index("--check") + 1]
        user, password = creds()
        ok, info = wait_published(user, password, dep, dep)
        print("published" if ok else "not-published")
        print(info)
        return 0 if ok else 1
    nodes = [a for a in argv if not a.startswith("-")] or NODES
    if "--nodes-only" in argv:
        wanted = set(argv[argv.index("--nodes-only") + 1].split(","))
        nodes = [n for n in NODES if n in wanted]

    user, password = creds()
    results = {}
    for node in nodes:
        print("== %s" % node, flush=True)
        bundle = build_bundle(node)
        print("  bundle %.1f MB" % (len(bundle) / 1e6), flush=True)
        if dry:
            continue
        dep = upload(user, password, node, bundle)
        print("  deploymentId %s" % dep, flush=True)
        ok, info = wait_published(user, password, node, dep)
        results[node] = {"deploymentId": dep, "published": ok, "info": info}
        print("  -> %s" % ("PUBLISHED" if ok else "FAILED/TIMEOUT"), flush=True)
        if not ok:
            print("  errors: %s" % (info.get("errors") or info), flush=True)
    print(results)
    return 0 if dry or all(r["published"] for r in results.values()) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
