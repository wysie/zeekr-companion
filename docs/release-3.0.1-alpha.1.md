# Zeekr Companion 3.0.1-alpha.1

Prepared November 2, 2026, at the owner's request. Alpha publication; no beta or
stable promotion. Android versionCode 351, application ID
`io.github.dts88.zeekrshortcut` retained.

## Included scope

- `6d64fea1`: bounded foreground recovery after a latched camera-access denial
  while recording remains wanted and no live camera is recording.
- `4dc93b21`: retain an acquired, fresh GPU mirror picture across same-source
  framing/resize edits; preserve source-age checks and reset on source changes.
- `5a59c95c`: five illustrated, localized mirror gesture help pages.
- `ed8523e3`: illustrated, localized installer Back/Open reminder; confirmation
  and cancellation behavior retained.

The USB cleanup/write/sync investigation is documentation only. No buffer, sync,
quality, destination or retention-default change, and no USB stall resolution is
claimed. No new upstream runtime import. App Lab disappearance remains unresolved.

## Validation

Fresh release preparation: all 1,575 JVM tests passed, zero failures/errors/skips;
five release packaging/naming tests passed; signed release assembly and debug
lint passed (zero errors/fatals, 299 warnings). Canonical package staging rejects
a tag/version mismatch.

The same runtime changes passed the November 2 native validation described in
[the camera/mirror report](009-diagnostics-20261102.md): 20 distinct Android
checks, targeted final reruns and the installer/help check in all three languages
and both themes. The final installer run passed its consent/cancel behavior.
The release version-only bump did not rerun that native suite. No vehicle test
or physical USB/HUD performance certification.

Local staged APK: `ZeekrCompanion-3.0.1-alpha.1.apk`, 23,370,742 bytes.
Local SHA-256: `5e489e2b2d35554f19cf43d5e359a0a44455313824a9a5475de6e943d4714e4e`.
APK v2 signature verified; certificate SHA-256 remains
`a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`.
The local and CI APKs have different byte hashes; each was independently checked.

## Publication verified

Published November 2 at 16:20 Singapore time as
[v3.0.1-alpha.1](https://github.com/wysie/zeekr-companion/releases/tag/v3.0.1-alpha.1),
Pre-release on, draft off, Latest off. Tag target
`c90699916d04fb2de07cda99fea2a25bca09c061` contains all four corrections above;
ancestry checked. [Tag build and draft publication](https://github.com/wysie/zeekr-companion/actions/runs/41411979476)
passed.

The CI-generated APK and checksum were downloaded and compared before publication.
APK versionCode 351 / versionName 3.0.1-alpha.1, package ID and existing signing
certificate were independently verified. Unauthenticated public APK and checksum
downloads were also compared and match:
`24e5369e27565bf72e868d859ff01179a881446fe768af893b38ba9821d1c053`.
Public APK size is 23,370,742 bytes. Website release labels and links are updated
only after the release became public.
