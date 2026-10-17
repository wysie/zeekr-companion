# Optional side-camera auto-fit

Blind spot camera → Image & framing → **Auto-fit to avoid black patches** is on
by default, including upgrades where no preference exists. It applies only while
Straighten picture is on; turning Straighten off disables the switch in the UI
without losing its saved choice. Reset side-camera settings restores Auto-fit on.

On retains the existing fitted projection: requested width, aim, rotation and
window shape determine a smaller actual width when the camera image edge would
be crossed. Off keeps the requested width and the same centre/aim/rotation. It
does not clamp individual UV samples onto source edges. Missing coverage is black.
The width control still has its existing 60–130 degree range.

The preference is shared by the screen popup, HUD (which uses the popup picture
renderer), and Super mirror's temporary side view. Window shapes/positions and
HUD mirroring remain independent. Normal Super mirror rear view, DIY 360, photos
and recordings are unchanged. On a projected HUD, black means no projected light.
Factory/reverse priority and live-frame guards are unchanged.

The unfitted Canvas mesh clips each tile to the selected source lane after its
perspective transform. An opaque black destination underlay fills unsupported
areas; a small inset avoids filtering against adjacent composite channels. Both
partial and fully unsupported tiles are handled without reintroducing the old
edge-clamping collapse. Correction remains a bounded mesh approximation.

Validation (disposable API 32 emulator; no vehicle validation claim):

- 1,440 JVM tests pass, including requested-width preservation for both lanes,
  portrait/square/extra-wide shapes and ±45° roll; toggling fitting recalculates
  coverage without moving the view centre, and fitted defaults retain their bounds.
- Native Canvas fixtures encode the selected lane as a colour gradient and every
  other lane magenta. Unfitted rotated square/tall/extra-wide views retain their
  projected picture, show opaque black missing areas and never sample magenta.
- Native preference navigation verifies default-on, persistence, immediate
  Straighten dependency and re-enabling without losing the saved fit choice.
  Existing popup resize/reset and HUD layout regressions also pass: 12 native
  tests in total.
- Android lint and signed release/debug builds pass. No version bump or new
  downloadable release is part of this change.
