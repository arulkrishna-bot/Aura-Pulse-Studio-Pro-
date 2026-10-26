Aura Pulse Studio — GitHub மூலம் APK உருவாக்குவது
====================================================

இந்த project-ல் Android app source code மற்றும் player HTML உள்ளது.
GitHub Actions மூலம் Android APK build செய்ய workflow சேர்க்கப்பட்டுள்ளது.
APK-ஐ இந்த chat-இல் நேரடியாக build/test செய்து உறுதிப்படுத்தவில்லை; GitHub Actions build result-ஐப் பார்க்கவும்.

முக்கிய கோப்பு
-------------
- Player UI: app/src/main/assets/index.html
- Android code: app/src/main/java/com/aurapulse/studio/
- APK build workflow: .github/workflows/build-apk.yml

GitHub-ல் upload செய்வது (மொபைலிலிருந்தும் செய்யலாம்)
----------------------------------------------------
1. இந்த ZIP-ஐ உங்கள் தொலைபேசியில் Extract செய்யவும்.
2. github.com-ல் sign in செய்து New repository உருவாக்கவும்.
   உதாரண repository பெயர்: AuraPulseStudio
3. Extract செய்த `AuraPulseStudio` folder-ஐ திறக்கவும்.
4. அந்த folder-க்குள் உள்ள எல்லா files மற்றும் folders-ஐ upload செய்யவும்.
   முக்கியம்: `.github` folder உட்பட அனைத்தும் repository-யின் root-ல் இருக்க வேண்டும்.
   `AuraPulseStudio` என்ற outer folder-ஐ அப்படியே உள்ளே upload செய்தால் workflow root-ல் இயங்காது.
5. GitHub repository-யில் `Actions` tab-ஐத் திறக்கவும்.
6. `Build Aura Pulse APK` workflow-ஐத் தேர்ந்தெடுத்து `Run workflow` அழுத்தவும்.
   கோப்புகளை push/upload செய்த பிறகும் workflow தானாகத் தொடங்கும்.
7. Build முடியும் வரை காத்திருந்து அந்த run-ஐத் திறக்கவும்.
8. கீழே உள்ள `Artifacts` பகுதியில் `AuraPulseStudio-APK` என்பதைப் பதிவிறக்கவும்.
9. ZIP artifact-ஐ extract செய்தால் `app-debug.apk` கிடைக்கும். அதை Android phone-ல் நிறுவவும்.
   Android கேட்கும்போது அந்த source-க்கு “Install unknown apps” அனுமதி தேவைப்படலாம்.

HTML player-ஐ மாற்றுவது
----------------------
`app/src/main/assets/index.html` கோப்பை மாற்றி repository-க்கு commit/upload செய்தால்
workflow புதிய APK-ஐ உருவாக்கும்.

குறிப்பு
-------
- இந்த workflow debug APK உருவாக்கும்; இது Play Store வெளியீட்டுக்கான production signing அல்ல.
- APK install செய்யும்போது Android version/device compatibility மாறலாம்.
- GitHub Actions build பிழை வந்தால் `Actions` run-ல் உள்ள build log-ஐப் பகிரவும்.
