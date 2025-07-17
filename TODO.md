# AI Waifu Android App

An Android application that creates an interactive AI companion using Live2D animation, integrated with Ollama for AI chat, and OpenAI-compatible APIs for Text-to-Speech (TTS) and Speech-to-Text (STT).

## ✅ Completed Features

- **Live2D Animation**: ✅ Interactive 2D character animation using the Live2D Cubism SDK
  - Haru and Beartz7 models included with full animations and expressions
  - Touch interaction triggers different animations (head pat, body touch)
  - Smooth OpenGL ES rendering with proper lifecycle management
  - Defensive error handling for missing models
  
- **AI Chat Integration**: ✅ Conversational AI powered by Ollama
  - Complete API integration with proper error handling
  - Configurable personality system
  - Conversation history management
  - Retry mechanisms for network issues
  
- **Voice Interaction**: ✅ Full audio pipeline implemented
  - Speech-to-Text for voice input (OpenAI-compatible API)
  - Text-to-Speech for AI responses (OpenAI-compatible API)
  - Audio recording and playback management
  - Permission handling for microphone access
  - Character-specific wake word detection ("Hey Haru" and "Hey Becca")
  - Voice Activity Detection (VAD) for automatic end-of-speech detection
  - Follow-up conversation support without wake word requirement
  - Intelligent audio filtering to prevent false transcriptions
  
- **Modern UI**: ✅ Material Design interface
  - Fullscreen immersive experience
  - Voice chat and text input buttons
  - Real-time status updates
  - Custom styled buttons and layouts
  - Wake lock prevents screen from sleeping during use

- **Enhanced Idle Animations**: ✅ Sophisticated idle behavior system
  - Multiple idle states (Normal, Deep, Attention-seeking)
  - Time-based animation variations
  - Weighted random animation selection
  - Automatic expression cycling
  - Smart cooldown system to prevent repetition
  - User interaction tracking and response

- **Character Switching**: ✅ Dynamic character management
  - Seamless switching between Haru and Beartz7 models
  - Proper GL thread model loading to prevent crashes
  - Smooth transitions without flickering
  - Character-specific animations and behaviors
  - Persistent character state management
  - **Settings Integration**: Default character selection now properly respected from settings UI

- **Smart Home Integration**: ✅ Home Assistant device control
  - Ollama function calling for device control
  - Natural language device commands
  - Support for lights, thermostats, and sensors
  - Multi-device control (e.g., "turn on bedroom lights" controls multiple lights)
  - Proper authentication and error handling

- **Advanced Tool System**: ✅ Comprehensive tool integration with dynamic model compatibility
  - **Configurable Tools**: Tools can be enabled or disabled via a configuration file, allowing for customized deployments.
  - **Dynamic Tool Calling**: Automatically detects model capabilities and routes to native tools or system prompt tools
  - **Weather Tool**: "What's the weather in Houston?" - Real-time weather data via SearXNG with 10 search results
  - **Search Tool**: "Who won the Astros game yesterday?" - Privacy-focused metasearch with comprehensive results
  - **Home Assistant Tools**: "Turn on the lights in the living room" - Smart home device control
  - **Temperature Control**: "Set the thermostat to 72 degrees" - Climate control integration
  - **Wikipedia Tool**: "Who is the president?" - Factual lookups and summaries
  - **Google Calendar Tool**: "Schedule a meeting for tomorrow at 2pm" - Event creation and management
  - **Google Drive Tool**: "Find my presentation slides" - File search in Google Drive
  - **Context-Aware Parameter Parsing**: Intelligent parameter handling prevents tool conflicts
  - **Thread-Safe VAD**: Fixed race conditions in Voice Activity Detection for reliable operation
  - **Model Compatibility Cache**: Optimized tool routing based on model capabilities

- **Settings Interface**: ✅ Comprehensive settings management
  - Categorized settings with intuitive navigation
  - **API Configuration**: Configure Ollama, TTS, STT, external APIs, and Home Assistant
  - **Character Settings**: Manage character selection, personalities, greetings, and wake words
  - **Audio Settings**: Voice activity detection, audio quality, levels, and testing
  - **Visual Settings**: Display options, animations, performance modes
  - **Advanced Settings**: Debug options, data management, backup/restore, system info
  - Settings button accessible from main screen
  - All settings automatically saved with validation
  - Dark theme with green accents matching app aesthetic

- **Augmented Reality**: ✅ AR mode fully implemented and stabilized
  - Live2D character overlay on rear camera view with proper transparency
  - Toggle between normal and AR modes with dedicated AR button
  - **Fixed Surface Lifecycle Issues**: Resolved destructive surface reconfiguration that caused black screens
  - **Thread-Safe OpenGL Operations**: Eliminated illegal OpenGL calls from UI thread
  - **Conditional Z-Order Management**: UI elements remain visible in both normal and AR modes
  - **Optimized Blending**: Proper premultiplied alpha blending for Live2D textures
  - **AR State Indicator**: Visual feedback system showing conversation state (listening/processing/speaking) when UI is hidden
  - Camera permission handling and comprehensive error management
  - Maintains all existing functionality in AR mode (voice chat, character switching, etc.)
  - Smooth transitions between normal and AR modes without context recreation
  - Diagnostic logging for troubleshooting and performance monitoring

- **Memory Management**: ✅ Added functionality to import, export, and merge character memories, allowing for easy backup and transfer of data across devices.

- **Conversational AI Improvements**: ✅ Fixed inappropriate tool usage for simple conversations
  - **Natural Conversation**: AI now responds naturally to jokes, greetings, and casual chat without mentioning tools
  - **Smart Tool Detection**: LLM automatically determines when tools are needed vs. conversational responses
  - **Fixed System Prompt**: Eliminated structured responses about tool decisions for simple requests
  - **Seamless Experience**: "Tell me a joke" now gets a joke, not explanations about function schemas
  - **Touch-to-Interrupt**: Fixed double-tap detection for reliable TTS interruption in all modes

## 🚀 Build Status

- ✅ **BUILD SUCCESSFUL** - Compiles without errors
- ✅ **Java 17 Compatible** - Updated from deprecated Java 11
- ✅ **Live2D Framework** - Successfully integrated
- ✅ **All Dependencies** - Properly configured
- ✅ **APK Generated** - Ready for installation

## ⚠️ Network Requirements

The following features require network connectivity to function:
- AI chat responses (requires Ollama server)
- Voice-to-text transcription (requires STT server)
- Text-to-speech synthesis (requires TTS server)
- Smart home device control (requires Home Assistant)

The Live2D animation and UI work offline.

## Architecture

### API Integration
- **Ollama API** (http://192.168.0.100:11434): AI chat functionality with tool calling
- **TTS API** (http://192.168.0.104:8005): Text-to-Speech conversion
- **STT API** (http://192.168.0.104:8000): Speech-to-Text transcription
- **Home Assistant API** (http://192.168.0.101:8123): Smart home device control

### Key Components

#### API Layer
- `ApiConfig`: Configuration for all API endpoints
- `ApiManager`: Centralized API management with Retrofit
- `OllamaService`, `TTSService`, `STTService`: Retrofit service interfaces
- Model classes for API requests/responses including tool calling support

#### Smart Home Integration
- `HomeAssistantManager`: Handles device control API calls
- `EntityManager`: Maps friendly names to Home Assistant entity IDs
- `ToolCall`: Model for Ollama function calling responses
- Support for lights, climate control, and sensors

#### Managers
- `AudioManager`: Handles audio recording and playback
- `ApiManager`: Manages all API communications
- `ConversationManager`: Orchestrates voice interaction flow with device control

#### Live2D Integration
- `WaifuDefine`: Constants and configuration for Live2D
- Integration with Live2D Cubism SDK for character animation

#### UI
- `MainActivity`: Main activity with OpenGL surface for Live2D rendering
- Voice and text chat buttons
- Status display overlay

## Setup Instructions

### Prerequisites
1. Android Studio with SDK 21+ support
2. Java 17
3. Live2D Cubism SDK for Java
4. Running Ollama server with a tool-capable model (llama3.1, mistral-nemo, etc.)
5. OpenAI-compatible TTS and STT servers
6. Home Assistant instance (optional, for smart home features)

### Installation

1. **Clone the project**:
   ```bash
   git clone https://github.com/shifusen329/Genuwin.git
   cd Genuwin
   ```

2. **Setup Live2D SDK**:
   - Download Live2D Cubism SDK for Java
   - Copy the `CubismJavaSamples` directory to the project root
   - Ensure the `Framework` and `Core` directories are properly linked

3. **Configure API endpoints**:
   - Create `src/main/assets/config.properties` from the `src/main/assets/config.properties.template` file.
   - Fill in your API keys and server URLs in `src/main/assets/config.properties`.

4. **Setup Home Assistant (Optional)**:
   - Create a long-lived access token in Home Assistant
   - Update `src/main/assets/homeassistant-entities.json` with your device mappings
   - Ensure Home Assistant API is accessible from your Android device

5. **Add Live2D Model**:
   - Place your Live2D model files in `src/main/assets/`
   - Update model paths in the configuration

### Building and Sideloading

1. **Open the project in Android Studio**.

2. **Build the debug APK**:
   - Go to `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   - Once the build is complete, you can find the debug APK in `app/build/outputs/apk/debug/`.

3. **Sideload the APK to your device**:
   - Enable USB debugging on your Android device.
   - Connect your device to your computer.
   - Use `adb` to install the APK:
   ```bash
   adb install app-debug.apk
   ```
   Alternatively, you can transfer the `app-debug.apk` file to your Android device and open it from a file manager to install it. You may need to enable "Install from unknown sources" in your device's settings.

### API Server Setup

#### Ollama Server
```bash
# Install and run Ollama with a tool-capable model
ollama pull llama3.1
ollama serve
```

#### TTS Server (OpenAI-compatible)
Ensure your TTS server supports the OpenAI speech API format:
```
POST /v1/audio/speech
Content-Type: application/json

{
  "model": "tts-1",
  "input": "Hello world",
  "voice": "alloy"
}
```

#### STT Server (OpenAI-compatible)
Ensure your STT server supports the OpenAI transcription API format:
```
POST /v1/audio/transcriptions
Content-Type: multipart/form-data

file: <audio_file>
model: whisper-1
```

#### Home Assistant Setup
1. Install Home Assistant
2. Create a long-lived access token
3. Configure your smart devices
4. Update the entity mappings in `homeassistant-entities.json`

## Usage

1. **Launch the app**: The AI waifu will introduce herself
2. **Voice Chat**: Tap "Voice Chat" to start voice conversation
3. **AR Mode**: Tap "AR" to toggle augmented reality mode - your character will appear overlaid on the rear camera view
4. **Device Control**: Say commands like:
   - "Turn on my bedroom lights"
   - "Turn off the living room lights"
   - "Set the thermostat to 72 degrees"
5. **Touch Interaction**: Touch the Live2D model to trigger reactions
6. **Text Chat**: (Coming soon) Text-based conversation

## Configuration

### Smart Home Device Mapping
Edit `src/main/assets/homeassistant-entities.json` to map friendly names to Home Assistant entity IDs:

```json
{
  "bedroom_lights": [
    "light.wiz_rgbw_tunable_b6f8d5",
    "light.wiz_rgbw_tunable_862676"
  ],
  "livingroom_lights": [
    "light.living_room_1",
    "light.living_room_2"
  ]
}
```

### Personality Customization
Edit `WaifuDefine.WAIFU_PERSONALITY` to customize the AI's personality:

```java
public static final String WAIFU_PERSONALITY = 
    "You are a friendly and caring AI companion named Yuki. " +
    "You are cheerful, supportive, and always ready to chat...";
```

### API Configuration
Update `ApiConfig.java` for different server configurations:

```java
public static final String OLLAMA_BASE_URL = "http://192.168.0.100:11434";
public static final String TTS_BASE_URL = "http://192.168.0.104:8005";
public static final String STT_BASE_URL = "http://192.168.0.104:8000";
```

## Dependencies

- **Live2D Cubism SDK**: Character animation
- **Retrofit2**: HTTP client for API calls
- **OkHttp3**: HTTP client implementation
- **Gson**: JSON serialization/deserialization
- **AndroidX**: Modern Android components

## Permissions

The app requires the following permissions:
- `INTERNET`: API communication
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `RECORD_AUDIO`: Voice input
- `MODIFY_AUDIO_SETTINGS`: Audio configuration
- `CAMERA`: AR mode functionality (rear camera access)
- `WRITE_EXTERNAL_STORAGE`: Temporary file storage
- `READ_EXTERNAL_STORAGE`: File access
- `WAKE_LOCK`: Prevent screen from sleeping during use

## Troubleshooting

### Common Issues

1. **API Connection Failed**:
   - Verify server IP addresses in `ApiConfig.java`
   - Ensure servers are running and accessible
   - Check network connectivity

2. **Home Assistant 401 Unauthorized**:
   - Verify your access token in `config.properties`
   - Ensure the token has proper permissions
   - Check Home Assistant logs for authentication errors

3. **Device Control Not Working**:
   - Verify entity IDs in `homeassistant-entities.json`
   - Test device control manually via Home Assistant UI
   - Check that Ollama model supports function calling

4. **Audio Issues**:
   - Grant microphone permissions
   - Check audio hardware functionality
   - Verify TTS/STT server responses

5. **Live2D Model Not Loading**:
   - Ensure model files are in the correct assets directory
   - Verify model file paths and names
   - Check Live2D SDK integration

### Logs
Enable debug logging by setting `DEBUG_LOG_ENABLE = true` in `WaifuDefine.java`

## TODO List

### High Priority (Core Functionality)
- [x] **Complete Live2D Integration**
  - [x] Create Live2D renderer class (GLRenderer)
  - [x] Implement LAppDelegate for Live2D lifecycle management
  - [x] Create LAppLive2DManager for model management
  - [x] Implement LAppModel wrapper for waifu character
  - [x] Add LAppView for rendering and touch handling
  - [x] Copy Live2D model assets to `src/main/assets/`
  - [x] Configure model loading and initialization
  - [X] Fixed text clipping on UI buttons

- [x] **Implement Volume-Based Lip Sync**
  - [x] Create LipSyncManager for real-time lip sync
  - [x] Add audio volume analysis for TTS audio
  - [x] Map volume levels to Live2D mouth parameters
  - [x] Synchronize lip movements with audio playback
  - [x] Integrate lip-sync processing into model update loop
  - [x] Configure model lip-sync parameters in .model3.json files

- [x] **Smart Home Integration**
  - [x] Implement Ollama function calling for device control
  - [x] Create Home Assistant API integration
  - [x] Add device entity mapping system
  - [x] Support multiple devices per friendly name
  - [x] Implement proper authentication handling
  - [x] Add error handling and user feedback

- [ ] **Complete Audio System**
  - [ ] Fix audio recording format compatibility with STT API
  - [ ] Implement proper audio file conversion (WAV/MP3)
  - [ ] Add audio level monitoring for voice activity detection
  - [ ] Implement noise cancellation and audio filtering
  - [ ] Add audio playback queue management

### Medium Priority (User Experience)
- [ ] **Text Chat Implementation**
  - [ ] Create text input dialog
  - [ ] Add chat history display
  - [ ] Implement typing indicators
  - [ ] Add message bubbles UI

- [ ] **Enhanced Interactions**
  - [X] Implement touch gesture recognition on Live2D model
  - [ ] Add different touch responses (head pat, poke, etc.)
  - [X] Create emotion system based on conversation context *(UNTESTED)*
  - [X] Add idle animations and behaviors
  - [ ] Implement eye tracking (following touch/cursor)

- [ ] **Animation System**
  - [ ] Create animation state machine
  - [ ] Add talking animations synchronized with TTS
  - [ ] Implement listening pose during STT
  - [ ] Add reaction animations for different emotions
  - [ ] Create transition animations between states

### Low Priority (Polish & Features)
- [ ] **Settings and Customization**
  - [X] Add settings screen
  ~~- [ ] Implement voice selection for TTS~~ //managed via backend
  - [X] Add personality customization options
  - [ ] Create theme/appearance settings
  - [ ] Add volume controls
  - [ ] Add configurable follow-up listening mode

- [ ] **Data Persistence**
  - [ ] Implement conversation history storage
  - [ ] Add user preferences persistence
  - [x] Create backup/restore functionality (for memories)
  - [ ] Add conversation export feature

- [ ] **Advanced Features**
  - [X] Multiple Live2D models/characters
  - [x] Character switching functionality
  - [x] Voice activity detection for hands-free operation
  - [x] Custom voice training integration
  - [ ] Background mode support
  - [ ] Push notifications for scheduled interactions

### Technical Debt & Optimization
- [ ] **Code Quality**
  - [ ] Add comprehensive error handling
  - [ ] Implement proper logging system
  - [ ] Add unit tests for API managers
  - [ ] Create integration tests
  - [ ] Add code documentation

- [ ] **Performance Optimization**
  - [ ] Optimize Live2D rendering performance
  - [ ] Implement audio streaming for large TTS responses
  - [ ] Add memory management for conversation history
  - [ ] Optimize API call batching
  - [ ] Implement caching for frequently used data

- [ ] **Security & Privacy**
  - [x] Add API key management
  - [ ] Implement secure storage for sensitive data
  - [ ] Add privacy controls for conversation data
  - [ ] Implement data encryption
  - [ ] Add user consent management

### Bug Fixes & Issues
- [x] **Touch Gesture System**
  - [x] Implement TouchManager for gesture recognition
  - [x] Add tap, double-tap, and swipe detection
  - [x] Fix coordinate transformation for Live2D view space
  - [x] Integrate gesture callbacks with Live2D animations
  - [x] Add proper lifecycle management to prevent crashes

- [x] **Live2D Model Settings Integration**
  - [x] Fixed default character not respecting settings UI configuration
  - [x] Added settings-based model loading during app initialization
  - [x] Implemented character selection persistence across app restarts
  - [x] Synchronized CharacterManager with Live2D model loading
  - [x] Added robust error handling and fallback mechanisms

- [ ] **Known Issues**
  - [x] Touch coordinates showing -1.0 (coordinate transformation needs fixing)
  - [ ] Fix audio format compatibility issues
  - [ ] Address memory leaks in audio playback
  - [ ] Fix UI responsiveness during API calls
  - [ ] Touch settings are not working correctly after view matrix changes.

## Future Enhancements

- [x] [WakeWord ASR Activation](https://github.com/kahrendt/microWakeWord)
- [x] Smart home device control via natural language
- [ ] Text input dialog for text chat
- [ ] Multiple Live2D models/characters
- [ ] Emotion detection and appropriate animations
- [x] Voice activity detection for hands-free operation
- [ ] Conversation history persistence
- [ ] Custom voice training integration
- [ ] Background mode support
- [ ] Configurable follow-up listening mode

## Future Feature / 2.0 Implementation

### 🥽 Augmented Reality
**"Augmented Reality. Emotional Presence. She's no longer on your screen—she's here"**

Transform your AI companion from a 2D screen experience into a fully immersive 3D presence in your real world:

- **Spatial Presence**: Your AI waifu appears as a 3D hologram in your physical space using ARCore
- **Real-World Interaction**: She can sit on your desk, walk around your room, or follow you through your home
- **Environmental Awareness**: Responds to your real environment - comments on your room, reacts to lighting changes, notices objects
- **Gesture Recognition**: Natural hand gestures for interaction without touching your phone
- **Persistent Placement**: Remembers where you like her to appear in different rooms
- **Scale Adaptation**: Adjusts size based on available space - from desktop companion to life-size presence
- **Occlusion Handling**: Properly hides behind real objects for realistic depth perception
- **Lighting Integration**: Matches your room's lighting conditions for photorealistic appearance
- **Multi-Surface Support**: Can appear on tables, floors, walls, or floating in mid-air
- **Social Presence**: Maintains eye contact and spatial awareness as you move around

**Technical Implementation:**
- ARCore integration for Android AR capabilities
- 3D model conversion from Live2D to volumetric representation
- Real-time environment mapping and occlusion
- Advanced lighting estimation and material rendering
- Spatial audio positioning for immersive voice interaction
- Hand tracking via MediaPipe for gesture recognition
- Cloud anchor support for shared AR experiences

## License

This project uses the Live2D Cubism SDK which has its own licensing terms. Please review the Live2D license before commercial use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review server logs for API issues
3. Verify Live2D SDK setup
4. Check Android logcat for runtime errors
