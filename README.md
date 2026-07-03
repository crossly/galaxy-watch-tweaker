# Galaxy Watch Tweaker

LSPosed/Xposed module for running Samsung Galaxy Watch features on non-Samsung phones, with a small Android UI for controlling each hook domain independently.

The Android package is `dev.ricky.galaxywatchtweaker`, so it can install side by side with the older `dev.ricky.galaxywatchselfheal` builds while the product moves to the Galaxy Watch Tweaker brand.

## Features

- Toggle Samsung Health Monitor patching for `com.samsung.android.shealthmonitor`.
- Toggle Watch companion identity spoofing for Watch Plugin and Google Wear companion paths.
- Toggle Watch capability exchange spoofing for Watch7 Plugin feature data.
- Toggle Android CompanionDeviceManager connection recovery in `system_server`.
- Override the watch display name shown by Galaxy Wearable without changing the fixed product name.
- Edit the spoofed Samsung phone profile from the app UI.
- Restore the built-in Galaxy S25 Ultra US preset.
- View detected package versions for phone SHM, Watch SHM, Watch Manager, Watch7 Plugin, and Google Wear.

## Default Spoof Profile

The built-in preset represents a US Galaxy S25 Ultra:

```text
Manufacturer: samsung
Brand: samsung
Model: SM-S938U1
Watch-facing model: SM-S938U
Sales code / CSC: XAA
Country ISO: US
Operator: 310260
Operator name: T-Mobile
```

Watch-facing compatibility paths intentionally map `SM-S938U1` to `SM-S938U`, because some Samsung companion payloads expect the carrier model rather than the unlocked model suffix.

## Version Matrix

The UI lists the package versions currently visible on the phone:

- Phone SHM: `com.samsung.android.shealthmonitor`
- Watch SHM: shown as a dedicated compatibility row; the phone cannot query the watch APK directly without a watch-side channel, so unavailable values display `Watch-side only`
- Watch Manager: `com.samsung.android.app.watchmanager`
- Watch7 Plugin: `com.samsung.wearable.watch7plugin`
- Google Wear: `com.google.android.wearable.app.cn`

Verified baseline:

```text
Phone SHM 1.4.6.003
Watch SHM 1.4.6.003
```

## Hook Boundaries

The module keeps risky domains separate:

- Samsung Health Monitor hooks run only inside `com.samsung.android.shealthmonitor`.
- Watch companion identity and capability hooks run in Samsung/Google companion packages.
- Connection recovery hooks run only in the Android framework process.

This split is intentional: disabling one feature in the app leaves the other domains untouched.

## LSPosed Scope

Enable the module for:

```text
android
system
com.google.android.wearable.app.cn
com.samsung.android.app.watchmanager
com.samsung.wearable.watch7plugin
com.samsung.android.shealthmonitor
```

Install APK updates with `adb install -r` or the Android package installer. Do not edit LSPosed databases directly.

## Build

Requires:

- JDK 17
- Android SDK platform 36

Run unit tests:

```sh
./gradlew :app:testDebugUnitTest
```

Build debug APK:

```sh
./gradlew :app:assembleDebug
```

Build release APK:

```sh
./gradlew :app:assembleRelease
```

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Some Honor builds require an explicit installer identity for adb installs:

```sh
adb shell pm install -r -i com.android.packageinstaller /data/local/tmp/galaxy-watch-tweaker-debug.apk
```

## GitHub Release Signing

The release workflow signs APKs online with `apksigner`.

Configure these repository secrets to use a stable release signing key:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

The release workflow requires these secrets. Missing signing secrets fail the release job rather than producing an APK with an unstable temporary key.

Create a release by pushing a semantic version tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```
