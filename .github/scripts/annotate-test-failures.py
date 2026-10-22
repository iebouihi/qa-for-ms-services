#!/usr/bin/env python3
"""Turns failed JUnit results (Surefire, Failsafe, Playwright) and Maven errors into
GitHub annotations, so a red build explains itself on the PR page without opening logs."""
import glob
import sys
import xml.etree.ElementTree as ET

MAX = 40
emitted = 0


def esc(text: str) -> str:
    return text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")


def emit(title: str, message: str) -> None:
    global emitted
    if emitted < MAX:
        print(f"::error title={esc(title)[:200]}::{esc(message)[:3000]}")
        emitted += 1


for path in sorted(glob.glob("**/TEST-*.xml", recursive=True) + glob.glob("**/results/junit.xml", recursive=True)):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for case in root.iter("testcase"):
        for kind in ("failure", "error"):
            node = case.find(kind)
            if node is not None:
                detail = (node.get("message") or "") + "\n" + (node.text or "")
                emit(f"{case.get('classname')}.{case.get('name')}", detail.strip()[:3000])

for log in sys.argv[1:]:
    if log.startswith("tail:"):  # emit the end of a log as-is (e.g. docker compose output)
        try:
            tail = open(log[5:], encoding="utf-8", errors="replace").read().splitlines()[-50:]
            if tail:
                emit(f"Last lines of {log[5:]}", "\n".join(l[:300] for l in tail))
        except OSError:
            pass
        continue
    try:
        lines = open(log, encoding="utf-8", errors="replace").read().splitlines()
    except OSError:
        continue
    errors = [l for l in lines if l.startswith("[ERROR]") or "Caused by:" in l]
    if errors:
        emit(f"Build errors in {log}", "\n".join(errors[:60]))
    # Application log lines (Spring Boot format) that usually explain a failing component test.
    app = [l for l in lines if (" ERROR " in l or " WARN " in l) and not l.startswith("[")]
    if app:
        emit(f"Application warnings/errors in {log}", "\n".join(l[:400] for l in app[-40:]))

print(f"{emitted} annotation(s) emitted")
