App 
- tracks periods, charts explaining, 1 slide for global/local chats for women around the same phase of their cycle
- stored in the DB but encrypted with a local key
- calendar based
- Tips and details on how to have a better time with the cramps/pains
- What many literature say about how periods should last
- Premium features : Analysis, Best fertility window, year of the time to get pregnant based on Astrology or sports or academic inclinations
- What does their cycle say about them( some bullshit eastern philosophy shit)
- Long term prediction of when the periods may start, early notification signs
- Notifications during and/before, based on the time of the month
- Premium : Suggesting intensity at the gym/run/ workouts based on the time of month, and how to effectively have a routine built around their moon.
- Friendly, soft language, with inclusive and smooth feeling UI and adaptive experience based on usage.

---

## Getting the APK

From the repo root (needs the Android SDK and JDK 19):

    .\gradlew.bat assembleDebug

The APK is written to `app\build\outputs\apk\debug\app-debug.apk` (~28 MB). It is signed with the
debug key, so it installs straight onto a device — with one plugged in:

    adb install -r app\build\outputs\apk\debug\app-debug.apk

Or just copy that `.apk` to the phone and open it (allow "install unknown apps"). Build output is
gitignored, so rebuild any time rather than expecting a committed binary. Tests live in the same
place: `.\gradlew.bat :app:testDebugUnitTest` (130) and `.\gradlew.bat :app:connectedDebugAndroidTest`
(4, needs a running emulator or device).

