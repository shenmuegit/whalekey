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

The keyboard builds on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Text rewriting runs on the device. The app does not request internet access and disables Android cloud backup for app data.

Development tasks and their acceptance criteria are tracked in [GitHub Issues](https://github.com/shenmuegit/whalekey/issues).

## Local rewrite model

The debug APK bundles [Qwen2.5-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF) (Q4_K_M, 491 MB) and runs it with [llama.cpp](https://github.com/ggml-org/llama.cpp). The first rewrite copies the model into app-private storage; it does not download a model. The rewrite operation does not save the text it receives. The keyboard action and preview are tracked in [Issue #5](https://github.com/shenmuegit/whalekey/issues/5).

## Build from source

Building requires Git LFS, JDK 21, Android SDK Platform 36, Build Tools 36.1.0, NDK 28.0.13004108, and CMake 3.31.6.

```sh
git clone --recurse-submodules https://github.com/shenmuegit/whalekey.git
cd whalekey
git lfs pull
./gradlew :app:assembleDebug -PbuildABI=x86_64
```

The APK is written to `app/build/outputs/apk/debug/`. The `x86_64` build is intended for an Android emulator; choose a supported ABI for your device.

## License and upstream

WhaleKey is based on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Its source and pinned dependencies retain their upstream copyright notices and licenses. The bundled Qwen model is Apache-2.0 licensed; llama.cpp is MIT licensed. See [LICENSE](LICENSE), the [bundled notices](app/src/main/assets/licenses), and the license headers in each component.
