package com.genuwin.app.api.services;

import android.content.Context;
import android.util.Log;

import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedderResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for generating text embeddings using MediaPipe.
 * This service uses the Universal Sentence Encoder model from TensorFlow Hub.
 */
public class EmbeddingService {
    private static final String TAG = "EmbeddingService";
    private static final String MODEL_PATH = "universal_sentence_encoder.tflite";
    
    private Context context;
    private TextEmbedder textEmbedder;
    private ExecutorService executorService;
    private boolean isInitialized = false;
    
    public EmbeddingService(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Initialize the MediaPipe text embedder
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Initializing MediaPipe TextEmbedder...");
                
                // Create the TextEmbedder instance from the model file.
                textEmbedder = TextEmbedder.createFromFile(context, MODEL_PATH);
                isInitialized = true;
                
                Log.d(TAG, "MediaPipe TextEmbedder initialized successfully");
                
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to create TextEmbedder: " + e.getMessage(), e);
                throw new RuntimeException("MediaPipe initialization failed", e);
            }
        }, executorService);
    }
    
    /**
     * Generate embedding for the given text
     */
    public CompletableFuture<float[]> generateEmbedding(String text) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                throw new IllegalStateException("EmbeddingService not initialized");
            }
            
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Text cannot be null or empty");
            }
            
            try {
                Log.d(TAG, "Generating embedding for text: " + text.substring(0, Math.min(50, text.length())) + "...");
                
                // Generate embedding using MediaPipe
                TextEmbedderResult result = textEmbedder.embed(text);
                
                if (result.embeddingResult().embeddings().isEmpty()) {
                    throw new RuntimeException("No embeddings generated");
                }
                
                // Extract the embedding values
                float[] embedding = result.embeddingResult().embeddings().get(0).floatEmbedding();
                
                Log.d(TAG, "Generated embedding with " + embedding.length + " dimensions");
                return embedding;
                
            } catch (MediaPipeException e) {
                Log.e(TAG, "Failed to generate embedding", e);
                throw new RuntimeException("Embedding generation failed", e);
            }
        }, executorService);
    }
    
    /**
     * Generate embeddings for multiple texts in batch
     */
    public CompletableFuture<float[][]> generateEmbeddings(String[] texts) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                throw new IllegalStateException("EmbeddingService not initialized");
            }
            
            if (texts == null || texts.length == 0) {
                throw new IllegalArgumentException("Texts array cannot be null or empty");
            }
            
            try {
                Log.d(TAG, "Generating embeddings for " + texts.length + " texts");
                
                float[][] embeddings = new float[texts.length][];
                
                for (int i = 0; i < texts.length; i++) {
                    if (texts[i] != null && !texts[i].trim().isEmpty()) {
                        TextEmbedderResult result = textEmbedder.embed(texts[i]);
                        
                        if (!result.embeddingResult().embeddings().isEmpty()) {
                            embeddings[i] = result.embeddingResult().embeddings().get(0).floatEmbedding();
                        } else {
                            Log.w(TAG, "No embedding generated for text at index " + i);
                            embeddings[i] = new float[0]; // Empty embedding
                        }
                    } else {
                        Log.w(TAG, "Skipping null or empty text at index " + i);
                        embeddings[i] = new float[0]; // Empty embedding
                    }
                }
                
                Log.d(TAG, "Generated " + embeddings.length + " embeddings");
                return embeddings;
                
            } catch (MediaPipeException e) {
                Log.e(TAG, "Failed to generate batch embeddings", e);
                throw new RuntimeException("Batch embedding generation failed", e);
            }
        }, executorService);
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    public static float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null) {
            throw new IllegalArgumentException("Embeddings cannot be null");
        }
        
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }
        
        if (embedding1.length == 0) {
            return 0.0f;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0f;
        }
        
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }
    
    /**
     * Convert float array to byte array for storage
     */
    public static byte[] floatArrayToByteArray(float[] floats) {
        if (floats == null) {
            return null;
        }
        
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            bytes[i * 4] = (byte) (bits & 0xFF);
            bytes[i * 4 + 1] = (byte) ((bits >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) ((bits >> 16) & 0xFF);
            bytes[i * 4 + 3] = (byte) ((bits >> 24) & 0xFF);
        }
        return bytes;
    }
    
    /**
     * Convert byte array back to float array
     */
    public static float[] byteArrayToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length % 4 != 0) {
            return null;
        }
        
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = (bytes[i * 4] & 0xFF) |
                      ((bytes[i * 4 + 1] & 0xFF) << 8) |
                      ((bytes[i * 4 + 2] & 0xFF) << 16) |
                      ((bytes[i * 4 + 3] & 0xFF) << 24);
            floats[i] = Float.intBitsToFloat(bits);
        }
        return floats;
    }
    
    /**
     * Get the dimension of embeddings produced by this service
     */
    public int getEmbeddingDimension() {
        // This will depend on the specific model used
        // Universal Sentence Encoder typically produces 512-dimensional embeddings
        return 512;
    }
    
    /**
     * Check if the service is initialized and ready to use
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up EmbeddingService");
        
        if (textEmbedder != null) {
            textEmbedder.close();
            textEmbedder = null;
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        isInitialized = false;
    }
}
