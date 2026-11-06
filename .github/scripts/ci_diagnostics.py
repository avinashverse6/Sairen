#!/usr/bin/env python3
"""Aggregate CI failures into GitHub check-run annotations.

Actions job logs are served from a signed object-store URL that is not reachable
from every environment. Check-run annotations, however, are plain REST data, so
this script turns the interesting parts of the Gradle output into `::error::`
commands: Kotlin compile errors, failing unit tests (from the JUnit XML), lint
errors, aapt2/resource-link errors and the APK native-library report.

GitHub truncates every annotation message to 4096 characters, so each fact gets
its own small annotation instead of one large blob.

The workflow passes each build step's `outcome` in the environment, so only
steps that actually failed are reported on.

Usage (inside the workflow):
    python3 .github/scripts/ci_diagnostics.py
"""

from __future__ import annotations

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

MAX_ANNOTATION_CHARS = 3_900
TAIL_LINES = 25
LOG_FILES = ("unit-tests.log", "lint.log", "assemble.log")

STEPS = (
    ("Unit tests", "TEST_OUTCOME", "unit-tests.log"),
    ("Lint", "LINT_OUTCOME", "lint.log"),
    ("Assemble debug APK", "ASSEMBLE_OUTCOME", "assemble.log"),
)


def escape(text: str) -> str:
    """Escape per GitHub workflow-command rules."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def clip(text: str, limit: int = MAX_ANNOTATION_CHARS) -> str:
    text = text.strip()
    return text if len(text) <= limit else text[:limit] + "\n… (truncated)"


def emit(level: str, title: str, message: str) -> None:
    message = message.strip()
    if not message:
        return
    print(f"::{level} title={title}::{escape(clip(message))}")


def read_text(name: str) -> str:
    if not os.path.exists(name):
        return ""
    with open(name, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def what_went_wrong(log: str) -> str:
    """The concise `* What went wrong:` section of a Gradle failure."""
    lines = log.splitlines()
    for index, line in enumerate(lines):
        if line.startswith("FAILURE: Build failed with an exception."):
            block = []
            for follow in lines[index:]:
                if follow.startswith(("* Try:", "* Exception is:", "BUILD FAILED")):
                    break
                block.append(follow)
            return "\n".join(block)
    return ""


def error_lines(log: str) -> list[str]:
    """Lines that carry the actual tool error (Kotlin, aapt2, lint, Gradle)."""
    collected = []
    for line in log.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("e: "):
            collected.append(stripped[:400])
        elif re.search(r"(?i)\bAAPT\d*\b|Android resource linking failed", stripped):
            collected.append(stripped[:400])
        elif re.search(r"(?i)^(error|fatal):|:\s*error:", stripped):
            collected.append(stripped[:400])
        elif re.search(r"(?i)Unresolved reference|Caused by: .*(Exception|Error)", stripped):
            collected.append(stripped[:400])
    return list(dict.fromkeys(collected))


def junit_failures() -> list[str]:
    failures: list[str] = []
    for path in sorted(glob.glob("app/build/test-results/**/TEST-*.xml", recursive=True)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in root.iter("testcase"):
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            if problem is None:
                continue
            name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
            message = re.sub(r"\s+", " ", (problem.get("message") or problem.text or "")).strip()
            failures.append(f"{name} :: {message[:500]}")
    return failures


def lint_errors() -> list[str]:
    errors: list[str] = []
    for path in sorted(glob.glob("app/build/reports/lint-results-*.txt")):
        for line in read_text(path).splitlines():
            if re.search(r":\s*(Error|Fatal):\s", line):
                errors.append(line.strip()[:500])
    for line in read_text("lint.log").splitlines():
        if "Lint found" in line and "error" in line:
            errors.append(line.strip()[:500])
    return list(dict.fromkeys(errors))


def apk_report() -> str:
    apks = sorted(glob.glob("app/build/outputs/apk/debug/*.apk"))
    if not apks:
        return ""
    sizes = [f"{os.path.basename(p)} ({os.path.getsize(p) // 1024} KiB)" for p in apks]
    listing = read_text("apk-contents.txt")
    libs = [line.strip() for line in listing.splitlines() if re.search(r"lib/|\.so$", line)]
    if libs:
        return "APK: " + ", ".join(sizes) + "\nNative libraries:\n" + "\n".join(libs)
    return "APK: " + ", ".join(sizes) + "\nNo lib/ entries and no .so files: no native code in the APK."


def failed_steps() -> list[tuple[str, str, str]]:
    """Return (step name, outcome, log file) for steps whose outcome was not success."""
    result = []
    for name, env_var, log_name in STEPS:
        outcome = os.environ.get(env_var, "success")
        if outcome not in ("success", ""):
            result.append((name, outcome, log_name))
    return result


def main() -> int:
    failures = junit_failures()
    if failures:
        emit("error", "Failing unit tests", "Failing test cases:\n" + "\n".join(failures))

    lint = lint_errors()
    if lint:
        emit("error", "Android lint errors", "\n".join(lint))

    for step_name, outcome, log_name in failed_steps():
        log = read_text(log_name)
        errors = error_lines(log)
        if errors:
            emit("error", f"{step_name}: compiler/tool errors", "\n".join(errors))
        block = what_went_wrong(log)
        if block:
            emit("error", f"{step_name}: what went wrong", f"outcome={outcome}\n{block}")
        if not errors and not block:
            tail = "\n".join(log.splitlines()[-TAIL_LINES:]) if log else "(no log captured)"
            emit("error", f"{step_name}: log tail", f"outcome={outcome}\n{tail}")

    apk = apk_report()
    if apk:
        emit("notice", "Debug APK report", apk)
    if not failures and not lint and not failed_steps():
        emit("notice", "CI diagnostics", "Unit tests, lint and the debug APK build all succeeded.")

    print("\n--- diagnostics summary ---")
    print(f"failing tests: {len(failures)}, lint errors: {len(lint)}, "
          f"failed steps: {[s for s, _, _ in failed_steps()]}")
    for line in failures[:40]:
        print("FAILED", line)
    for line in lint[:40]:
        print("LINT", line)
    for step_name, outcome, log_name in failed_steps():
        print(f"== {step_name} (outcome={outcome}, log={log_name}) ==")
        for line in error_lines(read_text(log_name))[:40]:
            print("ERR", line)
        print(what_went_wrong(read_text(log_name)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
