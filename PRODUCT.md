# Zeekr Companion

## Confirmed context
An Android camera and vehicle utility maintained by wysie, forked from dts88's Zeekr Shortcut, with credited Ferix CAMCORDER and EVCam work. The owner uses a Zeekr 009; 7X support requires vehicle validation where noted. It runs on the head unit alongside factory apps and Android Auto, in daylight and at night.

## Platform and stack
Native Android Views, Java and Material components; Gradle/Android SDK. Keep the application ID, signing identity and preferences compatible with existing installs. New public files use ZeekrCompanion; retain access to older ZeekrShortcutApp and EVCam folders. The GitHub repository has not been renamed.

## Main-screen task
See live cameras, start or stop recording, take a photo, open media, select preview sources/layout, and control independent Super mirror/360° windows. Preview choices are separate from recording sources and quality. Camera mapping and calibrated 360° projection remain separate settings. Existing choices must survive the redesign.

## Approved direction
The owner approved the camera-first concept and the refinements discussed on 2026-10-19: a large preview, header Preview selector and direct 360° access, bottom action dock, visible Companion logo, quick presets before other sources, and contextual Straighten preview. Preserve the existing driver-side preference. The owner supplied actual 009 settings photos and approved the factory-style main/Picture concepts on 2026-10-19: regular-weight settings type, flat near-white rows, neutral sidebar selection, orange-underlined tabs, compact inline main actions and a contextual live camera preview. Match this language in native Views; exact factory pixel parity is not established.

## Constraints and evidence
Never present a mock camera scene as real vehicle output. Preserve camera lifecycle, recording authority, factory priority and freshness guards. Source changes requiring different preview feeds still need quitting/reopening; the applied choice and saved next choice must remain distinct. Equivalent feed sets need no preview restart. Native emulator tests establish UI behavior, not HUD fitting, on-car startup reliability or frame rate.
