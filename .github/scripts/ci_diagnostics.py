#!/usr/bin/env python3
"""Aggregate CI failures into GitHub check-run annotations.

Actions job logs are served from a signed object-store URL that cannot be fetched
from every environment. Check-run annotations, however, are plain REST data, so
this script turns the interesting parts of the Gradle output into `::error::`
commands: compile errors, failing unit tests (from the JUnit XML), lint errors
and the APK native-library report.

Usage (inside the workflow):
    python3 .github/scripts/ci_diagnostics.py
"""

from __future__ import annotations

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

MAX_ANNOTATION_CHARS = 12_000
LOG_FILES = ("unit-tests.log", "lint.log", "assemble.log")


def escape(text: str) -> str:
    """Escape per GitHub workflow-command rules."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def emit(level: str, title: str, message: str) -> None:
    message = message.strip()
    if not message:
        return
    if len(message) > MAX_ANNOTATION_CHARS:
        message = message[:MAX_ANNOTATION_CHARS] + "\n… (truncated)"
    print(f"::{level} title={title}::{escape(message)}")


def read_log(name: str) -> str:
    if not os.path.exists(name):
        return ""
    with open(name, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def section(name: str) -> str:
    """Return the text of all logs so cross-cutting greps stay simple."""
    return "\n".join(read_log(name) for name in (name,) if name)


def kotlin_compile_errors(logs: str) -> list[str]:
    errors = []
    for line in logs.splitlines():
        stripped = line.strip()
        if re.match(r"^e: (file|.*\.kt)", stripped) or stripped.startswith("e: "):
            errors.append(stripped)
    # Keep order, drop duplicates.
    return list(dict.fromkeys(errors))


def gradle_failures(logs: str) -> list[str]:
    matches = []
    for match in re.finditer(
        r"(FAILURE: Build failed with an exception\.(?:\n.+?){0,25})", logs
    ):
        matches.append(match.group(1))
    for match in re.finditer(r"^> Task .* FAILED$", logs, re.MULTILINE):
        matches.append(match.group(0))
    return list(dict.fromkeys(matches))


def junit_failures() -> list[str]:
    failures = []
    patterns = [
        "app/build/test-results/**/TEST-*.xml",
        "app/build/test-results/**/*.xml",
    ]
    seen: set[str] = set()
    for pattern in patterns:
        for path in sorted(glob.glob(pattern, recursive=True)):
            if path in seen:
                continue
            seen.add(path)
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
                message = (problem.get("message") or problem.text or "").strip()
                message = re.sub(r"\s+", " ", message)
                failures.append(f"{name} :: {message[:600]}")
    return failures


def lint_errors() -> list[str]:
    errors: list[str] = []
    for path in glob.glob("app/build/reports/lint-results-*.txt"):
        with open(path, encoding="utf-8", errors="replace") as handle:
            for line in handle:
                if re.search(r":\s*(Error|Fatal):\s", line):
                    errors.append(line.strip())
    log = read_log("lint.log")
    for line in log.splitlines():
        if "Lint found" in line and "error" in line:
            errors.append(line.strip())
    return list(dict.fromkeys(errors))


def apk_report() -> str:
    if not os.path.exists("apk-contents.txt"):
        return ""
    with open("apk-contents.txt", encoding="utf-8", errors="replace") as handle:
        listing = handle.read()
    libs = [line.strip() for line in listing.splitlines() if re.search(r"lib/|\.so$", line)]
    apks = glob.glob("app/build/outputs/apk/debug/*.apk")
    sizes = [f"{os.path.basename(p)} ({os.path.getsize(p) // 1024} KiB)" for p in apks]
    if libs:
        return "APK: " + ", ".join(sizes) + "\nNative libraries:\n" + "\n".join(libs)
    return (
        "APK: "
        + (", ".join(sizes) or "MISSING")
        + "\nNo lib/ entries and no .so files: the APK contains no native code."
    )


def main() -> int:
    logs = "\n".join(read_log(name) for name in LOG_FILES)
    failures = junit_failures()

    if failures:
        emit("error", "Unit tests failed", "\n".join(failures))
    compile_errors = kotlin_compile_errors(logs)
    if compile_errors:
        emit("error", "Kotlin compilation errors", "\n".join(compile_errors))
    lint = lint_errors()
    if lint:
        emit("error", "Android lint errors", "\n".join(lint))
    if not failures and compile_errors:
        pass
    elif not failures and not compile_errors:
        build_errors = gradle_failures(logs)
        if build_errors:
            emit("error", "Gradle failure", "\n\n".join(build_errors))
    apk = apk_report()
    if apk:
        emit("notice", "Debug APK report", apk)
    if not (failures or compile_errors or lint):
        emit("notice", "CI diagnostics", "No test, compile or lint failures found in this run.")

    # Human-readable tail for the step log itself.
    print("\n--- diagnostics summary ---")
    print(f"failing tests: {len(failures)}, compile errors: {len(compile_errors)}, lint errors: {len(lint)}")
    for line in failures[:40]:
        print("FAILED", line)
    for line in compile_errors[:40]:
        print("COMPILE", line)
    for line in lint[:40]:
        print("LINT", line)
    return 0


if __name__ == "__main__":
    sys.exit(main())
