# Fork development

- `main` is the combined installable fork. `upstream-main` tracks dts88's
  unmodified `upstream/main` and is updated by fast-forward only. `personal-build`
  is a historical snapshot; new combined work belongs on `main`. `ferix-main`
  similarly tracks unmodified `ferix/main`; import selected Ferix changes through
  feature/integration branches and preserve attribution.
- Start independent upstream contributions from `upstream/main` or
  `upstream-main`. Keep unrelated features, personal version numbers and updater
  configuration out of their PR branches. Preserve existing PR heads unless the
  task explicitly calls for updating those PRs.
- Whenever adding or changing a feature, update `README.md` in the same change.
  Explain its behavior, settings, relevant defaults and known limitations; keep
  the fork additions list accurate. Mark research as research, not functionality.
- Credit imported contributions and preserve their authorship where possible.
  Update `NOTICE.md`, the changelog and test notes when applicable. Do not claim
  vehicle testing or compatibility beyond the available evidence.
- For every dts88/Ferix update review, update `docs/source-integration-ledger.md`,
  including reviews with no imports. Record exact reviewed heads, adopted/partial/
  deferred/skipped/retired decisions, reasons, fork modifications, local commits
  and validation limits. Keep changed decisions visible. Source-mirror updates
  are not app integrations; mark release inclusion only after checking its tag.
- Follow `FORK_WORKFLOW.md` for integration, builds and releases. Validate the
  combined result before releasing and retain the existing signing identity for
  update compatibility.

- This combined fork is **Zeekr Companion**. Use canonical `vX.Y.Z-alpha.N` / `vX.Y.Z-beta.N` / `vX.Y.Z` tags and `ZeekrCompanion-<versionName>.apk` downloads via `.github/scripts/package_release.py`. New alpha/beta releases use GitHub Pre-release. New public files use `ZeekrCompanion`; preserve legacy `ZeekrShortcutApp` and EVCam compatibility. Keep Android application ID/signing identity and historical download URLs; keep this branding out of independent upstream PRs.
