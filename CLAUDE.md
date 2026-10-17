# Vidalvl

Native Android app (Kotlin + Jetpack Compose), gamified productivity, RPG style, pixel-art avatar.
Course: Diseño de Interfaces de Software — UX/UI quality is graded as much as functionality.

## Working rules
- User is a beginner in Android Studio and speaks Spanish. Reply in Spanish, concise, with opinions.
- Code in English (variables, classes, files, comments). UI text in Spanish, switchable to English (use string resources).
- Work in phases. Finish one phase, tell me exactly how to test it, and WAIT for my approval before the next.
- Show a plan before touching many files. One problem at a time when debugging.
- Do not rewrite long files if a small edit is enough. Never delete files without listing them first.
- Tests go in src/test, never src/main.

## Stack
Package com.vidalvl.app. AGP 9.4.1 (built-in Kotlin), Kotlin 2.2.10, compileSdk/targetSdk 37, minSdk 26,
Compose BOM 2026.02.01, Room 2.8.5 (KSP 2.3.10), navigation-compose 2.9.0, lifecycle-viewmodel-compose 2.9.0.
Version catalog (libs.versions.toml). No Hilt: AndroidViewModel + manual wiring. MVVM.
Tested on a Xiaomi phone (USB install enabled) and Pixel 8 emulator (API 37).

## Game rules (keep)
- Attributes: BODY, MIND, DISCIPLINE, SOCIAL. Missions: DAILY, WEEKLY (weeks Monday-Sunday).
- xpForLevel(n) = 100 + 20*(n-1). Mission XP: daily 8, weekly 15. Glow: 0.6 untrained, 1 grace day, drops to 0.3 in 7 days, completing a mission restores 1.0.
- NO punishment: XP and level are never lost. Undoing a mission never removes rewards already earned.

## New direction (approved)
- Avatar: pixel art 32x32, layers drawn on Canvas with nearest-neighbor (FilterQuality.None), breathing = 1px bob in code.
  Customizable: gender (different basic outfit), skin tone (5), eyes (10), hairstyle (6), hair color (gray sprite tinted with BlendMode.Modulate).
- Sprite layers in res/drawable-nodpi: body_{male|female}_{1..5}, eyes_{male|female}_{01..10}, outfit_*_basic,
  hair_{1..6}_front, hair_{n}_back (optional), hair_{n}_hat_front / hair_{n}_hat_back (trimmed hair under hats; back falls back to the normal one).
  Items: hat_NNN[_v], glasses_NNN_v, cloak_NNN + cloak_NNN_back, outfit_{male|female}_NNN (paired by number; 020 and 021 are male only).
  Draw order: cloak back, hair back, body, eyes, outfit, hair front, hat, glasses, cloak.
  Items have a HairEffect (NONE / TRIMMED / HIDDEN). Catalog: 62 items (20 by total level 2..21, 4 attributes x 9 weekly, 6 special for reward missions, none wired to a mission yet).
- Rewards: one item per total level (fixed table, deterministic), next item of the attribute for each weekly mission (once per week),
  one special fixed item for "reward" missions (once). Items belong to an attribute; rare ones require a level in that attribute.
  Items live in a Kotlin catalog (not a DB table). Inventory + equipped-by-slot are Room tables.
- Room v2 migration needed: avatar_profile, inventory (unique itemId), equipped (slot -> item), MissionEntity.rewardItemId.
- NO companion/pet for now (removed on purpose). Old DragonView / CreatureScreen / CreatureRules are being deleted.

## Phases
5A sprites + AvatarView + test screen; 5B Room v2; 5C avatar creator; 5D rewards, inventory, equip;
4B missions screen redesign (Habitica-style references); 6 AI camera verification (CameraX + Retrofit, provider TBD);
7 AI suggestions + motivational messages; 8 achievements + "Reflejo" timeline; 9 polish (palette, dark background, icon, fonts, usability tests).

## Design (decided in phase 9)
Dark "dark fantasy": bg 0E0D12, cards 1A1822, borders 3A3548, primary B51A2B (crimson), accent F0A04B (ember). Never use the primary as text on dark: use `VidalvlTheme.attributeColors.accentText`.
Light "Pergamino": bg F6EFE0, cards EADFC8, primary 9E1B2B, accent C98A1B.
Titles in Cinzel (bundled, OFL, third_party/fonts); body in the system font. Dark theme draws a pixel-art night sky (NightSky) behind transparent screens; cards and buttons stay opaque.
Light theme draws a pixel-art day sky (DaySky: blue gradient fading into the parchment, pixel sun, 4 drifting clouds); both skies stay still when system animations are off (`animationsEnabled()` in Motion.kt).
Main cards use `PixelCornerShape` (staircase corners) plus `Modifier.pixelShadow` (hard offset shadow); completing a mission fires a `PixelBurst`. Buttons, small badges and creator thumbnails keep rounded corners.
ContrastTest enforces 4.5:1 for text in both modes: change a color, run the tests.

## Progress
Phases 5A-9C are done and approved; each approved phase is committed right away (English message, never a Co-Authored-By trailer: the user does not want Claude listed as a contributor).
Pending: the app icon (the user makes it). cloak_001 stays out of the catalog on purpose (its front sprite was never made). Usability test kit: docs/pruebas-usabilidad-vidalvl.docx.
