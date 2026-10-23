# Galaxy Watch Tweaker

LSPosed/Xposed module for running Samsung Galaxy Watch features on non-Samsung phones, with a small Android UI for controlling each hook domain independently.

The Android package is `dev.ricky.galaxywatchtweaker`, so it can install side by side with the older `dev.ricky.galaxywatchselfheal` builds while the product moves to the Galaxy Watch Tweaker brand.

## Features

- Toggle Samsung Health Monitor patching for `com.samsung.android.shealthmonitor`.
- Toggle Watch companion identity spoofing for Watch Plugin and Google Wear companion paths.
- Toggle Watch capability exchange spoofing for Watch7 Plugin feature data.
- Toggle Android CompanionDeviceManager connection recovery in `system_server`.
- Override the watch display name shown by Galaxy Wearable without changing the fixed product name.
- Edit the SHM phone profile from the app UI while keeping the companion identity stable.
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

## Reconnection and identity stability (0.1.4)

Connection recovery requests Bluetooth connections only for active Watch7 Plugin associations whose devices are still bonded. It never reports a synthetic Bluetooth-connected event. Android owns connection-state notifications, including failures and disconnects. Requests are throttled using elapsed time and delayed work runs on the main handler; BLE disappearance cancels pending BLE recovery.

Capability refresh runs after a connected state, with attempts at 15 and 75 seconds. A disconnect cancels both attempts, and a later reconnect starts a new session. Sessions are isolated by companion instance and watch address, so stale callbacks cannot refresh a later session. Unknown watch/state APIs are skipped rather than guessing a connection or address.

Only outbound capability messages are rewritten, and only identity fields already present in those messages. Received watch messages, pairing identifiers, and unrelated capability fields are preserved. The exact `SM-S938U1` to `SM-S938U` compatibility mapping remains unchanged. Ambiguous obfuscated methods or unsupported identity field layouts are rejected.

At the first app launch after this update, the existing configured profile is saved separately for companion identity and capability exchange. Later profile edits and Restore preset affect SHM only; the Profile page displays the saved companion model and CSC. All hooks in a process read one configuration snapshot. An unreadable or empty configuration skips hooks instead of substituting a default identity.

**Upgrade:** install the signed release over the existing installation, open Galaxy Watch Tweaker once to save/sync the current identity, then reboot the phone. Keep the companion identity and capability switches in their existing states for an already paired watch. Do not clear module, plugin, or Wear app data as part of this update. The module cannot discover the identity recorded by an earlier pairing or restore data already erased.

The regression suite covers connection request preconditions/cooldowns, disconnect/reconnect sessions, stale callbacks, independent watches, saved identity migration, atomic configuration reads, reflection contracts, and outbound message preservation. No watch/phone is attached to the release development environment: the reported watch wipe has not been reproduced, and these fixes do not establish its root cause.

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
