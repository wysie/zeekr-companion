#!/usr/bin/env python3
"""Build the public showcase from website source and existing README screenshots."""
import argparse
from html.parser import HTMLParser
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[2]
SCREENS = (
    "main", "menu", "preview-layout", "recording", "storage", "super-mirror",
    "blind-camera", "hud", "360-camera", "floating-button", "interface", "system",
    "gpu-processing",
)


class PageReferences(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.references = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            if attrs["id"] in self.ids:
                raise ValueError("Duplicate HTML id: " + attrs["id"])
            self.ids.add(attrs["id"])
        if tag == "img" and "alt" not in attrs:
            raise ValueError("Image is missing its alt attribute")
        for attr in ("src", "href"):
            if attr in attrs:
                self.references.append(attrs[attr])
        self.references.extend(item.strip().split()[0]
                               for item in attrs.get("srcset", "").split(",") if item.strip())


def validate(output):
    page = PageReferences()
    html = (output / "index.html").read_text()
    page.feed(html)
    for ref in page.references:
        url = urlsplit(ref)
        if url.scheme or url.netloc:
            continue
        if url.path and (url.path.startswith("/") or not (output / url.path).is_file()):
            raise ValueError("Missing or root-relative site asset: " + ref)
        if not url.path and url.fragment and url.fragment not in page.ids:
            raise ValueError("Missing anchor target: " + ref)
    css = (output / "styles.css").read_text()
    for ref in re.findall(r'url\(["\']?([^"\')]+)', css):
        if not (output / ref).is_file():
            raise ValueError("Missing CSS asset: " + ref)
    if "chatgpt.site" in html:
        raise ValueError("Website metadata still points to the previous host")


def build(output):
    source = ROOT / "website"
    output = output.resolve()
    # Never recursively delete an existing directory or publish an unrelated tree.
    if output.exists() and any(output.iterdir()):
        raise ValueError("Output must be empty; choose a new directory: " + str(output))
    if output == ROOT or output in source.parents or source == output or source in output.parents:
        raise ValueError("Output must be separate from website source")
    encoder = shutil.which("cwebp")
    if not encoder:
        raise ValueError("Install cwebp first (brew install webp / apt-get install webp)")
    output.mkdir(parents=True, exist_ok=True)
    for filename in ("index.html", "styles.css", "app.js", "theme-init.js"):
        shutil.copy2(source / filename, output / filename)
    shutil.copytree(source / "assets" / "fonts", output / "assets" / "fonts")
    shutil.copy2(ROOT / "docs/branding/companion-icon.svg", output / "assets/companion-icon.svg")
    images = output / "assets/screenshots"
    images.mkdir(parents=True)
    for screen in SCREENS:
        for theme in ("light", "dark"):
            name = screen + "-" + theme
            original = ROOT / "docs/screenshots" / (name + ".webp")
            shutil.copy2(original, images / original.name)
            for width in (800, 1600):
                subprocess.run([
                    encoder, "-quiet", "-resize", str(width), "0", "-q", "90",
                    str(original), "-o", str(images / (name + "-" + str(width) + ".webp")),
                ], check=True)
    (output / ".nojekyll").touch()
    validate(output)
    print("Built and validated 13 screens, both themes and 78 image variants: " + str(output))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "_site",
                        help="New or empty output directory (default: _site)")
    args = parser.parse_args()
    try:
        build(args.output)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, str(error) + "\n")
