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
- Follow `FORK_WORKFLOW.md` for integration, builds and releases. Validate the
  combined result before releasing and retain the existing signing identity for
  update compatibility.
