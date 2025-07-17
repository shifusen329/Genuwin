package com.genuwin.app.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import com.genuwin.app.api.ApiConfig;
import com.genuwin.app.api.models.ChatMessage;
import com.genuwin.app.api.models.ChatRequest;
import com.genuwin.app.api.models.OllamaChatResponse;
import com.genuwin.app.api.models.SimpleChatRequest;
import com.genuwin.app.api.models.TTSRequest;
import com.genuwin.app.api.models.GoogleTTSRequest;
import com.genuwin.app.api.models.GoogleTTSResponse;
import com.genuwin.app.api.services.OllamaService;
import com.genuwin.app.api.services.SimpleOllamaService;
import com.genuwin.app.api.services.STTService;
import com.genuwin.app.api.services.TTSService;
import com.genuwin.app.api.services.GoogleTTSService;
import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.utils.TTSTextProcessor;
import com.genuwin.app.tools.ToolManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manager class for handling API calls to Ollama, TTS, and STT services
 */
public class ApiManager implements SettingsManager.SettingsChangeListener {
    private static final String TAG = "ApiManager";
    
    private static ApiManager instance;
    private Context context;
    private SettingsManager settingsManager;
    
    private OllamaService ollamaService;
    private SimpleOllamaService simpleOllamaService;
    private TTSService ttsService;
    private STTService sttService;
    private GoogleTTSService googleTtsService;
    private ToolManager toolManager;
    
    private List<ChatMessage> conversationHistory;
    
    // Dynamic tool compatibility cache
    private java.util.Map<String, Boolean> modelToolsCompatibilityCache = new java.util.HashMap<>();
    
    // Cached system prompt for tools
    private String cachedToolSystemPrompt = null;
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface TTSCallback {
        void onSuccess(File audioFile);
        void onError(String error);
    }
    
    public interface STTCallback {
        void onSuccess(String transcription);
        void onError(String error);
    }
    
    private ApiManager(Context context) {
        this.context = context.getApplicationContext();
        this.settingsManager = SettingsManager.getInstance(context);
        this.conversationHistory = new ArrayList<>();
        
        // Register for settings changes
        settingsManager.addSettingsChangeListener(this);
        
        initializeServices();
    }
    
    public static synchronized ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
        return instance;
    }
    
    private void initializeServices() {
        // Get timeout settings from SettingsManager
        int connectTimeout = settingsManager.getConnectionTimeout();
        int readTimeout = settingsManager.getReadTimeout();
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
        
        // Initialize Ollama service with settings-based URL
        String ollamaBaseUrl = settingsManager.getOllamaBaseUrl();
        if (!ollamaBaseUrl.endsWith("/")) {
            ollamaBaseUrl += "/";
        }
        Retrofit ollamaRetrofit = new Retrofit.Builder()
                .baseUrl(ollamaBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        // Create separate OpenAI service with OpenAI base URL
        Retrofit openaiRetrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.DEFAULT_OPENAI_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        ollamaService = openaiRetrofit.create(OllamaService.class); // Use OpenAI retrofit for OpenAI calls
        simpleOllamaService = ollamaRetrofit.create(SimpleOllamaService.class); // Keep Ollama for simple calls
        
        // Initialize TTS service with settings-based URL
        String ttsBaseUrl = settingsManager.getTtsBaseUrl();
        if (!ttsBaseUrl.endsWith("/")) {
            ttsBaseUrl += "/";
        }
        Retrofit ttsRetrofit = new Retrofit.Builder()
                .baseUrl(ttsBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ttsService = ttsRetrofit.create(TTSService.class);
        
        // Initialize STT service with settings-based URL
        String sttBaseUrl = settingsManager.getSttBaseUrl();
        if (!sttBaseUrl.endsWith("/")) {
            sttBaseUrl += "/";
        }
        Retrofit sttRetrofit = new Retrofit.Builder()
                .baseUrl(sttBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        sttService = sttRetrofit.create(STTService.class);

        // Initialize Google TTS service
        Retrofit googleTtsRetrofit = new Retrofit.Builder()
                .baseUrl("https://texttospeech.googleapis.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        googleTtsService = googleTtsRetrofit.create(GoogleTTSService.class);
        
        
        // Initialize ToolManager for system prompt tools
        toolManager = ToolManager.getInstance(context);
        
        // Pre-generate and cache the tool system prompt
        cachedToolSystemPrompt = toolManager.generateToolSystemPrompt();
        Log.d(TAG, "Tool system prompt pre-generated and cached");
        Log.d(TAG, "System prompt preview: " + cachedToolSystemPrompt.substring(0, Math.min(200, cachedToolSystemPrompt.length())) + "...");
        
        Log.d(TAG, "Services initialized with settings - Ollama: " + ollamaBaseUrl + 
                   ", TTS: " + ttsBaseUrl + ", STT: " + sttBaseUrl);
    }
    
    public void sendMessage(String message, String system, ChatCallback callback) {
        sendMessage(message, system, null, callback);
    }
    
    /**
     * Send message using a specific model (for memory agent operations)
     */
    public interface MemoryAgentCallback {
        void onResult(String response);
        void onError(String error);
    }

    public void sendMessageWithSpecificModel(String model, String message, String system, MemoryAgentCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        // CRITICAL FIX: Do NOT add memory agent messages to the main conversation history
        // Memory agent calls should be completely isolated from the main conversation
        Log.d(TAG, "Memory agent call - NOT adding to conversation history");

        // Force OpenAI usage for specific model calls (since we're using OpenAI models)
        if (true) { // Always use OpenAI for specific model calls
            sendOpenAIWithSpecificModel(model, message, system, callback);
        } else {
            // Fallback to regular Ollama if needed
            sendOllamaWithUnifiedModel(message, system, null, (ChatCallback) callback);
        }
    }
    
    public void sendMessage(String message, String system, Object tools, ChatCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        // Check which API provider to use
        if ("OpenAI".equals(settingsManager.getApiProvider())) {
            // Use OpenAI with function calling support
            sendOpenAIMessageWithTools(message, system, tools, callback);
            return;
        }

        String model = settingsManager.getOllamaModel();
        
        // Automatically choose tool system based on model compatibility
        if (tools != null && isModelToolsCompatible(model)) {
            // Use native Ollama tools for compatible models
            sendMessageWithNativeTools(message, system, tools, callback);
        } else if (tools != null) {
            // Use system prompt tools for non-compatible models
            sendMessageWithSystemPromptTools(message, system, callback);
        } else {
            // No tools requested, use simple message
            sendSimpleMessage(message, system, callback);
        }
    }
    
    /**
     * Send message using native tools endpoint (supports both Ollama and OpenAI)
     */
    private void sendMessageWithNativeTools(String message, String system, Object tools, ChatCallback callback) {
        // Only add user message to history if message is not empty (for tool result continuations)
        if (!message.isEmpty()) {
            String timestamp = getCurrentTimestamp();
            conversationHistory.add(new ChatMessage("user", message, timestamp));
        }

        // Check if using OpenAI or Ollama
        if ("OpenAI".equals(settingsManager.getApiProvider())) {
            sendOpenAIWithUnifiedModel(message, system, tools, callback);
        } else {
            sendOllamaWithUnifiedModel(message, system, tools, callback);
        }
    }
    
    /**
     * Send message to OpenAI using a specific model (for memory agent)
     * CRITICAL: This method is for internal memory processing only and should NOT affect the main conversation
     */
    private void sendOpenAIWithSpecificModel(String model, String message, String system, MemoryAgentCallback callback) {
        // CRITICAL FIX: Memory agent needs conversation context to analyze, but responses should not be added to history
        
        // Build conversation context for memory agent analysis (READ-ONLY)
        StringBuilder inputBuilder = new StringBuilder();
        
        // Add conversation history for memory agent to analyze
        for (ChatMessage msg : conversationHistory) {
            if ("user".equals(msg.getRole())) {
                inputBuilder.append("User: ").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                inputBuilder.append("Assistant: ").append(msg.getContent()).append("\n");
            } else if ("tool".equals(msg.getRole())) {
                inputBuilder.append("Tool Result: ").append(msg.getContent()).append("\n");
            }
        }
        
        // Add current message (usually the memory agent prompt)
        if (!message.isEmpty()) {
            inputBuilder.append(message);
        }
        
        // Create request for OpenAI using actual API format
        ChatRequest request = new ChatRequest();
        request.setModel(model); // Use the specific model passed in
        request.setInput(inputBuilder.toString()); // Include conversation history for analysis
        
        // Use instructions field for system prompt (OpenAI format)
        if (system != null && !system.trim().isEmpty()) {
            request.setInstructions(system);
        }
        
        Log.d(TAG, "Sending MEMORY AGENT OpenAI request - Model: " + model + 
                   ", Input length: " + inputBuilder.length() + 
                   ", System: " + (system != null ? "present" : "null"));
        
        // Get OpenAI API key
        String apiKey = ApiConfig.getOpenAIApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("OpenAI API key not configured");
            return;
        }
        
        String authHeader = "Bearer " + apiKey;
        Call<OllamaChatResponse> call = ollamaService.openaiResponses(authHeader, "application/json", request);
        
        call.enqueue(new Callback<OllamaChatResponse>() {
            @Override
            public void onResponse(Call<OllamaChatResponse> call, Response<OllamaChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().getOutputText();
                    if (responseText == null || responseText.trim().isEmpty()) {
                        responseText = extractTextFromOutput(response.body().getOutput());
                    }
                    if (responseText == null || responseText.trim().isEmpty()) {
                        responseText = response.body().getResponse();
                    }
                    if (responseText != null && !responseText.trim().isEmpty()) {
                        // CRITICAL: Do NOT add memory agent responses to conversation history
                        // This prevents memory agent JSON from being sent to TTS
                        Log.d(TAG, "Memory agent response received (NOT added to conversation): " + 
                              responseText.substring(0, Math.min(100, responseText.length())) + "...");
                        callback.onResult(responseText);
                    } else {
                        callback.onError("Empty response from Memory LLM");
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read error body", e);
                    }
                    Log.e(TAG, "Memory LLM API Error - Code: " + response.code() + ", Body: " + errorBody);
                    callback.onError("Memory LLM error: " + response.code() + " - " + errorBody);
                }
            }
            
            @Override
            public void onFailure(Call<OllamaChatResponse> call, Throwable t) {
                Log.e(TAG, "Memory agent OpenAI request failed", t);
                callback.onError("Memory agent network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Send message to OpenAI using unified model
     */
    private void sendOpenAIWithUnifiedModel(String message, String system, Object tools, ChatCallback callback) {
        String model = settingsManager.getOpenAIModel();
        
        // Build conversation context for OpenAI input
        StringBuilder inputBuilder = new StringBuilder();
        
        // Add conversation history
        for (ChatMessage msg : conversationHistory) {
            if ("user".equals(msg.getRole())) {
                inputBuilder.append("User: ").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                inputBuilder.append("Assistant: ").append(msg.getContent()).append("\n");
            } else if ("tool".equals(msg.getRole())) {
                inputBuilder.append("Tool Result: ").append(msg.getContent()).append("\n");
            }
        }
        
        // Add current message
        if (!message.isEmpty()) {
            inputBuilder.append("User: ").append(message);
        }
        
        // Create request for OpenAI using actual API format
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        request.setInput(inputBuilder.toString()); // Use string input for OpenAI
        
        // Use instructions field for system prompt (OpenAI format)
        if (system != null && !system.trim().isEmpty()) {
            request.setInstructions(system);
        }
        
        if (tools != null) {
            request.setTools(tools);
        }
        
        Log.d(TAG, "Sending OpenAI request - Model: " + model + 
                   ", Input length: " + inputBuilder.length() + 
                   ", System: " + (system != null ? "present" : "null"));
        
        // DEBUG: Print full request payload
        String requestJson = new com.google.gson.Gson().toJson(request);
        Log.d(TAG, "Full OpenAI request payload: " + requestJson);
        
        // Get OpenAI API key
        String apiKey = ApiConfig.getOpenAIApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("OpenAI API key not configured");
            return;
        }
        
        String authHeader = "Bearer " + apiKey;
        Call<OllamaChatResponse> call = ollamaService.openaiResponses(authHeader, "application/json", request);
        
        call.enqueue(new Callback<OllamaChatResponse>() {
            @Override
            public void onResponse(Call<OllamaChatResponse> call, Response<OllamaChatResponse> response) {
                handleOpenAIResponse(response, callback);
            }
            
            @Override
            public void onFailure(Call<OllamaChatResponse> call, Throwable t) {
                Log.e(TAG, "OpenAI request failed", t);
                callback.onError("OpenAI network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Send message to Ollama using unified model
     */
    private void sendOllamaWithUnifiedModel(String message, String system, Object tools, ChatCallback callback) {
        String model = settingsManager.getOllamaModel();
        
        // Create request for Ollama
        ChatRequest request = new ChatRequest(
                model,
                new ArrayList<>(conversationHistory),
                false,
                system,
                tools
        );
        
        Log.d(TAG, "Sending Ollama request - Model: " + model + 
                   ", Messages: " + request.getMessages().size() + 
                   ", System: " + (system != null ? "present" : "null"));
        
        Call<OllamaChatResponse> call = ollamaService.chat(request);
        call.enqueue(new Callback<OllamaChatResponse>() {
            @Override
            public void onResponse(Call<OllamaChatResponse> call, Response<OllamaChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OllamaChatResponse chatResponse = response.body();
                    if (chatResponse.getMessage() != null) {
                        ChatMessage message = chatResponse.getMessage();
                        
                        // Add assistant response to conversation history
                        conversationHistory.add(message);
                        
                        // Check if the response contains tool calls
                        if (message.hasToolCalls()) {
                            Log.d(TAG, "Native tools response contains " + message.getToolCalls().size() + " tool calls");
                            String toolCallsJson = new com.google.gson.Gson().toJson(message.getToolCalls());
                            Log.d(TAG, "Native tool calls JSON: " + toolCallsJson);
                            // Return a special indicator for tool calls
                            callback.onSuccess("__TOOL_CALLS__:" + toolCallsJson);
                        } else {
                            String assistantMessage = message.getContent();
                            if (assistantMessage == null || assistantMessage.trim().isEmpty()) {
                                Log.w(TAG, "Received empty content from AI (no tool calls either)");
                                callback.onError("Empty response from AI");
                            } else {
                                Log.d(TAG, "Native tools text response: " + assistantMessage);
                                callback.onSuccess(assistantMessage);
                            }
                        }
                    } else {
                        callback.onError("Empty response from AI");
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read error body", e);
                    }
                    Log.e(TAG, "Native tools API Error - Code: " + response.code() + ", Body: " + errorBody);
                    callback.onError("Failed to get response: " + response.code() + " - " + errorBody);
                }
            }
            
            @Override
            public void onFailure(Call<OllamaChatResponse> call, Throwable t) {
                Log.e(TAG, "Native tools request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Send message using system prompt tools (for non-compatible models)
     */
    private void sendMessageWithSystemPromptTools(String message, String system, ChatCallback callback) {
        // Reset tool call tracking for new user messages
        if (!message.isEmpty()) {
            toolManager.resetToolCallTracking();
        }
        
        // Use SystemPromptManager for system prompt tools
        SystemPromptManager promptManager = SystemPromptManager.getInstance(context);
        String systemWithTools = promptManager.buildSystemPromptWithToolDescriptions();
        
        Log.d(TAG, "Using SYSTEM PROMPT TOOLS for model: " + settingsManager.getOllamaModel());
        
        // Send simple message with tools in system prompt
        sendSimpleMessage(message, systemWithTools, new ChatCallback() {
            @Override
            public void onSuccess(String response) {
                // Check if response contains tool calls
                if (toolManager.containsToolCalls(response)) {
                    Log.d(TAG, "System prompt tools detected in response");
                    
                    // Process tool calls using ToolManager
                    toolManager.processResponse(response, new ToolManager.ToolProcessingCallback() {
                        @Override
                        public void onNoToolCalls(String response) {
                            callback.onSuccess(response);
                        }
                        
                        @Override
                        public void onToolCallSuccess(String toolCallId, String toolName, String result, String cleanResponse) {
                            // Add tool result to history and continue conversation
                            addSimpleResultToHistory("Tool " + toolName + " result: " + result);
                            
                            // Send follow-up message with tool result
                            String followUpMessage = "Based on the " + toolName + " result: " + result + 
                                                   (cleanResponse.isEmpty() ? "" : "\n\n" + cleanResponse);
                            sendSimpleMessage("", followUpMessage, callback);
                        }
                        
                        @Override
                        public void onToolCallError(String toolName, String error) {
                            String errorMessage = "Tool " + toolName + " failed: " + error;
                            addSimpleResultToHistory(errorMessage);
                            callback.onError(errorMessage);
                        }
                        
                        @Override
                        public void onMultipleToolCallsComplete(List<String> results, String cleanResponse) {
                            String combinedResults = "Tool results:\n" + String.join("\n", results);
                            addSimpleResultToHistory(combinedResults);
                            
                            String followUpMessage = combinedResults + 
                                                   (cleanResponse.isEmpty() ? "" : "\n\n" + cleanResponse);
                            sendSimpleMessage("", followUpMessage, callback);
                        }
                        
                        @Override
                        public void onToolCallLimitExceeded() {
                            callback.onError("Too many tool calls, please try a simpler request");
                        }
                        
                        @Override
                        public void onRepeatedToolCall(String toolName) {
                            callback.onError("Repeated tool call detected for " + toolName + ", stopping to prevent loops");
                        }
                        
                        @Override
                        public void onUnknownTool(String toolName) {
                            callback.onError("Unknown tool requested: " + toolName);
                        }
                        
                        @Override
                        public void onInvalidParameters(String toolName, String error) {
                            callback.onError("Invalid parameters for " + toolName + ": " + error);
                        }
                        
                        @Override
                        public void onConfirmationRequired(String toolName, String message, Runnable onConfirm, Runnable onCancel) {
                            // For now, auto-confirm tool execution
                            // TODO: Implement user confirmation UI
                            Log.d(TAG, "Auto-confirming tool execution: " + toolName + " - " + message);
                            onConfirm.run();
                        }
                    });
                } else {
                    // No tool calls, return regular response
                    callback.onSuccess(response);
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void generateSpeech(String text, TTSCallback callback) {
        String ttsProvider = settingsManager.getTtsProvider();
        if ("Google".equals(ttsProvider)) {
            generateGoogleSpeech(text, callback);
        } else {
            generateOpenAISpeech(text, callback);
        }
    }

    public void generateGoogleSpeech(String text, TTSCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        String apiKey = ApiConfig.getGoogleApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Google API key not configured");
            return;
        }

        // Process text for TTS-friendly pronunciation
        String processedText = TTSTextProcessor.processForTTS(text);
        Log.d(TAG, "Google TTS text processing: '" + text + "' -> '" + processedText + "'");

        String languageCode = settingsManager.getGoogleTtsLanguageCode();
        String voiceName = settingsManager.getGoogleTtsVoiceName();

        GoogleTTSRequest.SynthesisInput input = new GoogleTTSRequest.SynthesisInput(processedText);
        GoogleTTSRequest.VoiceSelectionParams voice = new GoogleTTSRequest.VoiceSelectionParams(languageCode, voiceName);
        GoogleTTSRequest.AudioConfig audioConfig = new GoogleTTSRequest.AudioConfig("MP3");

        GoogleTTSRequest request = new GoogleTTSRequest(input, voice, audioConfig);

        Call<GoogleTTSResponse> call = googleTtsService.synthesize(apiKey, request);
        call.enqueue(new Callback<GoogleTTSResponse>() {
            @Override
            public void onResponse(Call<GoogleTTSResponse> call, Response<GoogleTTSResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String base64Audio = response.body().getAudioContent();
                        byte[] audioData = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT);
                        File audioFile = saveAudioToFile(audioData);
                        callback.onSuccess(audioFile);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to save Google TTS audio file", e);
                        callback.onError("Failed to save audio: " + e.getMessage());
                    }
                } else {
                    callback.onError("Failed to generate Google speech: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GoogleTTSResponse> call, Throwable t) {
                Log.e(TAG, "Google TTS request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void generateOpenAISpeech(String text, TTSCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        // Get audio API key
        String audioApiKey = ApiConfig.getAudioApiKey(context);
        if (audioApiKey == null || audioApiKey.trim().isEmpty()) {
            callback.onError("Audio API key not configured");
            return;
        }

        // Process text for TTS-friendly pronunciation
        String processedText = TTSTextProcessor.processForTTS(text);
        Log.d(TAG, "OpenAI TTS text processing: '" + text + "' -> '" + processedText + "'");

        // Use settings-based TTS model and voice
        String ttsModel = settingsManager.getTtsModel();
        String ttsVoice = settingsManager.getTtsVoice();
        TTSRequest request = new TTSRequest(
                ttsModel,
                processedText,
                ttsVoice
        );

        String authHeader = "Bearer " + audioApiKey;
        Call<ResponseBody> call = ttsService.generateSpeech(authHeader, request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Save audio to temporary file
                        File audioFile = saveAudioToFile(response.body());
                        callback.onSuccess(audioFile);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to save audio file", e);
                        callback.onError("Failed to save audio: " + e.getMessage());
                    }
                } else {
                    callback.onError("Failed to generate speech: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "TTS request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    public void transcribeAudio(File audioFile, STTCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }
        
        // Get audio API key
        String audioApiKey = ApiConfig.getAudioApiKey(context);
        if (audioApiKey == null || audioApiKey.trim().isEmpty()) {
            callback.onError("Audio API key not configured");
            return;
        }
        
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/wav"), audioFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", audioFile.getName(), requestFile);
        
        // Use settings-based STT model
        String sttModel = settingsManager.getSttModel();
        String authHeader = "Bearer " + audioApiKey;
        Call<ResponseBody> call = sttService.transcribeAudio(authHeader, body, sttModel);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseString).getAsJsonObject();
                        String transcription = jsonObject.get("text").getAsString();
                        callback.onSuccess(transcription);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read transcription response", e);
                        callback.onError("Failed to read response: " + e.getMessage());
                    }
                } else {
                    callback.onError("Failed to transcribe audio: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "STT request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    private File saveAudioToFile(ResponseBody body) throws IOException {
        File audioFile = new File(context.getCacheDir(), "tts_audio_" + System.currentTimeMillis() + ".mp3");
        
        try (InputStream inputStream = body.byteStream();
             FileOutputStream outputStream = new FileOutputStream(audioFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return audioFile;
    }

    private File saveAudioToFile(byte[] audioData) throws IOException {
        File audioFile = new File(context.getCacheDir(), "tts_audio_" + System.currentTimeMillis() + ".mp3");
        try (FileOutputStream outputStream = new FileOutputStream(audioFile)) {
            outputStream.write(audioData);
        }
        return audioFile;
    }
    
    public void clearConversationHistory() {
        conversationHistory.clear();
    }

    public void addMessageToHistory(ChatMessage message) {
        conversationHistory.add(message);
    }
    
    public void addToolResultToHistory(String toolCallId, String result) {
        String timestamp = getCurrentTimestamp();
        ChatMessage toolResult = new ChatMessage("tool", result, toolCallId, timestamp);
        conversationHistory.add(toolResult);
        Log.d(TAG, "Added tool result to conversation history - ID: " + toolCallId);
    }
    
    public List<ChatMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        return sdf.format(new Date());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Check if the current model supports native tools endpoint
     * Note: Ollama now strictly uses system prompt tools, only OpenAI uses native tools
     */
    private boolean isModelToolsCompatible(String model) {
        // Ollama models always use system prompt tools now
        Log.d(TAG, "Model " + model + " will use system prompt tools (Ollama policy)");
        return false;
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        // Handle settings changes that require service reinitialization
        switch (key) {
            case SettingsManager.Keys.OLLAMA_BASE_URL:
            case SettingsManager.Keys.TTS_BASE_URL:
            case SettingsManager.Keys.STT_BASE_URL:
            case SettingsManager.Keys.CONNECTION_TIMEOUT:
            case SettingsManager.Keys.READ_TIMEOUT:
                Log.d(TAG, "API configuration changed, reinitializing services: " + key + " = " + value);
                initializeServices();
                break;
            case SettingsManager.Keys.OLLAMA_MODEL:
            case SettingsManager.Keys.TTS_MODEL:
            case SettingsManager.Keys.TTS_VOICE:
            case SettingsManager.Keys.STT_MODEL:
                Log.d(TAG, "Model configuration changed: " + key + " = " + value);
                // Model changes don't require service reinitialization, just logging
                break;
        }
    }
    
    /**
     * New simplified sendMessage method for the new tools system (without native tools support)
     */
    public void sendSimpleMessage(String message, String system, ChatCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onError("No internet connection");
            return;
        }

        // Only add user message to history if message is not empty (for tool result continuations)
        if (!message.isEmpty()) {
            String timestamp = getCurrentTimestamp();
            conversationHistory.add(new ChatMessage("user", message, timestamp));
        }

        // Create simplified request without tools using settings-based model
        String model = settingsManager.getOllamaModel();
        SimpleChatRequest request = new SimpleChatRequest(
                model,
                new ArrayList<>(conversationHistory),
                false,
                system
        );
        
        // Debug logging
        Log.d(TAG, "Sending simple chat request - Model: " + request.getModel() + 
                   ", Messages: " + request.getMessages().size() + 
                   ", System: " + (system != null ? "present" : "null"));
        
        Call<OllamaChatResponse> call = simpleOllamaService.chat(request);
        call.enqueue(new Callback<OllamaChatResponse>() {
            @Override
            public void onResponse(Call<OllamaChatResponse> call, Response<OllamaChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OllamaChatResponse chatResponse = response.body();
                    if (chatResponse.getMessage() != null) {
                        ChatMessage message = chatResponse.getMessage();
                        
                        // Add assistant response to conversation history
                        conversationHistory.add(message);
                        
                        String assistantMessage = message.getContent();
                        if (assistantMessage == null || assistantMessage.trim().isEmpty()) {
                            Log.w(TAG, "Received empty content from AI");
                            Log.d(TAG, "Full message object: " + new com.google.gson.Gson().toJson(message));
                            callback.onError("Empty response from AI");
                        } else {
                            Log.d(TAG, "Simple text response: " + assistantMessage);
                            callback.onSuccess(assistantMessage);
                        }
                    } else {
                        callback.onError("Empty response from AI");
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read error body", e);
                    }
                    Log.e(TAG, "Simple Chat API Error - Code: " + response.code() + ", Body: " + errorBody);
                    callback.onError("Failed to get response: " + response.code() + " - " + errorBody);
                }
            }
            
            @Override
            public void onFailure(Call<OllamaChatResponse> call, Throwable t) {
                Log.e(TAG, "Simple chat request failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Send message using OpenAI API with function calling support
     */
    private void sendOpenAIMessage(String message, String system, ChatCallback callback) {
        sendOpenAIMessageWithTools(message, system, null, callback);
    }
    
    /**
     * Send message using OpenAI API with tools support
     * This method now delegates to the unified OpenAI implementation
     */
    private void sendOpenAIMessageWithTools(String message, String system, Object tools, ChatCallback callback) {
        // Delegate to the unified OpenAI implementation
        sendOpenAIWithUnifiedModel(message, system, tools, callback);
    }
    
    /**
     * Handle OpenAI response format
     */
    private void handleOpenAIResponse(Response<OllamaChatResponse> response, ChatCallback callback) {
        if (response.isSuccessful() && response.body() != null) {
            OllamaChatResponse openAIResponse = response.body();
            
            // DEBUG: Print full response payload
            String responseJson = new com.google.gson.Gson().toJson(openAIResponse);
            Log.d(TAG, "Full OpenAI response: " + responseJson);
            
            // Check if response contains function calls in the output array (OpenAI format)
            List<Object> functionCalls = extractFunctionCallsFromOutput(openAIResponse.getOutput());
            if (functionCalls != null && !functionCalls.isEmpty()) {
                Log.d(TAG, "Found function call in OpenAI output array");
                
                // Convert OpenAI function call format to tool call format
                String toolCallsJson = convertOpenAIFunctionCallsToToolCalls(functionCalls);
                
                // Create a ChatMessage with tool calls
                String timestamp = getCurrentTimestamp();
                ChatMessage assistantMessage = new ChatMessage("assistant", "", timestamp);
                conversationHistory.add(assistantMessage);
                
                Log.d(TAG, "OpenAI function call converted to tool calls: " + toolCallsJson);
                
                // Return the same format as Ollama native tools
                callback.onSuccess("__TOOL_CALLS__:" + toolCallsJson);
            } else {
                // Regular text response - use output_text convenience field first
                String responseText = openAIResponse.getOutputText();
                
                // Fallback to parsing the output array if output_text is not available
                if (responseText == null || responseText.trim().isEmpty()) {
                    responseText = extractTextFromOutput(openAIResponse.getOutput());
                }
                
                // Final fallback to response field
                if (responseText == null || responseText.trim().isEmpty()) {
                    responseText = openAIResponse.getResponse();
                }
                
                if (responseText != null && !responseText.trim().isEmpty()) {
                    // Add assistant response to conversation history
                    String timestamp = getCurrentTimestamp();
                    conversationHistory.add(new ChatMessage("assistant", responseText, timestamp));
                    
        Log.d(TAG, "OpenAI text response: " + responseText);
        callback.onSuccess(responseText);
                } else {
                    Log.w(TAG, "Empty response from OpenAI");
                    Log.d(TAG, "Full OpenAI response: " + new com.google.gson.Gson().toJson(openAIResponse));
                    callback.onError("Empty response from OpenAI");
                }
            }
        } else {
            String errorBody = "";
            try {
                if (response.errorBody() != null) {
                    errorBody = response.errorBody().string();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read OpenAI error body", e);
            }
            Log.e(TAG, "OpenAI API Error - Code: " + response.code() + ", Body: " + errorBody);
            callback.onError("OpenAI API error: " + response.code() + " - " + errorBody);
        }
    }
    
    /**
     * Add a simple text result to conversation history (for new tools system)
     */
    public void addSimpleResultToHistory(String result) {
        String timestamp = getCurrentTimestamp();
        ChatMessage toolResult = new ChatMessage("assistant", result, timestamp);
        conversationHistory.add(toolResult);
        Log.d(TAG, "Added simple result to conversation history");
    }
    
    /**
     * Check if OpenAI output array contains function calls
     */
    private boolean hasToolCallsInOutput(List<Object> output) {
        if (output == null || output.isEmpty()) {
            return false;
        }
        
        try {
            for (Object outputItem : output) {
                if (outputItem instanceof java.util.Map) {
                    java.util.Map<String, Object> outputMap = (java.util.Map<String, Object>) outputItem;
                    String type = (String) outputMap.get("type");
                    
                    if ("function_call".equals(type)) {
                        Log.d(TAG, "Found function call in OpenAI output array");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for tool calls in output array: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Extract text content from OpenAI output array
     * Based on OpenAI documentation: output[].content[].text
     */
    private String extractTextFromOutput(List<Object> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        
        StringBuilder textBuilder = new StringBuilder();
        
        try {
            for (Object outputItem : output) {
                if (outputItem instanceof java.util.Map) {
                    java.util.Map<String, Object> outputMap = (java.util.Map<String, Object>) outputItem;
                    
                    // Check if this is a message type output
                    String type = (String) outputMap.get("type");
                    if ("message".equals(type)) {
                        Object contentObj = outputMap.get("content");
                        if (contentObj instanceof java.util.List) {
                            java.util.List<Object> contentList = (java.util.List<Object>) contentObj;
                            
                            for (Object contentItem : contentList) {
                                if (contentItem instanceof java.util.Map) {
                                    java.util.Map<String, Object> contentMap = (java.util.Map<String, Object>) contentItem;
                                    String contentType = (String) contentMap.get("type");
                                    
                                    if ("output_text".equals(contentType)) {
                                        String text = (String) contentMap.get("text");
                                        if (text != null && !text.trim().isEmpty()) {
                                            if (textBuilder.length() > 0) {
                                                textBuilder.append(" ");
                                            }
                                            textBuilder.append(text);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing OpenAI output array: " + e.getMessage());
            return null;
        }
        
        String result = textBuilder.toString().trim();
        Log.d(TAG, "Extracted text from output array: " + result);
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Extract function calls from OpenAI output array
     */
    private List<Object> extractFunctionCallsFromOutput(List<Object> output) {
        List<Object> functionCalls = new ArrayList<>();
        
        if (output == null || output.isEmpty()) {
            return functionCalls;
        }
        
        try {
            for (Object outputItem : output) {
                if (outputItem instanceof java.util.Map) {
                    java.util.Map<String, Object> outputMap = (java.util.Map<String, Object>) outputItem;
                    String type = (String) outputMap.get("type");
                    
                    if ("function_call".equals(type)) {
                        functionCalls.add(outputItem);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting function calls from output: " + e.getMessage());
        }
        
        return functionCalls;
    }
    
    /**
     * Convert OpenAI function call format to tool call format expected by ToolCallParser
     */
    private String convertOpenAIFunctionCallsToToolCalls(List<Object> functionCalls) {
        List<java.util.Map<String, Object>> toolCalls = new ArrayList<>();
        
        try {
            for (Object functionCallObj : functionCalls) {
                if (functionCallObj instanceof java.util.Map) {
                    java.util.Map<String, Object> functionCall = (java.util.Map<String, Object>) functionCallObj;
                    
                    // Extract function call details
                    String id = (String) functionCall.get("id");
                    String callId = (String) functionCall.get("call_id");
                    String name = (String) functionCall.get("name");
                    String argumentsStr = (String) functionCall.get("arguments");
                    
                    // Create tool call in the format expected by ToolCallParser ALT_JSON_PATTERN
                    // Pattern expects: {"name": "tool_name", "arguments": "..."}
                    java.util.Map<String, Object> toolCall = new java.util.HashMap<>();
                    toolCall.put("name", name);
                    toolCall.put("arguments", argumentsStr); // Keep as JSON string for ToolCallParser
                    toolCall.put("id", id != null ? id : callId); // Add id for tracking
                    
                    toolCalls.add(toolCall);
                    
                    Log.d(TAG, "Converted OpenAI function call: " + name + " with args: " + argumentsStr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting OpenAI function calls to tool calls: " + e.getMessage());
        }
        
        // Convert to JSON string for ToolCallParser
        return new com.google.gson.Gson().toJson(toolCalls);
    }
    
    /**
     * Clean up resources when the manager is no longer needed
     */
    public void cleanup() {
        if (settingsManager != null) {
            settingsManager.removeSettingsChangeListener(this);
        }
    }
}
