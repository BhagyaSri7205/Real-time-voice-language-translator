# VoxTranslate — Android App

A real, installable Android app rebuilt from your VoxTranslate desktop project.
It reimplements every feature using Android's native capabilities so it works
as an actual phone app — not a mockup.

## What changed from the original project, and why

Your original project was a **Python desktop app** (Tkinter/`customtkinter` UI,
`PyAudio` for the mic, `pytesseract`/OpenCV for OCR, `gTTS`/`playsound` for
speech, PyInstaller `.spec` for packaging). None of that stack runs on
Android — Tkinter has no Android backend, and PyInstaller only builds
Windows/Mac/Linux executables. So this is a full rewrite in **Kotlin +
Jetpack Compose**, the standard toolkit for native Android apps, using
Android-native equivalents for every original feature:

| Feature | Original (desktop) | This app (Android) |
|---|---|---|
| Speech-to-text | `SpeechRecognition` (Google Web Speech API) | Android's built-in `SpeechRecognizer` |
| Translation | free web-scraping backends (`deep-translator`) | Google ML Kit **on-device** Translate — no API key, works offline after first download |
| Text-to-speech | `gTTS` + `playsound` (needed internet every time) | Android's built-in `TextToSpeech` engine (works offline once a voice is installed) |
| OCR | OpenCV + Tesseract | ML Kit **on-device** Text Recognition + CameraX |
| History / accounts | SQLite + `bcrypt` | Room database (on-device history; login/accounts were dropped — see below) |

**Note on accounts:** the original app had a login/signup system (`auth.py`,
`bcrypt`). A phone app is inherently single-user (it's tied to one person's
device), so this rebuild skips login entirely and everything just works
immediately — settings and history are stored locally on the phone. Say the
word if you actually want optional cloud sync or multi-user accounts added
back (that would need a backend server).

## Features included, all fully working

- 🎤 Speak into the mic → live speech-to-text
- 🌍 Translate between 20 languages (English, Hindi, Telugu, Tamil, Kannada,
  Malayalam, Bengali, Marathi, Gujarati, Punjabi, Urdu, Korean, Japanese,
  Chinese, French, German, Spanish, Russian, Arabic, Portuguese, Italian)
- 🔊 Listen to translations spoken aloud
- 👥 Conversation Mode — two mic buttons, one per person, each in their own
  language, auto-translates and speaks each turn
- ⌨️ Type-to-translate
- 📷 Point the camera at text and translate it on the spot (OCR)
- 🕘 Translation history, saved on-device, with replay/delete
- ⚙️ Settings — default languages, dark/light theme, speech rate, auto-speak
  toggle, history on/off

## How to build and run it (you'll need Android Studio)

1. **Install Android Studio** (free): https://developer.android.com/studio
2. **Open the project**: `File → Open`, select the `VoxTranslate` folder
   (the one containing `settings.gradle.kts`).
3. **Let Gradle sync** — Android Studio will download dependencies
   automatically (needs internet the first time). If it asks about the
   Gradle wrapper, let it regenerate — that's normal and automatic.
4. **Run it**:
   - Plug in an Android phone via USB with Developer Options + USB
     debugging enabled, **or** create a virtual device (Android Studio →
     Device Manager).
   - Click the green ▶️ Run button.
5. **Install permanently on your phone**: `Build → Generate Signed App
   Bundle / APK → APK`, follow the wizard (you can create a new signing
   key), then copy the resulting `.apk` from `app/release/` to your phone
   and open it to install (you may need to allow "install from unknown
   sources" once).

## First-run notes

- The **first time** you translate between a new pair of languages, ML Kit
  needs to download a small language model (a few MB) — this needs
  internet. After that, translation for that pair works fully offline.
- For **text-to-speech** in a language you haven't used before, Android may
  need to download a voice pack: **Settings → System → Languages & input →
  Text-to-speech output** on the phone.
- Camera and microphone permissions are requested the first time you use
  those features — grant both for full functionality.

## Project structure

```
VoxTranslate/
├── app/src/main/java/com/voxtranslate/app/
│   ├── MainActivity.kt, MainViewModel.kt, VoxTranslateApp.kt
│   ├── translate/        # Languages list + ML Kit translation wrapper
│   ├── speech/           # SpeechRecognizer (STT) + TextToSpeech (TTS)
│   ├── ocr/              # ML Kit text recognition
│   ├── data/db/          # Room database (history)
│   ├── data/settings/    # DataStore preferences (settings)
│   └── ui/               # Compose screens, navigation, theme, components
└── app/src/main/res/     # icons, strings, themes
```
