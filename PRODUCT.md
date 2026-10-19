# Zeekr Companion

## Confirmed context
An Android camera and vehicle utility maintained by wysie, forked from dts88's Zeekr Shortcut, with credited Ferix CAMCORDER and EVCam work. The owner uses a Zeekr 009; 7X support requires vehicle validation where noted. It runs on the head unit alongside factory apps and Android Auto, in daylight and at night.

## Platform and stack
Native Android Views, Java and Material components; Gradle/Android SDK. The application ID, signing identity and public ZeekrShortcutApp storage folder must remain compatible with existing installs. The GitHub repository has not been renamed.

## Main-screen task
See live cameras, start or stop recording, take a photo, open media, select preview sources/layout, and control independent Super mirror/360° windows. Preview choices are separate from recording sources and quality. Camera mapping and calibrated 360° projection remain separate settings. Existing choices must survive the redesign.

## Approved direction
The owner approved the camera-first concept and the refinements discussed on 2026-10-19: a large preview, header Preview selector and direct 360° access, bottom action dock, visible Companion logo, quick presets before other sources, and contextual Straighten preview. Preserve the existing driver-side preference. Future factory-menu screenshots may guide later styling; exact factory parity is not established.

## Constraints and evidence
Never present a mock camera scene as real vehicle output. Preserve camera lifecycle, recording authority, factory priority and freshness guards. Source changes still require quitting/reopening; an unapplied choice must remain visible. Native emulator tests establish UI behavior, not HUD fitting, on-car startup reliability or frame rate.
