# Translation completion — 21 October 2026

The supported interface languages are English (`en`), Bahasa Melayu (`ms`) and
Simplified Chinese (`zh-CN`). Default text resources are consistently Simplified
Chinese; English and Malay have explicit translations, including all 20 About
resources. Redundant Chinese overrides were removed, including seven Traditional
Chinese overrides of the upscaling settings.

Malay now uses consistent Cermin super, paparan sekeliling, tetingkap timbul,
unit utama, processing/fallback and reset terminology. Proper names, technical
abbreviations, version numbers, file paths and community credits are retained.
The Bilinear choice says “default” rather than incorrectly claiming to be the
current choice when another sampling mode is selected.

The mirror guide and installer reminder no longer embed English/Chinese-only
text in bitmaps. They use translated, scrollable native text with theme colours,
font scaling, selectable body text and accessible headings. Existing paging,
first-use tracking, outside dismissal and explicit download confirmation remain.
The installer instructions use current Companion branding. About storage/privacy
copy describes selected storage, private relay buffers, local sharing and manual
update checks instead of the obsolete USB-only description.

The update checker selects authored release-note blocks using the application
locale. It does not call a translation service. Release generation requires Malay
and Simplified Chinese entries alongside the English changelog. Recent versions
56–60 have authored translations; earlier notes use an explicit localized
English-only notice. Missing/empty translations retain readable original notes;
English clients never receive concatenated languages. The existing version-count
and length limits remain. English release bodies, tags, APKs and integrity checks
are not replaced by translated summaries.

Technical diagnostic exports and external system/vendor messages are not interface
translations. Named credits retain their original names. The phone-sharing web
page remains intentionally bilingual English/Chinese.

The five existing GitHub releases 56–60 now include both authored translations.
Their original English bodies, prerelease flags, tags and APK/checksum assets were
verified after the description-only updates. No new APK release was published.

Validation:

- All resource XML files, not just `strings.xml`, checked for language coverage,
  duplicate keys, plural/array consistency and format argument types/indexes.
- Debug, instrumentation and signed release variants built successfully.
- 1,556 JVM tests passed; Android lint: zero errors and zero translation warnings.
- Five Python packaging/release-note tests passed, including required translations
  and matching bullet counts for Unreleased and versions 56–60.
- Native Android test passed across all three languages and both themes at 1.3×
  system text: About attribution, all five help pages, installer text, visible
  action and cancellation/confirmation callbacks, at both 2560×1600 and 1920×1080.
  Screenshots inspected in Malay
  light/dark and Chinese dark. Local emulator evidence; no new vehicle test claim.
