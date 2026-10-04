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
- xpForLevel(n) = 100 + 50*(n-1). Glow: 0.6 untrained, 1 grace day, drops to 0.3 in 7 days, completing a mission restores 1.0.
- NO punishment: XP and level are never lost. Undoing a mission never removes rewards already earned.

## New direction (approved)
- Avatar: pixel art 32x32, layers drawn on Canvas with nearest-neighbor (FilterQuality.None), breathing = 1px bob in code.
  Customizable: gender (different basic outfit), skin tone (5), hairstyle (6), hair color (gray sprite tinted with BlendMode.Modulate).
- Sprite layers in res/drawable-nodpi: body_{male|female}_{1..5}, eyes_*, outfit_*_basic, hair_{1..6}_front, hair_{n}_back (optional).
  Draw order: hair back, body, eyes, outfit, hair front, (later: hat, glasses, cloak).
- Rewards: one item per total level (fixed table, deterministic), next item of the attribute for each weekly mission (once per week),
  one special fixed item for "reward" missions (once). Items belong to an attribute; rare ones require a level in that attribute.
  Items live in a Kotlin catalog (not a DB table). Inventory + equipped-by-slot are Room tables.
- Room v2 migration needed: avatar_profile, inventory (unique itemId), equipped (slot -> item), MissionEntity.rewardItemId.
- NO companion/pet for now (removed on purpose). Old DragonView / CreatureScreen / CreatureRules are being deleted.

## Phases
5A sprites + AvatarView + test screen; 5B Room v2; 5C avatar creator; 5D rewards, inventory, equip;
4B missions screen redesign (Habitica-style references); 6 AI camera verification (CameraX + Retrofit, provider TBD);
7 AI suggestions + motivational messages; 8 achievements + "Reflejo" timeline; 9 polish (palette, dark background, icon, fonts, usability tests).

## Design (to revisit in phase 9)
Dark: bg 161E2F, cards 242F49, borders 384358, primary B51A2B, accent FFA586. Light "Ocean Royale": bg F4FBFE, cards CAF0F8, primary 023EBA, accent FBB02D.
I dislike the current dark background; palette gets reviewed at the end.
