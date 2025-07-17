package com.genuwin.app.managers;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.live2d.WaifuDefine;
import com.genuwin.app.live2d.WaifuLive2DManager;
import com.genuwin.app.live2d.WaifuModel;
import com.genuwin.app.live2d.WaifuPal;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Manages emotion detection and expression mapping based on conversation context.
 * Analyzes user input and AI responses to determine appropriate emotional expressions
 * and maps them to available Live2D expressions.
 */
public class EmotionManager {
    private static final String TAG = "EmotionManager";
    private static EmotionManager instance;
    
    // Emotion categories based on conversation context
    public enum Emotion {
        NEUTRAL,        // Default, calm state
        HAPPY,          // Joy, contentment, positive responses
        VERY_HAPPY,     // Excitement, great joy, enthusiasm
        SAD,            // Sadness, disappointment, melancholy
        ANGRY,          // Frustration, annoyance, anger
        SURPRISED,      // Shock, amazement, unexpected responses
        EMBARRASSED,    // Shy, bashful, flustered responses
        SUSPICIOUS,     // Doubtful, questioning, skeptical
        CONFUSED        // Puzzled, uncertain, perplexed
    }
    
    // Expression mappings based on parameter analysis
    private static final Map<Emotion, String> EXPRESSION_MAPPINGS = new HashMap<>();
    private static final Map<Emotion, List<String>> FALLBACK_MAPPINGS = new HashMap<>();
    
    // Motion mappings for enhanced emotional expression
    private static final Map<Emotion, List<Integer>> MOTION_MAPPINGS = new HashMap<>();
    private static final Map<Emotion, List<Integer>> IDLE_MOTION_MAPPINGS = new HashMap<>();
    
    // Emotion detection patterns
    private static final Map<Emotion, List<Pattern>> EMOTION_PATTERNS = new HashMap<>();
    
    // Current emotional state
    private Emotion currentEmotion = Emotion.NEUTRAL;
    private float emotionIntensity = 1.0f;
    private long lastEmotionChange = 0;
    private String lastTrigger = "";
    
    // TTS state tracking
    private boolean isTTSSpeaking = false;
    private Emotion pendingEmotion = null;
    private String pendingTrigger = "";
    
    // Emotion persistence and decay
    private static final long EMOTION_DURATION = 5000; // 5 seconds
    private static final long MIN_EMOTION_INTERVAL = 1000; // 1 second between changes
    
    // Idle expression cycling (integrated from IdleBehaviorManager)
    private static final long IDLE_EXPRESSION_INTERVAL = 25000; // 25 seconds for more dynamic idle behavior
    private static final long IDLE_MOTION_INTERVAL = 15000; // 15 seconds for idle motion cycling
    private long lastIdleExpressionChange = 0;
    private long lastIdleMotionChange = 0;
    private boolean allowIdleExpressionCycling = true;
    private boolean allowIdleMotionCycling = true;
    private final List<Emotion> idleExpressions = new ArrayList<>();
    private final List<Integer> idleMotions = new ArrayList<>();
    private Emotion lastIdleExpression = Emotion.NEUTRAL;
    private Integer lastIdleMotion = null;
    
    // Context tracking
    private final List<String> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 10;
    
    static {
        initializeExpressionMappings();
        initializeMotionMappings();
        initializeEmotionPatterns();
    }
    
    public static EmotionManager getInstance() {
        if (instance == null) {
            instance = new EmotionManager();
        }
        return instance;
    }
    
    public static void releaseInstance() {
        instance = null;
    }
    
    private EmotionManager() {
        initializeIdleExpressions();
        initializeIdleMotions();
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager initialized with " + EXPRESSION_MAPPINGS.size() + " emotion mappings, idle expression cycling, and enhanced idle motion cycling");
        }
    }
    
    /**
     * Initialize available expressions for idle cycling
     * Cycles through neutral, happy, and very happy emotions only
     */
    private void initializeIdleExpressions() {
        // Production mode: Only cycle through positive emotions
        idleExpressions.add(Emotion.NEUTRAL);        // F01 - Default calm state
        // idleExpressions.add(Emotion.HAPPY);          // F02 - Pleasant expression
        idleExpressions.add(Emotion.VERY_HAPPY);     // F05 - Joyful expression
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("Initialized idle expressions: NEUTRAL, VERY_HAPPY");
        }
    }
    
    /**
     * Initialize available motions for idle cycling
     * Based on motion documentation - uses gentle, varied motions for dynamic idle behavior
     */
    private void initializeIdleMotions() {
        // Curated list of gentle motions suitable for idle cycling
        // Based on motion documentation analysis
        idleMotions.add(1);   // Head shake/nod with gentle movements
        idleMotions.add(2);   // Quick head turn with body sway
        idleMotions.add(7);   // Head turn with coordinated arm movements
        idleMotions.add(13);  // Quick head shake with wide eyes and frowning
        idleMotions.add(18);  // Happy, energetic head bob with smiling
        idleMotions.add(20);  // Complex sequence with head turns and expressions
        idleMotions.add(21);  // Variety of head movements with smile and surprised look
        idleMotions.add(23);  // Happy, bouncy head movement with smiling
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("Initialized idle motions: " + idleMotions.size() + " motions for enhanced idle behavior");
        }
    }
    
    /**
     * Initialize expression mappings based on parameter analysis
     * Note: F02 is excluded due to lip sync interference (ParamMouthOpenY=1.0 conflicts with TTS)
     */
    private static void initializeExpressionMappings() {
        // Primary mappings based on expression analysis
        EXPRESSION_MAPPINGS.put(Emotion.NEUTRAL, "F01");        // F01 now neutral
        EXPRESSION_MAPPINGS.put(Emotion.HAPPY, "F01");          // F01 instead of F02 (lip sync compatibility)
        EXPRESSION_MAPPINGS.put(Emotion.VERY_HAPPY, "F05");     // Closed eyes with big smile
        EXPRESSION_MAPPINGS.put(Emotion.SAD, "F04");            // F04 now sad (direct sadness)
        EXPRESSION_MAPPINGS.put(Emotion.ANGRY, "F03");          // Very negative brow values, frown
        EXPRESSION_MAPPINGS.put(Emotion.SURPRISED, "F06");      // Wide eyes, raised brows
        EXPRESSION_MAPPINGS.put(Emotion.EMBARRASSED, "F07");    // Complex expression with "ParamTere"
        EXPRESSION_MAPPINGS.put(Emotion.SUSPICIOUS, "F08");     // F08 for suspicious/disappointed
        EXPRESSION_MAPPINGS.put(Emotion.CONFUSED, "F06");       // F06 for confused/puzzled (wide eyes, raised brows)
        
        // Fallback mappings for graceful degradation (F02 removed due to lip sync interference)
        FALLBACK_MAPPINGS.put(Emotion.NEUTRAL, Arrays.asList("F01", "F08"));
        FALLBACK_MAPPINGS.put(Emotion.HAPPY, Arrays.asList("F01", "F05", "F08"));
        FALLBACK_MAPPINGS.put(Emotion.VERY_HAPPY, Arrays.asList("F05", "F01", "F06"));
        FALLBACK_MAPPINGS.put(Emotion.SAD, Arrays.asList("F04", "F08", "F01"));
        FALLBACK_MAPPINGS.put(Emotion.ANGRY, Arrays.asList("F03", "F04", "F01"));
        FALLBACK_MAPPINGS.put(Emotion.SURPRISED, Arrays.asList("F06", "F05", "F01"));
        FALLBACK_MAPPINGS.put(Emotion.EMBARRASSED, Arrays.asList("F07", "F05", "F01"));
        FALLBACK_MAPPINGS.put(Emotion.SUSPICIOUS, Arrays.asList("F08", "F04", "F03"));
        FALLBACK_MAPPINGS.put(Emotion.CONFUSED, Arrays.asList("F06", "F08", "F01"));
    }
    
    /**
     * Initialize motion mappings for enhanced emotional expression
     * Based on motion analysis and fixes from README.md
     */
    private static void initializeMotionMappings() {
        // Primary motion mappings - fixed based on motion issue analysis
        MOTION_MAPPINGS.put(Emotion.NEUTRAL, Arrays.asList(1, 2, 7, 13));           // Subtle, calm movements
        MOTION_MAPPINGS.put(Emotion.HAPPY, Arrays.asList(18, 23, 21));              // Energetic, positive movements (removed 25 due to motion conflicts)
        MOTION_MAPPINGS.put(Emotion.VERY_HAPPY, Arrays.asList(23, 21, 18));         // Very energetic, excited movements
        MOTION_MAPPINGS.put(Emotion.SAD, Arrays.asList(15, 19, 24));                // Slow, melancholy movements
        MOTION_MAPPINGS.put(Emotion.ANGRY, Arrays.asList(22, 3, 8, 12));            // Dramatic, intense movements (removed 4 due to parameter conflicts)
        MOTION_MAPPINGS.put(Emotion.SURPRISED, Arrays.asList(10, 11, 14));          // Quick, reactive movements
        MOTION_MAPPINGS.put(Emotion.EMBARRASSED, Arrays.asList(22, 5, 11));         // Shy, bashful movements (removed 17 - causes crash)
        MOTION_MAPPINGS.put(Emotion.SUSPICIOUS, Arrays.asList(22, 16, 20, 26));     // Complex, questioning movements
        MOTION_MAPPINGS.put(Emotion.CONFUSED, Arrays.asList(17, 6, 9, 20, 26));     // Head tilts, questioning movements, puzzled expressions (17: shy expression with head tilt)
        
        // Idle motion mappings (subset for background animations)
        IDLE_MOTION_MAPPINGS.put(Emotion.NEUTRAL, Arrays.asList(1, 2, 7));          // Basic calm movements
        IDLE_MOTION_MAPPINGS.put(Emotion.HAPPY, Arrays.asList(18, 23));             // Gentle positive movements
        IDLE_MOTION_MAPPINGS.put(Emotion.VERY_HAPPY, Arrays.asList(21, 18));        // Moderate excitement
        IDLE_MOTION_MAPPINGS.put(Emotion.SAD, Arrays.asList(19, 24));               // Subtle sadness
        IDLE_MOTION_MAPPINGS.put(Emotion.ANGRY, Arrays.asList(22, 3));              // Controlled intensity
        IDLE_MOTION_MAPPINGS.put(Emotion.SURPRISED, Arrays.asList(11, 14));         // Mild alertness
        IDLE_MOTION_MAPPINGS.put(Emotion.EMBARRASSED, Arrays.asList(22, 5));        // Gentle shyness (removed 17 - causes crash)
        IDLE_MOTION_MAPPINGS.put(Emotion.SUSPICIOUS, Arrays.asList(22, 16));        // Mild wariness
        IDLE_MOTION_MAPPINGS.put(Emotion.CONFUSED, Arrays.asList(6, 9));            // Head tilts and questioning movements
    }
    
    /**
     * Initialize emotion detection patterns (speech-based only, no emojis)
     */
    private static void initializeEmotionPatterns() {
        // Happy patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.HAPPY, Arrays.asList(
            Pattern.compile("\\b(happy|joy|glad|pleased|good|great|awesome|wonderful|nice|love|like)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(haha|hehe|lol|laugh|laughing)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(thank you|thanks|appreciate|grateful)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Very happy patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.VERY_HAPPY, Arrays.asList(
            Pattern.compile("\\b(amazing|fantastic|incredible|excellent|perfect|brilliant|outstanding)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(excited|thrilled|ecstatic|overjoyed|delighted)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(wonderful|marvelous|spectacular|phenomenal)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Sad patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.SAD, Arrays.asList(
            Pattern.compile("\\b(sad|unhappy|depressed|down|blue|upset|disappointed|hurt)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(cry|crying|tears|sob|weep)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(sorry|apologize|regret|mistake)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(sadness|feeling.*sad|wave.*sadness|melancholy|sorrow)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(capable.*feeling.*sadness|suppose.*feel.*sad)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(heaviness.*heart|heavy.*heart|gentle.*heaviness|cloud.*drifting)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(vulnerable.*emotion|difficult.*feel.*sad|brings.*sorrow|little.*sorrow)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Angry patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.ANGRY, Arrays.asList(
            Pattern.compile("\\b(angry|mad|furious|annoyed|irritated|frustrated|pissed)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(hate|stupid|dumb|idiot|ridiculous|terrible|awful)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(outrageous|unacceptable|disgusting)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(can't believe.*pushing|trying to provoke|unnecessary conflicts)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(pushing.*this|provoke me)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(rush.*frustration|storm.*gathering|when.*upset|moments.*anger)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(made.*angry|feel.*angry|little.*rush|gathering.*inside)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Surprised patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.SURPRISED, Arrays.asList(
            Pattern.compile("\\b(wow|whoa|amazing|incredible|unbelievable|shocking)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(surprised|shocked|stunned|astonished|bewildered)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(really\\?|seriously\\?|no way|what\\?!)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(caught.*off.*guard|caught.*guard|off.*guard|unexpected)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Embarrassed patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.EMBARRASSED, Arrays.asList(
            Pattern.compile("\\b(embarrassed|shy|bashful|awkward|flustered|blushing)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(oops|whoops|my bad|awkward|nervous)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(uncomfortable|self-conscious)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(cheeks.*warm|cheeks.*turn|fidget.*bit|shrink.*myself)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(made.*shy|bit.*shy|humility.*gentle)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(tender.*appreciation|brush.*off|laugh.*softly)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Suspicious patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.SUSPICIOUS, Arrays.asList(
            Pattern.compile("\\b(suspicious|doubt|questionable|fishy|weird|strange)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(really\\?|sure about that\\?|are you certain\\?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(skeptical|doubtful|uncertain)\\b", Pattern.CASE_INSENSITIVE)
        ));
        
        // Confused patterns - speech-based detection
        EMOTION_PATTERNS.put(Emotion.CONFUSED, Arrays.asList(
            Pattern.compile("\\b(confused|puzzled|perplexed|bewildered|baffled|mystified)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(don't understand|can't figure|makes no sense|what do you mean)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(huh\\?|what\\?|how\\?|why\\?|unclear|lost|mixed up)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(not sure what|don't get it|doesn't make sense|hard to follow)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(confused.*me|confusing|little.*tricky|tricky.*understand)\\b", Pattern.CASE_INSENSITIVE)
        ));
    }
    
    /**
     * Analyze text for emotional content and update current emotion
     */
    public void analyzeText(String text, boolean isUserInput) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Add to conversation history
        addToHistory(text);
        
        // Detect emotion from text
        Emotion detectedEmotion = detectEmotion(text);
        
        // Apply emotion with context consideration
        if (detectedEmotion != Emotion.NEUTRAL) {
            // For AI responses, set as pending emotion to be triggered during TTS
            if (!isUserInput) {
                setPendingEmotion(detectedEmotion, text);
            } else {
                // For user input, apply immediately
                applyEmotion(detectedEmotion, text, isUserInput);
            }
        }
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager analyzed: \"" + text.substring(0, Math.min(50, text.length())) + 
                             "\" -> " + detectedEmotion + " (current: " + currentEmotion + 
                             (isUserInput ? ", applied immediately" : ", set as pending") + ")");
        }
    }
    
    /**
     * Detect emotion from text using advanced pattern matching and context analysis
     */
    private Emotion detectEmotion(String text) {
        String lowerText = text.toLowerCase();
        
        // Score each emotion based on pattern matches with context weighting
        Map<Emotion, Float> emotionScores = new HashMap<>();
        
        for (Map.Entry<Emotion, List<Pattern>> entry : EMOTION_PATTERNS.entrySet()) {
            Emotion emotion = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            float score = 0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(lowerText).find()) {
                    // Base score for pattern match
                    float patternScore = 1.0f;
                    
                    // Apply context weighting
                    patternScore = applyContextWeighting(lowerText, emotion, patternScore);
                    
                    score += patternScore;
                }
            }
            
            if (score > 0) {
                emotionScores.put(emotion, score);
            }
        }
        
        // Apply overall sentiment analysis
        emotionScores = applySentimentAnalysis(lowerText, emotionScores);
        
        // Return emotion with highest score
        Emotion bestEmotion = Emotion.NEUTRAL;
        float bestScore = 0;
        
        for (Map.Entry<Emotion, Float> entry : emotionScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestEmotion = entry.getKey();
            }
        }
        
        return bestEmotion;
    }
    
    /**
     * Apply context weighting to emotion scores based on surrounding text
     */
    private float applyContextWeighting(String text, Emotion emotion, float baseScore) {
        // Reduce score for conditional/hypothetical statements
        if (text.contains("might feel") || text.contains("suppose i") || text.contains("if i") || 
            text.contains("even if") || text.contains("don't really")) {
            baseScore *= 0.3f; // Significantly reduce weight for hypothetical emotions
        }
        
        // Reduce score for negated emotions
        if (text.contains("don't get") || text.contains("don't experience") || 
            text.contains("can't feel") || text.contains("not really")) {
            baseScore *= 0.2f; // Very low weight for negated emotions
        }
        
        // Increase score for direct emotional expressions
        if (text.contains("i feel") || text.contains("i am") || text.contains("i'm")) {
            baseScore *= 1.5f; // Boost direct emotional statements
        }
        
        // Context-specific adjustments
        switch (emotion) {
            case SURPRISED:
                // "surprised or taken aback" in supportive context should be neutral/embarrassed
                if (text.contains("it's okay") || text.contains("i'm here") || text.contains("listen and understand")) {
                    baseScore *= 0.1f; // Very low weight - this is supportive, not surprised
                }
                break;
                
            case EMBARRASSED:
                // Boost embarrassed when AI acknowledges limitations
                if (text.contains("don't really") || text.contains("suppose") || text.contains("even if")) {
                    baseScore *= 1.3f;
                }
                break;
                
            case NEUTRAL:
                // Boost neutral for supportive, understanding responses
                if (text.contains("i'm here") || text.contains("listen") || text.contains("understand") || 
                    text.contains("it's okay") || text.contains("all ears")) {
                    baseScore *= 1.5f;
                }
                break;
        }
        
        return baseScore;
    }
    
    /**
     * Apply overall sentiment analysis to adjust emotion scores
     */
    private Map<Emotion, Float> applySentimentAnalysis(String text, Map<Emotion, Float> scores) {
        // Count supportive/positive indicators
        int supportiveCount = 0;
        String[] supportiveWords = {"okay", "here", "listen", "understand", "support", "help", "care"};
        for (String word : supportiveWords) {
            if (text.contains(word)) supportiveCount++;
        }
        
        // Count negative indicators
        int negativeCount = 0;
        String[] negativeWords = {"don't", "can't", "won't", "not", "never"};
        for (String word : negativeWords) {
            if (text.contains(word)) negativeCount++;
        }
        
        // Check if there are strong emotional expressions that should not be suppressed
        boolean hasStrongEmotion = false;
        String[] strongEmotionalPhrases = {"feel really", "makes me feel", "i feel", "feeling", "makes me"};
        for (String phrase : strongEmotionalPhrases) {
            if (text.contains(phrase)) {
                hasStrongEmotion = true;
                break;
            }
        }
        
        // Only boost neutral if supportive AND no strong emotions are expressed
        if (supportiveCount >= 3 && !hasStrongEmotion) {
            scores.put(Emotion.NEUTRAL, scores.getOrDefault(Emotion.NEUTRAL, 0f) + 2.0f);
            // Reduce other emotions when being supportive (but only if no strong emotions)
            for (Emotion emotion : scores.keySet()) {
                if (emotion != Emotion.NEUTRAL && emotion != Emotion.HAPPY) {
                    scores.put(emotion, scores.get(emotion) * 0.5f);
                }
            }
        }
        
        // If response has many negations but is supportive, lean toward embarrassed (only if no strong emotions)
        if (negativeCount >= 2 && supportiveCount >= 2 && !hasStrongEmotion) {
            scores.put(Emotion.EMBARRASSED, scores.getOrDefault(Emotion.EMBARRASSED, 0f) + 1.5f);
        }
        
        return scores;
    }
    
    /**
     * Apply detected emotion with timing and context considerations
     */
    private void applyEmotion(Emotion emotion, String trigger, boolean isUserInput) {
        long currentTime = System.currentTimeMillis();
        
        // Respect minimum interval between emotion changes
        if (currentTime - lastEmotionChange < MIN_EMOTION_INTERVAL) {
            return;
        }
        
        // Don't change to same emotion unless it's been a while
        if (emotion == currentEmotion && currentTime - lastEmotionChange < EMOTION_DURATION / 2) {
            return;
        }
        
        // Apply the emotion
        setEmotion(emotion, trigger);
    }
    
    /**
     * Set current emotion and trigger appropriate expression
     */
    public void setEmotion(Emotion emotion, String trigger) {
        if (emotion == null) emotion = Emotion.NEUTRAL;
        
        currentEmotion = emotion;
        lastEmotionChange = System.currentTimeMillis();
        lastTrigger = trigger != null ? trigger : "";
        
        // Apply expression and motion to Live2D model
        applyExpressionToModel(emotion);
        applyMotionToModel(emotion);
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager set emotion: " + emotion + " (trigger: \"" + 
                             (trigger != null ? trigger.substring(0, Math.min(30, trigger.length())) : "none") + "\")");
        }
    }
    
    /**
     * Apply emotion expression to Live2D model
     */
    private void applyExpressionToModel(Emotion emotion) {
        WaifuLive2DManager manager = WaifuLive2DManager.getInstance();
        WaifuModel model = manager.getWaifuModel();
        
        if (model == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No model available for expression");
            }
            return;
        }
        
        // Get expression name for emotion
        String expressionName = getExpressionForEmotion(emotion);
        
        if (expressionName != null) {
            model.setExpression(expressionName);
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager applied expression: " + expressionName + " for emotion: " + emotion);
            }
        } else {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No expression available for emotion: " + emotion);
            }
        }
    }
    
    /**
     * Apply emotion motion to Live2D model
     * Uses FORCE priority to ensure emotion motions can interrupt other motions
     */
    private void applyMotionToModel(Emotion emotion) {
        WaifuLive2DManager manager = WaifuLive2DManager.getInstance();
        WaifuModel model = manager.getWaifuModel();
        
        if (model == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No model available for motion");
            }
            return;
        }
        
        // Get motion index for emotion
        Integer motionIndex = getMotionForEmotion(emotion);
        
        if (motionIndex != null) {
            // Apply motion with high priority to ensure emotion motions can interrupt other motions
            int priority = WaifuDefine.Priority.FORCE.getPriority();
            
            // Start motion with callback for debugging
            int motionResult = model.startMotion(
                WaifuDefine.MotionGroup.IDLE.getId(), 
                motionIndex, 
                priority,
                new IFinishedMotionCallback() {
                    @Override
                    public void execute(ACubismMotion motion) {
                        if (WaifuDefine.DEBUG_LOG_ENABLE) {
                            WaifuPal.printLog("MOTION_DEBUG: Motion " + motionIndex + " for emotion " + emotion + " completed");
                        }
                    }
                }
            );
            
            if (motionResult >= 0) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("MOTION_DEBUG: ✓ PLAYING Motion ID " + motionIndex + " for emotion " + emotion + " with FORCE priority (result: " + motionResult + ")");
                    WaifuPal.printLog("MOTION_MAPPING: " + emotion + " -> Motion " + motionIndex + " (from available: " + MOTION_MAPPINGS.get(emotion) + ")");
                }
            } else {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("MOTION_DEBUG: ✗ FAILED Motion ID " + motionIndex + " for emotion " + emotion + " (result: " + motionResult + ")");
                }
                
                // Try fallback motion
                Integer fallbackMotion = getFallbackMotionForEmotion(emotion);
                if (fallbackMotion != null && !fallbackMotion.equals(motionIndex)) {
                    int fallbackResult = model.startMotion(
                        WaifuDefine.MotionGroup.IDLE.getId(), 
                        fallbackMotion, 
                        priority,
                        null
                    );
                    
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("MOTION_DEBUG: Fallback motion " + fallbackMotion + " for emotion " + emotion + 
                                         (fallbackResult >= 0 ? " started successfully (result: " + fallbackResult + ")" : " also failed (result: " + fallbackResult + ")"));
                    }
                }
            }
        } else {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No motion available for emotion: " + emotion);
            }
        }
    }
    
    /**
     * Get motion index for emotion with random selection
     */
    private Integer getMotionForEmotion(Emotion emotion) {
        List<Integer> motions = MOTION_MAPPINGS.get(emotion);
        if (motions != null && !motions.isEmpty()) {
            // Select random motion from available options
            int randomIndex = (int) (Math.random() * motions.size());
            return motions.get(randomIndex);
        }
        return null;
    }
    
    /**
     * Get fallback motion for emotion (first motion in list)
     */
    private Integer getFallbackMotionForEmotion(Emotion emotion) {
        List<Integer> motions = MOTION_MAPPINGS.get(emotion);
        if (motions != null && !motions.isEmpty()) {
            return motions.get(0); // Return first motion as fallback
        }
        return null;
    }
    
    /**
     * Get idle motion for emotion (for background animations)
     */
    public Integer getIdleMotionForEmotion(Emotion emotion) {
        List<Integer> idleMotions = IDLE_MOTION_MAPPINGS.get(emotion);
        if (idleMotions != null && !idleMotions.isEmpty()) {
            // Select random idle motion from available options
            int randomIndex = (int) (Math.random() * idleMotions.size());
            return idleMotions.get(randomIndex);
        }
        return null;
    }
    
    /**
     * Get expression name for emotion with fallback support
     */
    private String getExpressionForEmotion(Emotion emotion) {
        // Try primary mapping first
        String expression = EXPRESSION_MAPPINGS.get(emotion);
        if (expression != null) {
            return expression;
        }
        
        // Try fallback mappings
        List<String> fallbacks = FALLBACK_MAPPINGS.get(emotion);
        if (fallbacks != null && !fallbacks.isEmpty()) {
            return fallbacks.get(0); // Return first fallback
        }
        
        // Ultimate fallback
        return "F08"; // Neutral expression
    }
    
    /**
     * Add text to conversation history
     */
    private void addToHistory(String text) {
        conversationHistory.add(text);
        
        // Maintain history size limit
        while (conversationHistory.size() > MAX_HISTORY_SIZE) {
            conversationHistory.remove(0);
        }
    }
    
    /**
     * Update emotion state (called periodically)
     * Now includes idle expression cycling functionality
     */
    public void update(float deltaTimeSeconds) {
        long currentTime = System.currentTimeMillis();
        
        // Check if current emotion should decay back to neutral
        if (currentEmotion != Emotion.NEUTRAL && 
            currentTime - lastEmotionChange > EMOTION_DURATION) {
            
            setEmotion(Emotion.NEUTRAL, "emotion_decay");
        }
        
        // Handle idle expression cycling when in neutral state
        handleIdleExpressionCycling(currentTime);
        
        // Handle enhanced idle motion cycling
        handleIdleMotionCycling(currentTime);
    }
    
    /**
     * Handle automatic expression cycling during idle periods
     * Integrated from IdleBehaviorManager - only cycles when in neutral emotional state and not during TTS
     */
    private void handleIdleExpressionCycling(long currentTime) {
        if (!allowIdleExpressionCycling) {
            return;
        }
        
        // Suppress idle animations during TTS playback
        if (isTTSSpeaking) {
            return;
        }
        
        // Only cycle expressions when in neutral emotional state
        if (currentEmotion != Emotion.NEUTRAL) {
            return;
        }
        
        // Check if enough time has passed for idle expression change
        if (currentTime - lastIdleExpressionChange >= IDLE_EXPRESSION_INTERVAL) {
            Emotion newIdleEmotion = selectRandomIdleEmotion();
            
            if (newIdleEmotion != null && newIdleEmotion != lastIdleExpression) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("EmotionManager: Cycling idle expression to: " + newIdleEmotion);
                }
                
                // Apply the idle expression
                applyExpressionToModel(newIdleEmotion);
                lastIdleExpression = newIdleEmotion;
                lastIdleExpressionChange = currentTime;
            }
        }
    }
    
    /**
     * Select a random emotion for idle expression cycling
     */
    private Emotion selectRandomIdleEmotion() {
        if (idleExpressions.isEmpty()) {
            return Emotion.NEUTRAL;
        }
        
        // Filter out the last used expression to avoid repetition
        List<Emotion> availableEmotions = new ArrayList<>();
        for (Emotion emotion : idleExpressions) {
            if (emotion != lastIdleExpression) {
                availableEmotions.add(emotion);
            }
        }
        
        if (availableEmotions.isEmpty()) {
            // If all emotions are filtered out, use any from the list
            availableEmotions = new ArrayList<>(idleExpressions);
        }
        
        // Select random emotion
        int randomIndex = (int) (Math.random() * availableEmotions.size());
        return availableEmotions.get(randomIndex);
    }
    
    /**
     * Handle enhanced idle motion cycling during idle periods
     * Provides more dynamic idle behavior with varied motions
     */
    private void handleIdleMotionCycling(long currentTime) {
        if (!allowIdleMotionCycling) {
            return;
        }
        
        // Suppress idle motion cycling during TTS playback
        if (isTTSSpeaking) {
            return;
        }
        
        // Only cycle motions when in neutral emotional state
        if (currentEmotion != Emotion.NEUTRAL) {
            return;
        }
        
        // Check if enough time has passed for idle motion change
        if (currentTime - lastIdleMotionChange >= IDLE_MOTION_INTERVAL) {
            Integer newIdleMotion = selectRandomIdleMotion();
            
            if (newIdleMotion != null && !newIdleMotion.equals(lastIdleMotion)) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("EmotionManager: Cycling idle motion to: " + newIdleMotion);
                }
                
                // Apply the idle motion
                applyIdleMotionToModel(newIdleMotion);
                lastIdleMotion = newIdleMotion;
                lastIdleMotionChange = currentTime;
            }
        }
    }
    
    /**
     * Select a random motion for idle motion cycling
     */
    private Integer selectRandomIdleMotion() {
        if (idleMotions.isEmpty()) {
            return 1; // Default to motion 1
        }
        
        // Filter out the last used motion to avoid repetition
        List<Integer> availableMotions = new ArrayList<>();
        for (Integer motion : idleMotions) {
            if (!motion.equals(lastIdleMotion)) {
                availableMotions.add(motion);
            }
        }
        
        if (availableMotions.isEmpty()) {
            // If all motions are filtered out, use any from the list
            availableMotions = new ArrayList<>(idleMotions);
        }
        
        // Select random motion
        int randomIndex = (int) (Math.random() * availableMotions.size());
        return availableMotions.get(randomIndex);
    }
    
    /**
     * Apply idle motion to Live2D model with appropriate priority
     */
    private void applyIdleMotionToModel(Integer motionIndex) {
        WaifuLive2DManager manager = WaifuLive2DManager.getInstance();
        WaifuModel model = manager.getWaifuModel();
        
        if (model == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No model available for idle motion");
            }
            return;
        }
        
        // Apply idle motion with normal priority (can be interrupted by user interactions)
        int priority = WaifuDefine.Priority.NORMAL.getPriority();
        
        int motionResult = model.startMotion(
            WaifuDefine.MotionGroup.IDLE.getId(), 
            motionIndex, 
            priority,
            new IFinishedMotionCallback() {
                @Override
                public void execute(ACubismMotion motion) {
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("IDLE_MOTION_DEBUG: Enhanced idle motion " + motionIndex + " completed");
                    }
                }
            }
        );
        
        if (motionResult >= 0) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("IDLE_MOTION_DEBUG: ✓ PLAYING Enhanced Idle Motion ID " + motionIndex + " (result: " + motionResult + ")");
            }
        } else {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("IDLE_MOTION_DEBUG: ✗ FAILED Enhanced Idle Motion ID " + motionIndex + " (result: " + motionResult + ")");
            }
        }
    }
    
    /**
     * Reset interaction timer - call when user interacts with the model
     * Integrated from IdleBehaviorManager functionality
     */
    public void onUserInteraction() {
        // Reset idle expression timer to prevent immediate cycling after interaction
        lastIdleExpressionChange = System.currentTimeMillis();
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager: User interaction detected - resetting idle expression timer");
        }
    }
    
    /**
     * Enable or disable idle expression cycling
     */
    public void setIdleExpressionCyclingEnabled(boolean enabled) {
        this.allowIdleExpressionCycling = enabled;
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager: Idle expression cycling " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Get current emotion
     */
    public Emotion getCurrentEmotion() {
        return currentEmotion;
    }
    
    /**
     * Get emotion intensity
     */
    public float getEmotionIntensity() {
        return emotionIntensity;
    }
    
    /**
     * Get last emotion trigger
     */
    public String getLastTrigger() {
        return lastTrigger;
    }
    
    /**
     * Force set emotion (for testing or special cases)
     */
    public void forceEmotion(Emotion emotion) {
        setEmotion(emotion, "forced");
    }
    
    /**
     * Reset to neutral emotion
     */
    public void resetToNeutral() {
        setEmotion(Emotion.NEUTRAL, "reset");
    }
    
    /**
     * Get available emotions
     */
    public static Emotion[] getAvailableEmotions() {
        return Emotion.values();
    }
    
    /**
     * Get expression mapping for debugging
     */
    public static Map<Emotion, String> getExpressionMappings() {
        return new HashMap<>(EXPRESSION_MAPPINGS);
    }
    
    // ========== TTS STATE MANAGEMENT ==========
    
    /**
     * Called when TTS starts speaking
     * Triggers emotion animation during speech
     */
    public void onTTSStarted() {
        isTTSSpeaking = true;
        
        // If we have a pending emotion, apply it now during TTS
        if (pendingEmotion != null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: TTS started - applying pending emotion: " + pendingEmotion);
            }
            
            // Apply the pending emotion during TTS
            applyEmotionDuringTTS(pendingEmotion, pendingTrigger);
            
            // Clear pending emotion
            pendingEmotion = null;
            pendingTrigger = "";
        } else if (currentEmotion != Emotion.NEUTRAL) {
            // If we have a current emotion, trigger its animation during TTS
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: TTS started - triggering current emotion animation: " + currentEmotion);
            }
            
            applyEmotionDuringTTS(currentEmotion, "tts_animation");
        }
    }
    
    /**
     * Called when TTS finishes speaking
     * Returns to idle animation
     */
    public void onTTSFinished() {
        isTTSSpeaking = false;
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager: TTS finished - returning to idle animation");
        }
        
        // Return to idle animation
        returnToIdleAnimation();
    }
    
    /**
     * Set pending emotion to be applied when TTS starts
     * This allows emotion detection to happen before TTS starts
     */
    public void setPendingEmotion(Emotion emotion, String trigger) {
        if (emotion == null) return;
        
        pendingEmotion = emotion;
        pendingTrigger = trigger != null ? trigger : "";
        
        // Update current emotion state for consistency
        currentEmotion = emotion;
        lastEmotionChange = System.currentTimeMillis();
        lastTrigger = pendingTrigger;
        
        // Apply expression immediately (but not motion until TTS starts)
        applyExpressionToModel(emotion);
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager: Set pending emotion: " + emotion + " (trigger: \"" + 
                             (trigger != null ? trigger.substring(0, Math.min(30, trigger.length())) : "none") + "\")");
        }
    }
    
    /**
     * Apply emotion animation during TTS speaking
     */
    private void applyEmotionDuringTTS(Emotion emotion, String trigger) {
        // Apply expression (if not already applied)
        applyExpressionToModel(emotion);
        
        // Apply motion animation during TTS
        applyMotionToModel(emotion);
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("EmotionManager: Applied emotion animation during TTS: " + emotion);
        }
    }
    
    /**
     * Return to idle animation after TTS finishes
     */
    private void returnToIdleAnimation() {
        WaifuLive2DManager manager = WaifuLive2DManager.getInstance();
        WaifuModel model = manager.getWaifuModel();
        
        if (model == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: No model available for idle animation");
            }
            return;
        }
        
        // Get idle motion for current emotion
        Integer idleMotion = getIdleMotionForEmotion(currentEmotion);
        
        if (idleMotion != null) {
            // Apply idle motion with normal priority
            int priority = WaifuDefine.Priority.NORMAL.getPriority();
            
            int motionResult = model.startMotion(
                WaifuDefine.MotionGroup.IDLE.getId(), 
                idleMotion, 
                priority,
                new IFinishedMotionCallback() {
                    @Override
                    public void execute(ACubismMotion motion) {
                        if (WaifuDefine.DEBUG_LOG_ENABLE) {
                            WaifuPal.printLog("EmotionManager: Idle motion " + idleMotion + " completed");
                        }
                    }
                }
            );
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("MOTION_DEBUG: ✓ IDLE Motion ID " + idleMotion + " for emotion " + currentEmotion + 
                                 (motionResult >= 0 ? " (success)" : " (failed)"));
                WaifuPal.printLog("IDLE_MOTION_MAPPING: " + currentEmotion + " -> Idle Motion " + idleMotion + " (from available: " + IDLE_MOTION_MAPPINGS.get(currentEmotion) + ")");
            }
        } else {
            // Fallback to basic idle motion
            int basicIdleMotion = 1; // Motion 1 is typically a basic idle
            int priority = WaifuDefine.Priority.NORMAL.getPriority();
            
            int motionResult = model.startMotion(
                WaifuDefine.MotionGroup.IDLE.getId(), 
                basicIdleMotion, 
                priority,
                null
            );
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("EmotionManager: Started fallback idle motion " + basicIdleMotion + 
                                 (motionResult >= 0 ? " (success)" : " (failed)"));
            }
        }
    }
    
    /**
     * Check if TTS is currently speaking
     */
    public boolean isTTSSpeaking() {
        return isTTSSpeaking;
    }
}
