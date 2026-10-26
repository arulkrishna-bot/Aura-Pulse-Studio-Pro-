# Aura Pulse Studio — Android APK

தமிழில் படிப்படியான வழிமுறைகளுக்கு [`README_தமிழ்.txt`](README_தமிழ்.txt) பார்க்கவும்.

GitHub Actions workflow: `.github/workflows/build-apk.yml`

1. இந்த folder-க்குள் உள்ள எல்லா கோப்புகளையும் (குறிப்பாக `.github` folder-ஐ) repository root-க்கு upload செய்யவும்.
2. `Actions` → `Build Aura Pulse APK` → `Run workflow` தேர்வு செய்யவும்.
3. முடிந்த run-ஐத் திறந்து `Artifacts` → `AuraPulseStudio-APK` பதிவிறக்கவும்.
4. Artifact ZIP-ஐ extract செய்து `app-debug.apk` நிறுவவும்.

Player HTML: `app/src/main/assets/index.html`

> This workflow creates a debug APK. It is not a production-signed Play Store release.
