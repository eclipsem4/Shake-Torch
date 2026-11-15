# ShakeLight

Native Kotlin & Jetpack Compose utility that toggles your Android device's flashlight on or off with a physical double-chop or shake gesture—even while locked, in the background, or after a reboot.

## Build & Run in Android Studio
1. Open the project root in **Android Studio Ladybug or newer** (supports AGP 9.1.1 & JDK 17).
2. Let Gradle sync resolve dependencies (Hilt, Jetpack Compose, DataStore, Navigation Compose).
3. Connect a physical Android 12+ (`API 31+`) device with a rear LED camera flash (accelerometers & hardware torch cannot be fully exercised on a virtual emulator without sensor injection).
4. Click **Run (`app`)** to build and install the debug APK.

## Feature Testing Checklist
1. **Onboarding & Core Permissions**: Launch fresh -> swipe through the 3 glass onboarding cards -> tap **Grant Core Permissions** -> verify Camera, Notifications, and Phone State prompts appear.
2. **Start / Stop Shake Detection**: On Home, tap the central Liquid Glass orb -> verify status pill switches to **Listening for shake** and the persistent notification appears with a **Stop** action -> tap **Stop** in the notification to verify the service stops cleanly.
3. **Shake Intensity & Live Meter**: Open **Settings** -> adjust **Shake Intensity Threshold** (1–10) -> return to Home and shake the phone to watch the **Live Shake Meter** animate past the amber threshold marker.
4. **Cooldown Guard**: Set **Toggle Cooldown** to `3.0 s` in Settings -> perform two double-chops 1 second apart -> confirm the second gesture is ignored until 3 seconds elapse.
5. **Haptic Feedback**: Enable **Vibrate on Toggle** -> shake to turn ON (1 crisp pulse) and shake again to turn OFF (2 short pulses).
6. **Pause During Calls**: Enable **Pause During Calls** with Phone State granted -> place/receive a call -> verify status switches to **Paused: Call in Progress** and resumes automatically when the call ends.
7. **Screen-Off & Boot Persistence**: Enable **Start on Device Boot** -> arm ShakeLight -> lock the screen and shake to toggle the torch -> reboot the phone and verify the service restarts automatically.
8. **Battery Saver & Exemption**: Open **System** tab -> tap **Allow Unrestricted** to exempt ShakeLight from Doze -> enable **Battery Saver Mode** in Settings to verify low-power sampling and low-battery cutoff behavior.
