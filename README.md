# CobraOSS

CobraOSS is a defensive, unrooted Android diagnostics application. It provides a Material 3 dashboard, app-private diagnostic environment, USB enumeration, BLE advertisement inspection, bounded TCP checks, and JSON/CSV export.

## Build

Open this repository in Android Studio or run:

```bash
./gradlew assembleDebug
```

Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The project intentionally does not include exploit runners, password cracking, raw Wi-Fi capture, HID injection, MAC spoofing, or an unauthenticated remote shell. Android applications cannot reliably provide those capabilities without privileged access, and shipping them as turnkey features would be unsafe.

## Requirements

- Android Studio Ladybug or newer
- Android SDK 35
- JDK 17
- Android device with USB debugging enabled for hardware testing
