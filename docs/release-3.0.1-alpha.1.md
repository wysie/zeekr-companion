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
Public CI asset checksum must be checked separately after publication; a local
build checksum does not establish the contents of a remote asset.
