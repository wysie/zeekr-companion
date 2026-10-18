# Upstream contributions

This index tracks outgoing PRs. Incoming source reviews, imports, adaptations and
exclusions are tracked in the [source integration ledger](source-integration-ledger.md).

Submitted on 2026-10-16 against dts88 main at `0f145f7a` (2.12.0-alpha).

Each branch starts directly from that upstream commit and builds independently.
None requires our combined fork, personal version numbers or updater configuration.
The existing label and blind spot PR heads were left unchanged.

| Batch item | Upstream PR | Branch |
| --- | --- | --- |
| 1 | [Remove unused microphone permission checks](https://github.com/dts88/zeekr-shortcut-car/pull/28) | `contrib/01-unused-microphone` |
| 2 | [Keep settings controls visible in narrow layouts](https://github.com/dts88/zeekr-shortcut-car/pull/29) | `contrib/02-settings-layout` |
| 3 | [Keep playback action labels aligned and readable](https://github.com/dts88/zeekr-shortcut-car/pull/30) | `contrib/03-playback-actions` |
| 4 | [Group existing settings by task and add a stamps subpage](https://github.com/dts88/zeekr-shortcut-car/pull/31) | `contrib/04-settings-organisation` |
| 5 | [Expose developer mode as a warned System toggle](https://github.com/dts88/zeekr-shortcut-car/pull/32) | `contrib/05-developer-toggle` |
| 6 | [Preserve footage across relay and archive transfer failures](https://github.com/dts88/zeekr-shortcut-car/pull/33) | `contrib/06-footage-transfer-integrity` |
| 7 | [Retain existing footage locks when list replacement fails](https://github.com/dts88/zeekr-shortcut-car/pull/34) | `contrib/07-protection-list-integrity` |
| 8 | [Preserve verified originals before bounded MP4 repair](https://github.com/dts88/zeekr-shortcut-car/pull/35) | `contrib/08-repair-original-backup` |
| 9 | [Cover stalled main-camera and Super mirror previews](https://github.com/dts88/zeekr-shortcut-car/pull/36) | `contrib/09-stalled-preview-covers` |
| 10 | [Yield automatic UI recovery to reverse and factory cameras](https://github.com/dts88/zeekr-shortcut-car/pull/37) | `contrib/10-factory-camera-recovery` |
| 11 | [Verify updater APK hashes and restrict redirects to HTTPS](https://github.com/dts88/zeekr-shortcut-car/pull/38) | `contrib/11-update-integrity` |

The full batch also merged cleanly on the local `review/upstream-pr-batch-20261016`
validation branch. Its debug/release builds, 1,113 JVM tests and eight native Android
regressions passed. Individual branches were tested before submission. Native tests
cover layout, navigation, developer-mode state and opaque stalled-preview covers;
they do not establish vehicle compatibility.

Full debug lint has existing upstream errors: the unchanged baseline reports 845;
the combined batch reports 844, with no new errors. Release builds and release lint
pass. The transfer and repair branches include an identical verified-copy helper,
so either can merge first.

PR #31 reorganizes only existing upstream settings. It does not include the fork’s
blind spot or 360 sections. The source-tracking branches remain unmodified; update
them by fast-forward and integrate selected changes separately into the fork.
