# 360° render performance — 2026-10-19

The owner still reports lag. An older retained state in the 18 October drive dump
reported 42 ms readback, 93 ms worker render and a 226 ms presentation gap at
Balanced (768×3084 input, 640×960 output). It was retained after leaving the view;
it is one sample, not a sustained live benchmark or evidence for release 50.

## Changes

- Store lookup entries in two packed long arrays instead of five int arrays:
  16 rather than 20 bytes per output pixel, excluding array headers. That is 20%
  less lookup storage (about 9.38 rather than 11.72 MiB at Balanced).
- Outside the narrow seams, return the single camera sample directly instead of
  performing a blend with itself.
- Sample red/blue in separate lanes of a long and green in unsigned int arithmetic.
  Preserve the previous bilinear weights and rounding exactly. A direct pixel
  sample still validates opacity and never reads an unused neighbour.
- Add `workerQueueMs`, `publishMs` and `processingMs` to existing Diagnostics.
  Processing includes capture start through output-bitmap publication; it does
  not measure the GPU compositor's physical screen presentation.

Calibration, lane ownership, source/output dimensions, quality caps, pressure
fallbacks, one-frame-in-flight behavior, freshness checks, factory handover,
photo rendering and recording behavior are unchanged. Both main and popup use
this renderer. No GPU pipeline or new camera output is introduced.

## Validation

1. Compare all 65,536 sub-pixel fractions for six high-contrast/random colour
   sets against independent per-channel interpolation: 393,216 exact comparisons.
2. On Android API 32, ARM64 `sdk_gphone64_arm64` / `ranchu`, compare every output
   pixel against the frozen previous lookup builder, sampler and renderer for
   Manual and 009 image-fit projections at Low, Balanced and Full. All match.
3. Warm both paths eight times, alternate their timing order and report the median
   of eleven runs. [Raw synthetic results](360-render-benchmark-2026-10-19.txt):

| Projection / quality | Previous render | New render | Compute reduction |
| --- | ---: | ---: | ---: |
| Manual / Balanced | 5.32 ms | 4.33 ms | 18.7% |
| 009 fit / Balanced | 5.20 ms | 4.23 ms | 18.7% |
| 009 fit / Full | 12.14 ms | 9.76 ms | 19.7% |

These timings exclude camera readback, Bitmap extraction/publication, UI scheduling
and real head-unit contention. The emulator is not a forecast of 009 frame rate.
The tests do not impose a flaky wall-clock performance threshold.

## Still to measure on the car

The CPU stitcher still snapshots the preview with UI-thread TextureView.getBitmap.
That copy, background scheduling and output upload can dominate the result.
The quality caps remain roughly 15/10/5 fps for Low/Balanced/Full. This patch cannot
promise factory-like smoothness, and it does not validate 7X performance.

Run the main 360° view and the popup, including while recording, then retain a
Diagnostics/drive-log sample from each. Compare readback, render, queue, publish,
processing and presentation-gap measurements. Check source loss/factory takeover
as well as ordinary live motion. A GPU path that avoids repeated CPU readback is
still the next architectural step if copy/upload dominates; this patch is a
pixel-preserving CPU improvement, not a claim that the owner-reported lag is solved.
