<p align="center">
  <img src="assets/whalekey-mark.svg" alt="WhaleKey icon" width="96" height="96">
</p>

<h1 align="center">WhaleKey</h1>

<p align="center">An offline Android Pinyin keyboard with an on-device writing agent.</p>

<p align="center">
  <a href="https://github.com/shenmuegit/whalekey/issues">Roadmap and progress</a>
</p>

> **Status: early development.** A debug APK builds from source and has been tested on an Android 15 emulator. There is no release APK yet.

## What WhaleKey will do

- Type Chinese with full Pinyin.
- Work without an internet connection or uploading typed text.
- Rewrite selected text in the current input field when the user asks. The result is previewed before it replaces the original text.

## Implementation direction

The keyboard will build on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Text rewriting will run on the device. The release app will not request internet access and will disable Android cloud backup for app data.

Development tasks and their acceptance criteria are tracked in [GitHub Issues](https://github.com/shenmuegit/whalekey/issues).

## Build from source

The debug build was verified with JDK 21, Android SDK Platform 36, Build Tools 36.1.0, NDK 28.0.13004108, and CMake 3.31.6.

```sh
git clone --recurse-submodules https://github.com/shenmuegit/whalekey.git
cd whalekey
./gradlew :app:assembleDebug -PbuildABI=x86_64
```

The APK is written to `app/build/outputs/apk/debug/`. The `x86_64` build is intended for an Android emulator; choose a supported ABI for your device.

## License and upstream

WhaleKey is based on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Its source and pinned dependencies retain their upstream copyright notices and licenses. See [LICENSE](LICENSE) and the license headers in each component.
