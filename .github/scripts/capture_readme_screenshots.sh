#!/usr/bin/env bash
# Capture real app UI with fictional camera pixels on a disposable emulator.
set -euo pipefail
cd "$(dirname "$0")/../.."

serial="${1:?Usage: bash .github/scripts/capture_readme_screenshots.sh emulator-PORT}"
case "$serial" in
  emulator-*) ;;
  *) echo "A disposable emulator serial is required." >&2; exit 2 ;;
esac
command -v adb >/dev/null
command -v cwebp >/dev/null
command -v rg >/dev/null
scratch="$(mktemp -d "${TMPDIR:-/tmp}/companion-readme.XXXXXX")"
trap 'rm -rf "$scratch"' EXIT

bash ./gradlew assembleDebug assembleDebugAndroidTest
adb -s "$serial" get-state
adb -s "$serial" shell wm size 2560x1600
adb -s "$serial" shell wm density 320
adb -s "$serial" shell settings put system font_scale 1.0
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
remote=/sdcard/Android/data/io.github.dts88.zeekrshortcut/files
adb -s "$serial" shell mkdir -p "$remote"
adb -s "$serial" push docs/screenshots/source/camera-atlas.png "$remote/readme-camera-atlas.png"
adb -s "$serial" shell am instrument -w -r -e captureReadme true \
  -e class 'com.kooo.evcam.settings.ReadmeScreenshotsAndroidTest#captureReadmeScreenshots' \
  io.github.dts88.zeekrshortcut.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$scratch/instrumentation.log"
# adb itself can exit zero even when instrumentation reports failure.
rg -q '^OK \(1 test\)' "$scratch/instrumentation.log"
adb -s "$serial" pull "$remote/readme-screenshots" "$scratch/captures"
for screen in main menu preview-layout recording storage super-mirror blind-camera hud 360-camera floating-button interface system gpu-processing; do
  for theme in light dark; do
    name="$screen-$theme"
    test -s "$scratch/captures/$name.png"
    cwebp -quiet -lossless -z 6 "$scratch/captures/$name.png" -o "docs/screenshots/$name.webp"
  done
done
echo "Captured 26 screenshots in docs/screenshots/."
