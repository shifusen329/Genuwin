package com.genuwin.app.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.genuwin.app.tools.ToolManager;
import com.genuwin.app.vad.VADConfig;
import com.genuwin.app.wakeword.WakeWordManager;
import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.formatting.MemoryContextFormatter;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class ConversationManager implements WakeWordManager.WakeWordListener {
    private static final String TAG = "ConversationManager";
    
    public enum State {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING
    }

    private State state = State.IDLE;
    private final WakeWordManager wakeWordManager;
    private final AudioManager audioManager;
    private final ApiManager apiManager;
    private final ToolManager toolManager;
    private final SettingsManager settingsManager;
    private final EmotionManager emotionManager;
    private final MemoryManager memoryManager;
    private final VoiceParameterManager voiceParameterManager;
    private final Context context;
    private final ConversationCallback callback;
    
    // Tool tracking handled by ToolManager
    private String lastSearchResults = null;
    private String currentToolCallId = null;
    private String lastExecutedToolType = null;

    public interface ConversationCallback {
        void onStateChanged(State state);
        void onTranscription(String text);
        void onResponse(String text);
        void onError(String error);
    }

    public ConversationManager(Context context, ConversationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.wakeWordManager = new WakeWordManager(context, this);
        this.audioManager = new AudioManager(context);
        this.apiManager = ApiManager.getInstance(context);
        this.toolManager = ToolManager.getInstance(context);
        this.settingsManager = SettingsManager.getInstance(context);
        this.emotionManager = EmotionManager.getInstance();
        this.memoryManager = MemoryManager.getInstance(context);
        this.voiceParameterManager = VoiceParameterManager.getInstance();
        
        // Set up wake word manager reference in CharacterManager for automatic model switching
        CharacterManager.getInstance(context).setWakeWordManager(this.wakeWordManager);
    }

    public void start() {
        wakeWordManager.start();
        setState(State.IDLE);
    }

    public void stop() {
        wakeWordManager.stop();
        audioManager.stopRecording();
    }

    @Override
    public void onWakeWordDetected() {
        if (state == State.IDLE) {
            startListening();
        }
    }

    public void startListening() {
        setState(State.LISTENING);
        wakeWordManager.pause();
        
        // Use settings-based VAD configuration for initial listening
        int threshold = settingsManager.getVadSilenceThreshold();
        long duration = settingsManager.getVadMinSilenceDuration();
        
        // Use AudioManager's enhanced initial listening with timeout
        audioManager.startInitialListening(new VADConfig(threshold, duration), new AudioManager.RecordingCallback() {
            @Override
            public void onRecordingStarted() {}

            @Override
            public void onRecordingStopped(File audioFile) {
                setState(State.PROCESSING);
                transcribeAudio(audioFile);
            }

            @Override
            public void onRecordingError(String error) {
                if ("No meaningful audio detected".equals(error)) {
                    // This is expected when there's no follow-up input, just return to wake word mode
                    setState(State.IDLE);
                    wakeWordManager.resume();
                } else {
                    // This is a real error
                    callback.onError(error);
                    setState(State.IDLE);
                    wakeWordManager.resume();
                }
            }
        }, 10000); // 10 seconds timeout for initial listening
    }

    private void transcribeAudio(File audioFile) {
        apiManager.transcribeAudio(audioFile, new ApiManager.STTCallback() {
            @Override
            public void onSuccess(String transcription) {
                callback.onTranscription(transcription);
                sendMessageToAI(transcription);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
                setState(State.IDLE);
                wakeWordManager.resume();
            }
        });
    }

    private void sendMessageToAI(String message) {
        // Check for voice parameter commands first (only for actual user messages, not tool results)
        if (!message.startsWith("SEARCH_RESULTS:") && !message.startsWith("SCRAPE_RESULTS:") && 
            voiceParameterManager.isVoiceParameterControlEnabled()) {
            
            boolean wasParameterCommand = voiceParameterManager.processVoiceCommand(message);
            if (wasParameterCommand) {
                // Parameter command was processed, provide acknowledgment and return to listening
                Log.d(TAG, "Voice parameter command processed: " + message);
                
                // Generate a brief acknowledgment response
                String acknowledgment = "Okay.";
                callback.onResponse(acknowledgment);
                generateSpeech(acknowledgment);
                return; // Don't send to AI for regular conversation
            }
        }
        
        // Reset tool call tracking for new user messages (not tool results)
        if (!message.startsWith("SEARCH_RESULTS:") && !message.startsWith("SCRAPE_RESULTS:")) {
            toolManager.resetToolCallTracking();
            lastSearchResults = null;
            android.util.Log.d("ConversationManager", "Reset tool call tracking for new user message");
        }
        
        // Inject memory context as fake tool response (only for actual user messages, not tool results)
        if (!message.startsWith("SEARCH_RESULTS:") && !message.startsWith("SCRAPE_RESULTS:") && 
            !toolManager.isToolCallActive()) {
            
            // CRITICAL FIX: Wait for memory injection to complete before sending to LLM
            injectMemoryContextAndContinue(message);
        } else {
            // No memory injection needed, proceed directly
            sendMessageToLLM(message);
        }
    }
    
    /**
     * Inject memory context and then continue with LLM call
     * This ensures memory context is properly added before the LLM request
     */
    private void injectMemoryContextAndContinue(String userMessage) {
        // Wait for memory system initialization before attempting to inject context
        memoryManager.waitForInitialization()
            .thenCompose(v -> {
                // Double-check initialization status after waiting
                if (!memoryManager.isInitialized()) {
                    Log.d(TAG, "Memory system initialization failed, skipping memory context injection");
                    return java.util.concurrent.CompletableFuture.completedFuture(java.util.Collections.emptyList());
                }
                
                Log.d(TAG, "Memory system initialized, retrieving relevant memories for context injection");
                return memoryManager.retrieveRelevantMemories(userMessage, 5);
            })
            .thenAccept(relevantMemories -> {
                if (relevantMemories != null && !relevantMemories.isEmpty()) {
                    // Get memory relationships for enhanced context
                    Map<String, List<MemoryRelationship>> relationships = memoryManager.getMemoryRelationships(relevantMemories);
                    
                    // Use advanced formatter for memory context
                    MemoryContextFormatter formatter = new MemoryContextFormatter(context);
                    int maxTokens = formatter.getMaxMemoryTokens();
                    String formattedContext = formatter.formatMemoryContext(relevantMemories, relationships, maxTokens);
                    
                    if (!formattedContext.isEmpty()) {
                        // Inject as fake tool response with a consistent tool call ID
                        String memoryToolCallId = "memory_context_" + System.currentTimeMillis();
                        apiManager.addToolResultToHistory(memoryToolCallId, formattedContext);
                        
                        Log.d(TAG, "Injected " + relevantMemories.size() + " relevant memories with advanced formatting");
                        Log.d(TAG, "Memory context preview: " + formattedContext.substring(0, 
                            Math.min(300, formattedContext.length())) + "...");
                    } else {
                        Log.d(TAG, "Memory formatter returned empty context");
                    }
                } else {
                    Log.d(TAG, "No relevant memories found for context injection");
                }
                
                // CRITICAL: Now that memory injection is complete, send to LLM
                sendMessageToLLM(userMessage);
            })
            .exceptionally(e -> {
                Log.e(TAG, "Error injecting memory context: " + e.getMessage(), e);
                // Don't fail the conversation if memory injection fails - continue without memory
                sendMessageToLLM(userMessage);
                return null;
            });
    }
    
    /**
     * Send message to LLM after memory injection is complete
     */
    private void sendMessageToLLM(String message) {
        // Use SystemPromptManager for clean, non-redundant system prompt construction
        SystemPromptManager promptManager = SystemPromptManager.getInstance(context);
        String systemInstructions = promptManager.buildSystemPrompt(true); // Include tools
        Object tools = promptManager.getTools();
        
        apiManager.sendMessage(message, systemInstructions, tools, new ApiManager.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                // Delegate tool processing to ToolManager
                toolManager.processResponse(response, new ToolManager.ToolProcessingCallback() {
                    @Override
                    public void onNoToolCalls(String response) {
                        // Regular text response - send to TTS immediately
                        callback.onResponse(response);
                        generateSpeech(response);
                        
                        // Process conversation for memory extraction and storage AFTER TTS
                        // This runs asynchronously in the background and doesn't affect the conversation flow
                        processConversationForMemory(message, response);
                    }
                    
                    @Override
                    public void onToolCallSuccess(String toolCallId, String toolName, String result, String cleanResponse) {
                        // Store search results for potential reuse
                        if (toolName.equals("searxng_search") || toolName.equals("search")) {
                            lastSearchResults = result;
                        }
                        
                        // Track the executed tool type for follow-up prompt
                        lastExecutedToolType = toolName;
                        
                        // Add tool result to conversation history
                        apiManager.addToolResultToHistory(toolCallId, result);
                        
                        // Continue conversation with the updated history
                        continueConversationAfterToolResult();
                    }
                    
                    @Override
                    public void onToolCallError(String toolName, String error) {
                        callback.onError("Tool execution failed: " + error);
                        setState(State.IDLE);
                        wakeWordManager.resume();
                    }
                    
                    @Override
                    public void onMultipleToolCallsComplete(List<String> results, String cleanResponse) {
                        // Handle multiple tool results
                        String combinedResults = String.join("\n", results);
                        apiManager.addToolResultToHistory(currentToolCallId, combinedResults);
                        continueConversationAfterToolResult();
                    }
                    
                    @Override
                    public void onToolCallLimitExceeded() {
                        generateSpeech("I found some information, but let me provide you with what I have so far.");
                    }
                    
                    @Override
                    public void onRepeatedToolCall(String toolName) {
                        if (lastSearchResults != null && (toolName.equals("searxng_search") || toolName.equals("search"))) {
                            android.util.Log.d("ConversationManager", "Using stored search results for final response");
                            String finalPrompt = lastSearchResults + "\n\nBased on the above search results, please provide a direct answer to the user's question. Do not search again.";
                            sendMessageToAI(finalPrompt);
                        } else {
                            generateSpeech("I've searched for that information. Based on what I found, let me give you an answer.");
                        }
                    }
                    
                    @Override
                    public void onUnknownTool(String toolName) {
                        generateSpeech("I'm sorry, I don't know how to use that tool: " + toolName);
                    }
                    
                    @Override
                    public void onInvalidParameters(String toolName, String error) {
                        generateSpeech("I'm sorry, there was an issue with the tool parameters: " + error);
                    }
                    
                    @Override
                    public void onConfirmationRequired(String toolName, String message, Runnable onConfirm, Runnable onCancel) {
                        // For MVP, auto-confirm tool executions
                        // TODO: Implement user confirmation dialog in future
                        onConfirm.run();
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
                setState(State.IDLE);
                wakeWordManager.resume();
            }
        });
    }
    

    private void generateSpeech(String text) {
        setState(State.SPEAKING);
        apiManager.generateSpeech(text, new ApiManager.TTSCallback() {
            @Override
            public void onSuccess(File audioFile) {
                audioManager.playAudio(audioFile, new AudioManager.PlaybackCallback() {
                    @Override
                    public void onPlaybackStarted() {
                        // Notify EmotionManager that TTS has started
                        emotionManager.onTTSStarted();
                    }

                    @Override
                    public void onPlaybackCompleted() {
                        // Notify EmotionManager that TTS has finished
                        emotionManager.onTTSFinished();
                        startFollowUpListener();
                    }

                    @Override
                    public void onPlaybackInterrupted() {
                        // TTS was interrupted, notify EmotionManager and don't start follow-up listener
                        emotionManager.onTTSFinished();
                        // The interruption handler will determine the next state
                        Log.d(TAG, "TTS playback was interrupted");
                    }

                    @Override
                    public void onPlaybackError(String error) {
                        // TTS error, notify EmotionManager
                        emotionManager.onTTSFinished();
                        callback.onError(error);
                        setState(State.IDLE);
                        wakeWordManager.resume();
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
                setState(State.IDLE);
                wakeWordManager.resume();
            }
        });
    }

    private void startFollowUpListener() {
        setState(State.LISTENING);
        
        // Use settings-based VAD configuration for follow-up listening
        int followupThreshold = settingsManager.getVadFollowupThreshold();
        long followupDuration = settingsManager.getVadFollowupDuration();
        
        // Use AudioManager's enhanced follow-up listening with timeout
        audioManager.startFollowUpListening(new VADConfig(followupThreshold, followupDuration), new AudioManager.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                android.util.Log.d("ConversationManager", "Follow-up listening started with VAD(" + followupThreshold + ", " + followupDuration + ")");
            }

            @Override
            public void onRecordingStopped(File audioFile) {
                android.util.Log.d("ConversationManager", "Follow-up VAD detected speech end, processing audio");
                setState(State.PROCESSING);
                transcribeAudio(audioFile);
            }

            @Override
            public void onRecordingError(String error) {
                android.util.Log.d("ConversationManager", "Follow-up recording error: " + error);
                
                if ("No meaningful audio detected".equals(error)) {
                    // This is expected when there's no follow-up input, just return to wake word mode
                    android.util.Log.d("ConversationManager", "No follow-up input detected, returning to wake word mode with cooldown.");
                    setState(State.IDLE);
                    // Introduce a cooldown before resuming wake word detection
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        wakeWordManager.resume();
                    }, followupDuration);
                } else {
                    // This is a real error
                    callback.onError(error);
                    setState(State.IDLE);
                    wakeWordManager.resume();
                }
            }
        }, 2000); // 2 seconds - shorter timeout for better responsiveness
    }

    private void continueConversationAfterToolResult() {
        android.util.Log.d("ConversationManager", "Continuing conversation after tool result");
        
        // Use SystemPromptManager for clean, non-redundant system prompt construction
        SystemPromptManager promptManager = SystemPromptManager.getInstance(context);
        String systemInstructions = promptManager.buildSystemPrompt(true); // Include tools
        Object tools = promptManager.getTools();
        
        // Use SystemPromptManager to get appropriate follow-up prompt based on tool type
        String contextMessage = promptManager.getToolFollowUpPrompt(lastExecutedToolType);
        
        apiManager.sendMessage(contextMessage, systemInstructions, tools, new ApiManager.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                // Delegate tool processing to ToolManager (same pattern as sendMessageToAI)
                toolManager.processResponse(response, new ToolManager.ToolProcessingCallback() {
                    @Override
                    public void onNoToolCalls(String response) {
                        // Regular text response - conversation complete
                        callback.onResponse(response);
                        generateSpeech(response);
                    }
                    
                    @Override
                    public void onToolCallSuccess(String toolCallId, String toolName, String result, String cleanResponse) {
                        // Another tool call - add result and continue
                        apiManager.addToolResultToHistory(toolCallId, result);
                        continueConversationAfterToolResult();
                    }
                    
                    @Override
                    public void onToolCallError(String toolName, String error) {
                        callback.onError("Tool execution failed: " + error);
                        setState(State.IDLE);
                        wakeWordManager.resume();
                    }
                    
                    @Override
                    public void onMultipleToolCallsComplete(List<String> results, String cleanResponse) {
                        String combinedResults = String.join("\n", results);
                        apiManager.addToolResultToHistory(currentToolCallId, combinedResults);
                        continueConversationAfterToolResult();
                    }
                    
                    @Override
                    public void onToolCallLimitExceeded() {
                        generateSpeech("I found some information, but let me provide you with what I have so far.");
                    }
                    
                    @Override
                    public void onRepeatedToolCall(String toolName) {
                        generateSpeech("I've already looked into that. Let me give you what I found.");
                    }
                    
                    @Override
                    public void onUnknownTool(String toolName) {
                        generateSpeech("I'm sorry, I don't know how to use that tool: " + toolName);
                    }
                    
                    @Override
                    public void onInvalidParameters(String toolName, String error) {
                        generateSpeech("I'm sorry, there was an issue with the tool parameters: " + error);
                    }
                    
                    @Override
                    public void onConfirmationRequired(String toolName, String message, Runnable onConfirm, Runnable onCancel) {
                        // For MVP, auto-confirm tool executions
                        onConfirm.run();
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
                setState(State.IDLE);
                wakeWordManager.resume();
            }
        });
    }

    private void setState(State newState) {
        if (state != newState) {
            state = newState;
            callback.onStateChanged(state);
        }
    }
    
    /**
     * Interrupt TTS and transition to listening mode (single tap)
     */
    public void interruptToListening() {
        if (state == State.SPEAKING) {
            android.util.Log.d(TAG, "Interrupting TTS to start listening");
            audioManager.interruptPlayback();
            setState(State.LISTENING);
            startListening();
        }
    }
    
    /**
     * Interrupt TTS and return to idle mode (double tap)
     */
    public void interruptToIdle() {
        if (state == State.SPEAKING) {
            android.util.Log.d(TAG, "Interrupting TTS to return to idle");
            audioManager.interruptPlayback();
            setState(State.IDLE);
            wakeWordManager.start();
        }
    }
    
    /**
     * Get current conversation state
     */
    public State getCurrentState() {
        return state;
    }
    
    
    /**
     * Process the conversation for memory extraction and storage
     * This analyzes the user message and AI response to extract meaningful memories
     * CRITICAL: This runs completely asynchronously and does NOT affect the conversation flow
     */
    private void processConversationForMemory(String userMessage, String aiResponse) {
        // Run memory processing in a separate thread to ensure it doesn't interfere with conversation
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting background memory processing for conversation");
                
                // Use MemoryManager to process the conversation asynchronously
                memoryManager.processConversation(userMessage, aiResponse, new MemoryManager.MemoryProcessingCallback() {
                    @Override
                    public void onMemoryProcessed(List<Memory> extractedMemories) {
                        if (extractedMemories != null && !extractedMemories.isEmpty()) {
                            Log.d(TAG, "Successfully processed " + extractedMemories.size() + " memories from conversation");
                            for (Memory memory : extractedMemories) {
                                Log.d(TAG, "Stored memory: " + memory.getContent());
                            }
                        } else {
                            Log.d(TAG, "No significant memories extracted from conversation");
                        }
                    }
                    
                    @Override
                    public void onMemoryProcessingError(String error) {
                        Log.e(TAG, "Error processing conversation for memory: " + error);
                        // Don't fail the conversation if memory processing fails
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error initiating memory processing: " + e.getMessage(), e);
                // Don't fail the conversation if memory processing fails
            }
        }).start();
    }
}
