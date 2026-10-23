#!/usr/bin/env python3
"""
Build the GitHub Release body for a given version.

Takes the matching section out of CHANGELOG.md so each release shows its own
changes instead of the same boilerplate every time, then appends the standing
safety / attribution text.

A stable version (no suffix) is introduced by its changelog section as a whole: that
section carries its own headings, written as ### in CHANGELOG.md and promoted to ## here.
Test builds (-alpha, -beta) list the section under "What changed".

Usage:  python3 .github/scripts/release_notes.py 0.3.0-alpha > RELEASE_NOTES.md
"""
import io
import os
import re
import sys

APP_NAME = "Zeekr Companion"
REPO = "https://github.com/wysie/zeekr-companion"


def changelog_section(version, path="CHANGELOG.md"):
    """Return the body of the '## [<version>]' section, or None."""
    if not os.path.exists(path):
        return None
    with io.open(path, encoding="utf-8") as source:
        text = source.read()
    lines = text.split("\n")

    start = None
    for i, line in enumerate(lines):
        if line.startswith("## ") and ("[" + version + "]") in line:
            start = i + 1
            break
    if start is None:
        return None

    end = len(lines)
    for i in range(start, len(lines)):
        if lines[i].startswith("## "):
            end = i
            break

    body = "\n".join(lines[start:end]).strip()
    return body if body else None


def promote_headings(section):
    """### in CHANGELOG.md (so the section parser keeps them) become ## in the release."""
    return "\n".join("## " + line[4:] if line.startswith("### ") else line
                     for line in section.split("\n"))


def translated_sections(version):
    """Authored change notes only; no online translation or translated safety claims."""
    blocks = []
    for language, label, heading in [
            ("ms", "Bahasa Melayu", "Perubahan"),
            ("zh-CN", "简体中文", "更新内容")]:
        section = changelog_section(version, "CHANGELOG." + language + ".md")
        if not section:
            raise ValueError("Missing " + language + " changelog for " + version)
        blocks.append("<details>\n<summary>" + label + "</summary>\n\n"
                      "<!-- companion-notes:" + language + " -->\n"
                      "## " + APP_NAME + " " + version + "\n\n## " + heading + "\n\n"
                      + promote_headings(section) + "\n<!-- /companion-notes -->\n</details>")
    return "<!-- companion-translations -->\n" + "\n\n".join(blocks)


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else "unknown"
    stable = "-" not in version

    out = []
    out.append("## " + APP_NAME + " " + version)
    out.append("")

    section = changelog_section(version)
    if stable:
        if section:
            out.append(promote_headings(section))
            out.append("")
    else:
        out.append("Download the `.apk` below and sideload it through App Lab.")
        out.append("")
        if section:
            out.append("## What changed")
            out.append("")
            out.append(section)
            out.append("")


    out.append("## Getting started")
    out.append("")
    out.append("Plug in a USB drive.")
    out.append("")
    out.append("Something wrong? Export a report from **Settings \u2192 System \u2192 Diagnostics** "
               "and attach it to an issue.")
    out.append("")

    out.append("## Safety")
    out.append("")
    out.append("Experimental, unofficial software. Not affiliated with, approved by, or endorsed "
               "by ZEEKR, and not certified for any vehicle safety function.")
    out.append("")
    out.append("- Try anything new in a stationary vehicle first.")
    out.append("- It does not replace the factory dash cam, reversing camera or blind-spot monitor.")
    out.append("- Do not operate it while driving, and do not judge distances or obstacles from "
               "its picture.")
    if not stable:
        out.append("- It competes with the head unit for resources and **may** affect factory "
                   "features; stop using it and uninstall if anything behaves oddly.")
    out.append("- Recordings on the USB drive are not encrypted and can contain faces and plate "
               "numbers. Follow your local law.")
    out.append("")

    out.append("## Credits")
    out.append("")
    out.append("Maintained by **wysie**, primarily for personal use. Based on "
               "[Zeekr Shortcut by dts88](https://github.com/dts88/zeekr-shortcut-car), "
               "with credited features from [CAMCORDER by FerixzTanz](https://github.com/FerixzTanz/CAMCORDER).")
    out.append("")
    out.append("For the original, more thoroughly tested version, use "
               "[dts88’s releases](https://github.com/dts88/zeekr-shortcut-car/releases). "
               "Report Zeekr Companion issues in [this repository](" + REPO + "/issues).")
    out.append("")
    out.append("Released under **GPL-3.0**, based on "
               "[EVCam](https://github.com/suyunkai/EVCam) by suyunkai.")
    out.append("")
    out.append(translated_sections(version))
    sys.stdout.write("\n".join(out) + "\n")


if __name__ == "__main__":
    main()
