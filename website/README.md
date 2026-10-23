# Zeekr Companion website

Public showcase: **https://wysie.github.io/zeekr-companion/**

The website belongs to the app repository. Native HTML, CSS and JavaScript
provide one 13-screen preview with a native screen selector, a theme switch,
full-size image dialog, brief feature summary, installation steps and credits.
There are no accounts, analytics, forms or external runtime dependencies.

## Source and assets

- Edit `index.html`, `styles.css`, `app.js` and `theme-init.js` here.
- The approved logo comes from `docs/branding/companion-icon.svg`.
- Full-size screenshots come from `docs/screenshots/`; no duplicate originals
  or generated camera atlas are committed under the website.
- The build generates 800px and 1600px WebP variants for responsive loading and
  copies the originals for the full-size viewer.
- Geist is self-hosted in `assets/fonts/` with its OFL license.
  Font source: https://github.com/vercel/geist-font

Screenshots are actual Android UI with fictional, generated camera scenes.
They do not demonstrate car-camera quality, frame rate, HUD optics or 360°
alignment. [Capture provenance](../docs/screenshots/README.md).

The visible release number and its GitHub release URL in `index.html` must be
updated together when a new app release is published. Do not point visitors to
an unpublished tag. No app version or package change is required for website edits.

## Build and preview

Requires Python 3, Node.js for syntax checks, and `cwebp`
(`brew install webp` on macOS or `apt-get install webp` on Ubuntu).

From the app repository root:

```sh
node --check website/app.js
node --check website/theme-init.js
python3 .github/scripts/build_website.py
python3 -m http.server 4173 --directory _site --bind 127.0.0.1
```

Before publishing, check desktop and mobile layouts in both themes, screen
selection, Enlarge / Actual size, Escape-to-close and keyboard focus.

The build validates local assets, image alt attributes, anchors and unique IDs.
It accepts only a new or empty output directory, preventing accidental deletion
or inclusion of unrelated files. Use `--output /absolute/new-directory` for an
additional build; `_site/` is generated and ignored by Git.

Asset paths are relative, so the site works under GitHub Pages' repository
subpath. Canonical and Open Graph URLs point to the public Pages address.

## Deployment

`.github/workflows/pages.yml` builds website changes and screenshot refreshes.
Pull requests validate the build without deployment. Merges to `main` deploy
the generated `_site/` artifact through the `github-pages` environment; the
workflow also supports a manual run. Only the deploy job can write Pages and
request an OIDC token.

GitHub Pages uses **GitHub Actions** as its publishing source. This keeps the
website in `main` alongside the app and does not need a generated `gh-pages`
branch. The artifact contains only explicit website files, fonts, logo and
screenshot assets; it excludes repository source, local logs and owner exports.
