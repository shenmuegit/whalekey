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

- Type Chinese with the Rime Ice full-Pinyin scheme.
- Work without an internet connection or uploading typed text.
- Rewrite selected text in the current input field when the user asks. The result is previewed before it replaces the original text.

## Implementation direction

The keyboard builds on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android) and uses [Rime](https://github.com/rime/librime) with [Rime Ice](https://github.com/iDvel/rime-ice) as its only Chinese input method. Text rewriting runs on the device. The app does not request internet access and disables Android cloud backup for app data.

Next-word suggestions after committing a phrase are currently unavailable in the bundled Rime Ice scheme.

Development tasks and their acceptance criteria are tracked in [GitHub Issues](https://github.com/shenmuegit/whalekey/issues).

## Local rewrite model

The debug APK bundles [Qwen2.5-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF) (Q4_K_M, 491 MB) and runs it with [llama.cpp](https://github.com/ggml-org/llama.cpp). The first rewrite copies the model into app-private storage; it does not download a model. The rewrite operation does not save the text it receives. The keyboard action and preview are tracked in [Issue #5](https://github.com/shenmuegit/whalekey/issues/5).

When an editor cannot report the selection's position, WhaleKey temporarily reads the current field to verify it. Only the selected text goes to the on-device model; field contents are not logged, saved, or uploaded.

## Clipboard history

Clipboard history recording starts off on a fresh install. You can enable it in Settings → Clipboard or from the keyboard's Clipboard panel. Turning recording off stops new entries but does not delete entries already saved; use **Clear clipboard database** to remove them. Saved history and clipboard suggestions are hidden while the lock screen is showing. Manual data exports include saved clipboard history. Verification is recorded in [Issue #9](https://github.com/shenmuegit/whalekey/issues/9).

## Build from source

Building requires Git LFS, JDK 21, Android SDK Platform 36, Build Tools 36.1.0, NDK 28.0.13004108, and CMake 3.31.6.

```sh
git clone --recurse-submodules https://github.com/shenmuegit/whalekey.git
cd whalekey
git lfs pull
./gradlew :app:assembleDebug -PbuildABI=x86_64
./gradlew :app:assembleDebug -PbuildABI=arm64-v8a
```

The APK is written to `app/build/outputs/apk/debug/`. Use `x86_64` for an Android emulator or `arm64-v8a` for an ARM64 device.

## License and upstream

WhaleKey is based on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Its source and pinned dependencies retain their upstream copyright notices and licenses. The bundled Qwen model is Apache-2.0 licensed; llama.cpp is MIT licensed. See [LICENSE](LICENSE), the [bundled notices](app/src/main/assets/licenses), and the license headers in each component.

The bundled Rime runtime statically links [librime-octagram at `dfcc151`](https://github.com/lotem/librime-octagram/tree/dfcc15115788c828d9dd7b4bff68067d3ce2ffb8), licensed GPL-3.0-only at that revision. Its [full license text](app/src/main/assets/licenses/librime-octagram-GPL-3.0.txt) is included in the APK.

Rime Ice is pinned at [`da1fbe602e38f26db846fa10120ee64c2b0324c0`](https://github.com/iDvel/rime-ice/tree/da1fbe602e38f26db846fa10120ee64c2b0324c0). The APK includes the complete default `8105`, `base`, `ext`, `tencent` and `others` Chinese tables and the full-Pinyin scheme dependencies, preserving entries such as `𤭢 cei` and `𤭢了 cei le`. The source assets retain their upstream notices; Rime Ice is GPL-3.0-only licensed. Its [source notice](app/src/main/assets/licenses/rime-ice-SOURCE.txt) and [license](app/src/main/assets/licenses/rime-ice-GPL-3.0.txt) are bundled in the APK. On first use, Rime compiles these bundled dictionaries locally.
