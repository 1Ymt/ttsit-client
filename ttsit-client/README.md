# ttsit-client

A JavaFX desktop GUI for [ttsit](https://github.com/1Ymt/ttsit), the local
Kokoro-82M text-to-speech server. It starts and stops the server for you,
splits the text you paste in into sentences, and streams each one back as
audio while the next is still being synthesized.

## Usage

Paste or type text into the transcript box, pick a voice, language and speed,
then hit **Read Aloud**. Use **Start Server** / **Stop Server** to control the
local `ttsit` process; its state is shown by the status lamp.

| | |
|---|---|
| ![Server closed](docs/ui/state-closed.png) | ![Voice picker](docs/ui/voices-popup.png) |

## Setup

Requires JDK 21 (a Maven wrapper is included, no separate Maven install
needed) and a working [ttsit](https://github.com/1Ymt/ttsit) checkout, since
this app launches it as a subprocess.

```
git clone --recurse-submodules https://github.com/1Ymt/ttsit-client.git
```

(or, if already cloned: `git submodule update --init`)

Then set up the `ttsit` submodule's Python environment as described in
[its README](https://github.com/1Ymt/ttsit#setup).

## Running

```
mvnw.cmd javafx:run      # Windows
./mvnw javafx:run        # Linux/macOS
```

## License

MIT — see [LICENSE](LICENSE).
