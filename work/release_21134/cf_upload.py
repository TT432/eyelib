#!/usr/bin/env python3
"""Upload eyelib 21.1.34 jars (three Stonecutter nodes) to the CurseForge legacy Upload API.

Usage: python work/release_21134/cf_upload.py [--dry-run] [node ...]
  node = 1.20.1 | 1.21.1 | 26.1.2
"""
import json
import os
import sys

import requests

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PROJECT_ID = 1388469
VERSION = "21.1.34"
CHANGELOG = os.path.join(ROOT, "work", "release_21134", "cf_changelog.md")
IDS_OUT = os.path.join(ROOT, "work", "release_21134", "cf_ids.json")
M2 = "C:/Users/q2437/.m2/repository/io/github/tt432/eyelib"

NODES = [
    ("1.20.1", "forge", ["Client", "Server", "1.20.1", "Forge"]),
    ("1.21.1", "neoforge", ["Client", "Server", "1.21.1", "NeoForge"]),
    ("26.1.2", "neoforge", ["Client", "Server", "26.1.2", "NeoForge"]),
]
HOSTS = ["https://www.curseforge.com", "https://minecraft.curseforge.com"]


def token():
    path = "C:/Users/q2437/.gradle/gradle.properties"
    value = None
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            if line.strip().startswith("CURSE_TOKEN="):
                value = line.split("=", 1)[1].strip()
    if not value:
        raise SystemExit("CURSE_TOKEN not found")
    return value


def jar_path(mc, loader):
    version = "%s+%s-%s" % (VERSION, mc, loader)
    return "%s/%s/eyelib-%s.jar" % (M2, version, version)


def upload(tok, metadata, jar):
    errors = []
    session = requests.Session()
    session.trust_env = False
    headers = {
        "X-Api-Token": tok,
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36",
        "Accept": "*/*",
    }
    for host in HOSTS:
        url = "%s/api/projects/%d/upload-file" % (host, PROJECT_ID)
        try:
            with open(jar, "rb") as fh:
                resp = session.post(
                    url,
                    headers=headers,
                    data={"metadata": json.dumps(metadata, ensure_ascii=False)},
                    files={"file": (os.path.basename(jar), fh, "application/java-archive")},
                    timeout=900,
                )
        except requests.RequestException as exc:
            errors.append("%s -> %r" % (host, exc))
            continue
        if resp.status_code == 200:
            return resp.json(), None
        errors.append("%s -> HTTP %s %s" % (host, resp.status_code, resp.text[:200]))
    return None, "; ".join(errors)


def main(argv):
    dry = "--dry-run" in argv
    wanted = [a for a in argv if not a.startswith("-")]
    tok = token()
    changelog = open(CHANGELOG, encoding="utf-8").read()
    ids = {}
    if os.path.exists(IDS_OUT):
        ids = json.load(open(IDS_OUT, encoding="utf-8"))

    for mc, loader, game_versions in NODES:
        if wanted and mc not in wanted:
            continue
        jar = jar_path(mc, loader)
        if not os.path.exists(jar):
            raise SystemExit("missing jar: " + jar)
        version = "%s+%s-%s" % (VERSION, mc, loader)
        if version in ids:
            print("skip existing %s fileId %s" % (version, ids[version]), flush=True)
            continue
        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": "eyelib-" + version,
            "releaseType": "release",
            "gameVersionNames": game_versions,
        }
        print("== %s (%d bytes)" % (version, os.path.getsize(jar)), flush=True)
        if dry:
            print(json.dumps(metadata, ensure_ascii=False)[:300])
            continue
        result, error = upload(tok, metadata, jar)
        if result is None:
            raise SystemExit("%s failed: %s" % (version, error))
        file_id = result.get("id")
        ids[version] = file_id
        json.dump(ids, open(IDS_OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
        print("  fileId %s" % file_id, flush=True)
    print(json.dumps(ids, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main(sys.argv[1:])
