# Genuwin AI Waifu

Genuwin is an Android application that provides an interactive AI companion using Live2D animation. It integrates with Ollama for AI chat, and OpenAI-compatible APIs for Text-to-Speech (TTS) and Speech-to-Text (STT). *Your data. Your rules. Genuinely.*

## Key Features

- **Live2D Animation**: Interactive 2D character animation.
- **AI Chat**: Conversational AI powered by Ollama.
- **Voice Interaction**: Full audio pipeline with STT and TTS.
- **Smart Home Integration**: Control Home Assistant devices.
- **Advanced Tool System**: Weather, search, and more.
- **Augmented Reality**: AR mode for immersive experiences.

## Requirements

- Android Studio or Gradle
- Java 17
- [Live2D Cubism SDK for Java](https://github.com/Live2D/CubismJavaFramework)
- An Ollama server and OpenAI-compatible TTS/STT servers (see credits), OR an OpenAI API key
- Home Assistant (Optional)

## Quickstart

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/shifusen329/Genuwin.git
    cd Genuwin
    ```
2.  **Setup Live2D SDK**:
    - Download the [Live2D Cubism SDK for Java](https://www.live2d.com/en/sdk/download/cubism/).
    - Copy the `CubismJavaSamples` directory to the project root.
    - Ensure the `Framework` and `Core` directories are properly linked.
3.  **Configure API Endpoints**:
    - Copy `src/main/assets/config.properties.template` to `src/main/assets/config.properties`.
    - Edit `src/main/assets/config.properties` with your server URLs or your OpenAI API key.
4.  **Build and Install**:
    - **Android Studio**: Open the project and build the APK.
    - **Gradle**: Run the following command in the project root:
      ```bash
      ./gradlew assembleDebug
      ```
    - Install the generated APK located at `app/build/outputs/apk/debug/app-debug.apk` on your device.

## License

This project is built upon the following open-source software:

- **Ollama**: Licensed under the MIT License. See [https://github.com/ollama/ollama](https://github.com/ollama/ollama).
- **openedai-speech**: Licensed under the AGPL-3.0 License. See [https://github.com/matatonic/openedai-speech](https://github.com/matatonic/openedai-speech).
- **openedai-whisper**: Licensed under the AGPL-3.0 License. See [https://github.com/matatonic/openedai-whisper](https://github.com/matatonic/openedai-whisper).

This project also uses the Live2D Cubism SDK, which has its own licensing terms. Please review the Live2D license before commercial use.
