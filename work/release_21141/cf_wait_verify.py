#!/usr/bin/env python3
"""Poll CurseForge files/all through r.jina.ai until every expected file name shows up.

Usage: python work/release_21141/cf_wait_verify.py [--slug eyelib] [--rounds 12]
"""
import os
import sys
import time

import requests

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SLUG = "eyelib"
NAMES = [
    "eyelib-21.1.41+1.20.1-forge",
    "eyelib-21.1.41+1.21.1-neoforge",
    "eyelib-21.1.41+26.1.2-neoforge",
]
OUT = os.path.join(ROOT, "work", "release_21141", "cf-files-page.txt")
TEMPLATE = ("https://r.jina.ai/https://www.curseforge.com/minecraft/mc-mods/%s/files/all"
            "?page=1&pageSize=20&sortBy=dateCreated&sortOrder=desc&cb=%d")


def main(argv):
    rounds = 12
    if "--rounds" in argv:
        rounds = int(argv[argv.index("--rounds") + 1])
    last = ""
    for i in range(rounds):
        url = TEMPLATE % (SLUG, int(time.time()))
        try:
            resp = requests.get(url, timeout=180)
            text = resp.text
        except Exception as exc:  # transient network failure -> next round
            print(time.strftime("%H:%M:%S"), "fetch error", repr(exc)[:120], flush=True)
            time.sleep(240)
            continue
        found = [n for n in NAMES if n in text]
        print(time.strftime("%H:%M:%S"), "%d/%d" % (len(found), len(NAMES)),
              [n.split("+")[1] for n in found], flush=True)
        if len(found) == len(NAMES):
            with open(OUT, "w", encoding="utf-8") as fh:
                fh.write(text)
            print("ALL VISIBLE ->", OUT)
            return 0
        last = text
        time.sleep(240)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(last)
    print("TIMEOUT, last snapshot ->", OUT)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
