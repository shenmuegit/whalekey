<p align="center">
  <img src="assets/whalekey-mark.svg" alt="WhaleKey icon" width="96" height="96">
</p>

<h1 align="center">WhaleKey</h1>

<p align="center">An offline Android Pinyin keyboard with an on-device writing agent.</p>

<p align="center">
  <a href="https://github.com/shenmuegit/whalekey/issues">Roadmap and progress</a>
</p>

> **Status: planning.** There is no installable build yet.

## What WhaleKey will do

- Type Chinese with full Pinyin.
- Work without an internet connection or uploading typed text.
- Rewrite selected text in the current input field when the user asks. The result is previewed before it replaces the original text.

## Implementation direction

The keyboard will build on [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android). Text rewriting will run on the device. The release app will not request internet access and will disable Android cloud backup for app data.

Development tasks and their acceptance criteria are tracked in [GitHub Issues](https://github.com/shenmuegit/whalekey/issues).
