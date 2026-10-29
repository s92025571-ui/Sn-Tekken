# SN TEKKEN — Animal Warriors (3D Android Fighting Game)

**SN TEKKEN** is a 100% free, completely offline single-player 3D arcade fighting game built natively for Android using **Kotlin**, **OpenGL ES 2.0**, and **Jetpack Compose**.

## Core Features
- **Four Playable Bipedal Animal Warriors**:
  1. **Iron Buffalo** — Heavyweight Water Buffalo brawler with curved horns, dark-orange shockwaves, *Buffalo Apocalypse* Ultimate, and **Seismic Floor Shatter** stomps.
  2. **Titan Rhino** — Armored Rhinoceros tank with super-armor poise, compressed rock blasts, *Rhino Earthbreaker* Ultimate, and **Pillar Shrapnel Bash**.
  3. **Shadow Tiger** — Fast agile Bengal Tiger martial artist with rapid claw combos, azure claw crescents, *Phantom Tiger Fury* Ultimate, and **Smoke Veil Ambush** cloaking.
  4. **Inferno Dragon** — Bipedal Fire Dragon warrior with folded/flaring wings, flame breath, fireballs, *Dragon Inferno* Ultimate, and **Scorched Fissure Ignition**.
- **Interactive Beast Battle Arena**:
  - **Destructible Cyber-Stone Pillars**: Heavy strikes, charges, and knockbacks crack and shatter the 4 arena pillars, hurling shrapnel at nearby foes and venting dense smoke clouds.
  - **Shattered Floor Zones**: Heavy stomps fracture the octagonal platform, slowing grounded opponents and increasing stagger vulnerability.
  - **Tactical Dust & Smoke Clouds**: Obscure opponent AI vision and grant Shadow Tiger stealth ambush damage bonuses.
  - **Scorched Flame Fissures**: Draconic fire attacks ignite cracked floor tiles and broken pillar cores.
- **3-Stage Arcade Campaign & 2-Phase Boss**:
  - Battle 1 & Battle 2 against rival Animal Warriors, culminating in Battle 3 against the **Shadow Dragon Overlord** (300 HP, telegraphed lethal attacks, and Phase 2 fury below 50% HP).
- **100% Free & Offline**:
  - Zero internet permissions, zero remote CDNs, zero ads, zero microtransactions, and local device progress saving.

## Building the Android APK
- **In Google AI Studio**: Open the **Settings / Export** menu to generate an APK/AAB directly or push to GitHub.
- **Local / CLI Build**:
  ```bash
  gradle :app:assembleDebug
  ```
  The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.
- **GitHub Actions**: Pushing to `main` triggers `.github/workflows/android-apk.yml`, running unit tests and uploading the compiled APK artifact.
