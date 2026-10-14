#!/usr/bin/env python3
"""Read-only ADB inventory for identifying the factory surround-view pipeline.

No camera session, activity launch, setting write or root
command is issued. The user opens/closes the factory UI manually between runs.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import tempfile
import zipfile

HINT = re.compile(r"avm|surround|aroundview|camera|parking|parkassist|ecarx|zeekr|evs|automotive\.sv", re.I)
FOCUS = re.compile(r"mCurrentFocus|mFocusedApp|topResumedActivity|mResumedActivity|Window #")
PROPERTIES = (
    "ro.build.display.id", "ro.build.fingerprint", "ro.build.version.release",
    "ro.build.version.sdk", "ro.board.platform", "ro.hardware", "ro.vndk.version",
)


def invoke(argv, timeout=15):
    try:
        r = subprocess.run(argv, capture_output=True, text=True, timeout=timeout, errors="replace")
        return {"exit_code": r.returncode, "stdout": r.stdout, "stderr": r.stderr}
    except subprocess.TimeoutExpired:
        return {"exit_code": None, "stdout": "", "stderr": "Timed out; not evidence that the interface is absent"}
    except OSError as e:
        return {"exit_code": None, "stdout": "", "stderr": str(e)}


def shell(adb, serial, tokens):
    # ADB's shell side interprets arguments again; quote each token explicitly.
    return invoke([adb, "-s", serial, "shell", " ".join(shlex.quote(x) for x in tokens)])


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--adb", default=shutil.which("adb") or "adb", help="ADB executable")
    p.add_argument("--serial", help="Device serial; required if several devices are connected")
    p.add_argument("--label", default="inventory", help="State label, e.g. idle or factory-360")
    p.add_argument("--out", type=Path, help="New output directory; default is a temporary directory")
    p.add_argument("--package", help="Factory package identified from a previous inventory")
    p.add_argument("--pull-package", action="store_true", help="Copy only the selected package's readable APKs")
    args = p.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,40}", args.label):
        p.error("Use letters, numbers, hyphens or underscores for --label")
    if args.package and not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+", args.package):
        p.error("--package must be an Android package name")
    if args.pull_package and not args.package:
        p.error("--pull-package requires an identified --package")
    listing = invoke([args.adb, "devices"])
    if listing["exit_code"] != 0:
        p.error("ADB could not list devices: " + listing["stderr"])
    states = {}
    for line in listing["stdout"].splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2:
            states[parts[0]] = parts[1]
    serial = args.serial
    if serial is None:
        if len(states) != 1:
            p.error("Connect and authorize one head unit, or select it with --serial")
        serial = next(iter(states))
    if states.get(serial) != "device":
        p.error("Selected device is not connected and authorized (state: " + states.get(serial, "missing") + ")")
    out = args.out.expanduser().resolve() if args.out else Path(tempfile.mkdtemp(prefix="zeekr-360-"))
    archive = out.parent / (out.name + ".zip")
    if archive.exists() or (args.out and out.exists()):
        p.error("Choose a new --out path; existing reports will not be overwritten")
    out.mkdir(parents=True, exist_ok=True)
    records = []

    def collect(name, tokens, pattern=None):
        result = shell(args.adb, serial, tokens)
        raw = result["stdout"]
        if pattern:
            result["stdout"] = "\n".join(x for x in raw.splitlines() if pattern.search(x)) + "\n"
        (out / (name + ".json")).write_text(json.dumps({"command": tokens, **result}, indent=2), encoding="utf-8")
        records.append({"name": name, "exit_code": result["exit_code"], "stderr": result["stderr"]})
        print(name + ": " + ("collected" if result["exit_code"] == 0 else "unavailable; see report"))
        return raw

    for prop in PROPERTIES:
        collect(prop, ["getprop", prop])
    packages = collect("package_candidates", ["pm", "list", "packages", "-f"], HINT)
    collect("binder_services", ["service", "list"])
    collect("hal_services", ["lshal"])
    collect("camera_service", ["dumpsys", "media.camera"])
    windows = collect("window_focus", ["dumpsys", "window", "windows"], FOCUS)
    activities = collect("activity_focus", ["dumpsys", "activity", "activities"], FOCUS)
    collect("process_candidates", ["ps", "-A"], HINT)
    for variable in ["BOOTCLASSPATH", "SYSTEMSERVERCLASSPATH", "DEX2OATBOOTCLASSPATH"]:
        collect(variable, ["printenv", variable])
    collect("framework_inventory", ["ls", "/system/framework", "/system_ext/framework", "/product/framework", "/vendor/framework"])
    collect("vendor_manifest", ["cat", "/vendor/etc/vintf/manifest.xml"])
    hints = sorted(set(x.strip() for text in [packages, windows, activities] for x in text.splitlines() if HINT.search(x)))
    if args.package:
        collect("selected_package", ["dumpsys", "package", args.package])
        paths = collect("selected_apk_paths", ["pm", "path", args.package])
        if args.pull_package:
            for i, line in enumerate(paths.splitlines()):
                if not line.startswith("package:"):
                    continue
                remote = line[len("package:"):].strip()
                if not remote.startswith("/") or not remote.endswith(".apk"):
                    continue
                target = out / "package_apks" / (str(i) + "-" + Path(remote).name)
                target.parent.mkdir(exist_ok=True)
                result = invoke([args.adb, "-s", serial, "pull", remote, str(target)], timeout=30)
                records.append({"name": "pull " + remote, **result})
    summary = {
        "captured_at_utc": datetime.now(timezone.utc).isoformat(), "label": args.label,
        "camera_sessions_opened": False, "candidate_hints": hints, "commands": records,
        "interpretation": "Names are leads only. Access failures do not prove absence. No stitched video feed was tested.",
    }
    (out / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED) as z:
        for path in sorted(out.rglob("*")):
            if path.is_file():
                z.write(path, path.relative_to(out))
    print("Report: " + str(archive))


if __name__ == "__main__":
    main()
