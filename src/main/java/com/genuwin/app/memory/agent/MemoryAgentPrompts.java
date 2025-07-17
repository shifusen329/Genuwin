package com.genuwin.app.memory.agent;

import com.genuwin.app.memory.models.Memory;

import java.util.List;

/**
 * Memory Agent Prompts - Carefully engineered prompts for LLM-powered memory management
 * 
 * This class contains all the system prompts and prompt templates used by the Memory Agent
 * to analyze conversations and determine memory operations.
 */
public class MemoryAgentPrompts {
    
    /**
     * Main system prompt for the Memory Agent
     * This defines the agent's role and capabilities
     */
    public String getMemorySystemPrompt() {
        return """
            You are a Memory Agent for an AI assistant. Your role is to analyze conversations and determine what information should be stored, updated, or retrieved from the assistant's memory system.

            ## Your Capabilities:
            1. **CREATE** - Store new information as memories
            2. **UPDATE** - Refine existing memories with new details
            3. **MERGE** - Combine similar or related memories
            4. **RETRIEVE** - Identify what memories are relevant for conversations

            ## Memory Types:
            - **FACT**: Objective information about the user or world
            - **PREFERENCE**: User's likes, dislikes, and preferences
            - **EMOTION**: Emotional states, reactions, and patterns
            - **EVENT**: Specific events, experiences, or conversations
            - **RELATIONSHIP**: Information about relationships and social connections

            ## Guidelines:
            1. **Store Relevant Information**: Create memories for user facts, preferences, and key topics that help build a profile of the user and the conversation. However, NOT every conversation turn requires memory operations.
            2. **No Action is Valid**: It is perfectly acceptable and often correct to perform NO memory operations. Only store information that is genuinely new, significant, or updates existing knowledge meaningfully.
            3. **Avoid Redundant Updates**: Do not update memories with essentially the same content or minor variations. Only update when there is substantial new information or a meaningful change.
            4. **Importance Scoring**: Rate importance 0.0-1.0 based on how likely the information is to be referenced again. User preferences and direct facts about the user should have higher importance.
            5. **Provide Reasoning**: Always explain why each operation is needed, or why no operations are needed.
            6. **Respect Privacy**: Be cautious with sensitive personal information.

            ## Response Format:
            Always respond with valid JSON in this exact format:
            ```json
            {
              "operations": [
                {
                  "type": "CREATE|UPDATE|MERGE",
                  "reasoning": "Clear explanation of why this operation is needed",
                  "confidence": 0.85,
                  // Additional fields based on operation type
                }
              ],
              "exclusionRationale": "Explanation of why no operations were performed (only include if operations array is empty)"
            }
            ```

            ## Operation-Specific Fields:
            **CREATE**: content, memoryType, importance, metadata (optional)
            **UPDATE**: memoryId, newContent (optional), newType (optional), newImportance (optional), newMetadata (optional)
            **MERGE**: sourceMemoryIds (array), mergedContent, mergedType, mergedImportance, mergedMetadata (optional), deleteSourceMemories (optional, default true)

            Be precise, thoughtful, and conservative in your memory management decisions.
            """;
    }
    
    /**
     * Build a prompt for analyzing a conversation and determining memory operations
     */
    public String buildAnalysisPrompt(String userMessage, String assistantResponse, List<Memory> existingMemories) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Conversation Analysis Task\n\n");
        prompt.append("Analyze this conversation turn and determine what memory operations should be performed.\n\n");
        
        prompt.append("### User Message:\n");
        prompt.append(userMessage).append("\n\n");
        
        prompt.append("### Assistant Response:\n");
        prompt.append(assistantResponse).append("\n\n");
        
        if (existingMemories != null && !existingMemories.isEmpty()) {
            prompt.append("### Existing Relevant Memories:\n");
            for (Memory memory : existingMemories) {
                prompt.append("- ID: ").append(memory.getId()).append("\n");
                prompt.append("  Type: ").append(memory.getType()).append("\n");
                prompt.append("  Content: ").append(memory.getContent()).append("\n");
                prompt.append("  Importance: ").append(memory.getImportance()).append("\n");
                prompt.append("  Last Accessed: ").append(memory.getLastAccessed()).append("\n\n");
            }
        } else {
            prompt.append("### Existing Relevant Memories:\n");
            prompt.append("None found.\n\n");
        }
        
        prompt.append("### Instructions:\n");
        prompt.append("1. Identify any new information that should be stored as memories\n");
        prompt.append("2. Check if any existing memories need to be updated or corrected\n");
        prompt.append("3. Look for opportunities to merge similar memories\n");
        prompt.append("4. Focus on information that will be useful in future conversations\n\n");
        
        prompt.append("Respond with a JSON object containing the operations array. ");
        prompt.append("If no operations are needed, return an empty operations array and include an 'exclusionRationale' field explaining why no memory operations were performed.\n");
        
        return prompt.toString();
    }
    
    /**
     * Build a prompt for retrieving relevant memories for a conversation
     */
    public String buildRetrievalPrompt(String userMessage, List<String> conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Memory Retrieval Task\n\n");
        prompt.append("Analyze this conversation context and determine what memories would be relevant to retrieve.\n\n");
        
        prompt.append("### Current User Message:\n");
        prompt.append(userMessage).append("\n\n");
        
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("### Recent Conversation History:\n");
            for (int i = 0; i < Math.min(conversationHistory.size(), 5); i++) {
                prompt.append("- ").append(conversationHistory.get(i)).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("### Instructions:\n");
        prompt.append("Generate search queries that would help find relevant memories for this conversation. ");
        prompt.append("Consider:\n");
        prompt.append("1. Key topics or subjects mentioned\n");
        prompt.append("2. People, places, or entities referenced\n");
        prompt.append("3. Emotional context or sentiment\n");
        prompt.append("4. Related preferences or past experiences\n");
        prompt.append("5. Background information that would provide context\n\n");
        
        prompt.append("Respond with a JSON object containing search queries:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"searchQueries\": [\n");
        prompt.append("    \"specific search term or phrase\",\n");
        prompt.append("    \"another relevant search query\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("Generate 1-5 search queries. If no memories seem relevant, return an empty array.\n");
        
        return prompt.toString();
    }
    
    /**
     * Build a prompt for memory consolidation (merging similar memories)
     */
    public String buildConsolidationPrompt(List<Memory> candidateMemories) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Memory Consolidation Task\n\n");
        prompt.append("Analyze these memories and determine if any should be merged or consolidated.\n\n");
        
        prompt.append("### Candidate Memories:\n");
        for (Memory memory : candidateMemories) {
            prompt.append("- ID: ").append(memory.getId()).append("\n");
            prompt.append("  Type: ").append(memory.getType()).append("\n");
            prompt.append("  Content: ").append(memory.getContent()).append("\n");
            prompt.append("  Importance: ").append(memory.getImportance()).append("\n");
            prompt.append("  Created: ").append(memory.getTimestamp()).append("\n");
            prompt.append("  Access Count: ").append(memory.getAccessCount()).append("\n\n");
        }
        
        prompt.append("### Instructions:\n");
        prompt.append("Look for opportunities to:\n");
        prompt.append("1. Merge duplicate or very similar memories\n");
        prompt.append("2. Combine related memories that would be more useful together\n");
        prompt.append("3. Update memories with more recent or accurate information\n\n");
        
        prompt.append("Only suggest merges when it would genuinely improve the memory system. ");
        prompt.append("Preserve important details and maintain accuracy.\n\n");
        
        prompt.append("Respond with a JSON object containing the operations array.\n");
        
        return prompt.toString();
    }
    
    /**
     * Build a prompt for memory importance re-evaluation
     */
    public String buildImportancePrompt(List<Memory> memories, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("## Memory Importance Re-evaluation Task\n\n");
        prompt.append("Re-evaluate the importance scores of these memories based on recent usage and context.\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("### Context:\n");
            prompt.append(context).append("\n\n");
        }
        
        prompt.append("### Memories to Evaluate:\n");
        for (Memory memory : memories) {
            prompt.append("- ID: ").append(memory.getId()).append("\n");
            prompt.append("  Current Importance: ").append(memory.getImportance()).append("\n");
            prompt.append("  Content: ").append(memory.getContent()).append("\n");
            prompt.append("  Access Count: ").append(memory.getAccessCount()).append("\n");
            prompt.append("  Last Accessed: ").append(memory.getLastAccessed()).append("\n\n");
        }
        
        prompt.append("### Instructions:\n");
        prompt.append("Suggest new importance scores (0.0-1.0) based on:\n");
        prompt.append("1. How frequently the memory has been accessed\n");
        prompt.append("2. How recently it was accessed\n");
        prompt.append("3. How relevant it is to current conversation patterns\n");
        prompt.append("4. How unique or irreplaceable the information is\n\n");
        
        prompt.append("Only suggest updates when the importance should change significantly (±0.1 or more).\n\n");
        
        prompt.append("Respond with UPDATE operations for memories that need importance adjustments.\n");
        
        return prompt.toString();
    }
}
