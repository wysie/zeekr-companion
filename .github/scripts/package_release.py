#!/usr/bin/env python3
"""Stage the signed release APK with the same public name locally and in CI."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil

APP_NAME = "Zeekr Shortcut — wysie Edition"
_NUMBER = r"(?:0|[1-9][0-9]*)"
VERSION = re.compile(rf"{_NUMBER}\.{_NUMBER}\.{_NUMBER}(?:-(alpha|beta)\.([1-9][0-9]*))?")


def release_identity(version):
    if not isinstance(version, str) or not VERSION.fullmatch(version):
        raise ValueError("Use X.Y.Z, X.Y.Z-alpha.N or X.Y.Z-beta.N for wysie Edition releases")
    return {
        "version": version,
        "tag": "v" + version,
        "title": APP_NAME + " " + version,
        "prerelease": "-" in version,
        "filename": f"ZeekrShortcut-wysie-{version}.apk",
    }


def stage(metadata_path, output_dir, expected_tag=None):
    metadata_path, output_dir = Path(metadata_path), Path(output_dir)
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    if metadata.get("variantName") != "release" or metadata.get("applicationId") != "io.github.dts88.zeekrshortcut":
        raise ValueError("Expected the signed wysie Edition release variant and existing application ID")
    elements = metadata.get("elements", [])
    if len(elements) != 1 or elements[0].get("filters"):
        raise ValueError("Expected exactly one universal APK")
    element = elements[0]
    result = release_identity(element["versionName"])
    if expected_tag is not None and result["tag"] != expected_tag:
        raise ValueError(f"Release tag {expected_tag} does not match built APK {result['tag']}")
    code = element.get("versionCode")
    if type(code) is not int or code <= 0:
        raise ValueError("Expected a positive Android versionCode")
    source = metadata_path.parent / element["outputFile"]
    if source.parent.resolve() != metadata_path.parent.resolve() or not source.is_file() or source.stat().st_size == 0:
        raise ValueError("Missing or invalid signed APK output")
    output_dir.mkdir(parents=True, exist_ok=True)
    target = output_dir / result["filename"]
    if source.resolve() == target.resolve():
        raise ValueError("Staging directory must differ from the Gradle APK directory")
    shutil.copyfile(source, target)
    with target.open("rb") as apk:
        digest = hashlib.file_digest(apk, "sha256").hexdigest()
    checksum = target.with_suffix(".apk.sha256")
    checksum.write_text(f"{digest}  {target.name}\n", encoding="ascii")
    result.update(path=str(target), checksum=str(checksum), sha256=digest,
                  versionCode=code, bytes=target.stat().st_size)
    (output_dir / "release-manifest.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--metadata", default="app/build/outputs/apk/release/output-metadata.json")
    parser.add_argument("--output-dir", default="app/build/edition")
    parser.add_argument("--tag", help="Require this Git tag to match the built APK version")
    args = parser.parse_args()
    result = stage(args.metadata, args.output_dir, args.tag)
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for key in ("path", "checksum", "version", "title", "tag", "prerelease"):
                value = str(result[key]).lower() if isinstance(result[key], bool) else result[key]
                output.write(f"{key}={value}\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
