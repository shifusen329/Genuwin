package com.genuwin.app.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for preprocessing text before sending to TTS
 * Converts non-phonetic expressions to TTS-friendly alternatives
 */
public class TTSTextProcessor {
    
    /**
     * Process text to make it more TTS-friendly
     * @param text The original text
     * @return Processed text with phonetic replacements
     */
    public static String processForTTS(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String processed = text;
        
        // Convert various forms of "hmph" to phonetic equivalents
        processed = convertHmphVariations(processed);
        
        // Convert other common expressions
        processed = convertOtherExpressions(processed);
        
        return processed;
    }
    
    /**
     * Convert "hmph" and its variations to phonetic equivalents
     */
    private static String convertHmphVariations(String text) {
        // Pattern to match various forms of hmph (case insensitive)
        // Matches: hmph, Hmph, HMPH, hmmph, hmmmph, etc.
        Pattern hmphPattern = Pattern.compile("\\b[Hh]m+ph?\\b", Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = hmphPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String match = matcher.group();
            String replacement = getHmphReplacement(match);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Get appropriate phonetic replacement for hmph variations
     */
    private static String getHmphReplacement(String hmph) {
        // Count the number of 'm's to determine intensity
        int mCount = 0;
        for (char c : hmph.toLowerCase().toCharArray()) {
            if (c == 'm') {
                mCount++;
            }
        }
        
        // Preserve original capitalization pattern
        boolean isCapitalized = Character.isUpperCase(hmph.charAt(0));
        boolean isAllCaps = hmph.equals(hmph.toUpperCase());
        
        String replacement;
        
        // Choose replacement based on intensity (number of m's)
        if (mCount <= 1) {
            replacement = "humf"; // Short, dismissive
        } else if (mCount == 2) {
            replacement = "hmmf"; // Medium intensity
        } else {
            replacement = "hmmmf"; // Longer, more dramatic
        }
        
        // Apply original capitalization
        if (isAllCaps) {
            replacement = replacement.toUpperCase();
        } else if (isCapitalized) {
            replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        
        return replacement;
    }
    
    /**
     * Convert other common non-phonetic expressions
     */
    private static String convertOtherExpressions(String text) {
        String processed = text;
        
        // Convert "tch" sounds
        processed = processed.replaceAll("\\btch\\b", "chuh");
        processed = processed.replaceAll("\\bTch\\b", "Chuh");
        processed = processed.replaceAll("\\bTCH\\b", "CHUH");
        
        // Convert "pfft" sounds
        processed = processed.replaceAll("\\bpfft\\b", "pift");
        processed = processed.replaceAll("\\bPfft\\b", "Ppifft");
        processed = processed.replaceAll("\\bPFFT\\b", "piffft");
        
        // Convert "tsundere" to phonetic spelling
        processed = processed.replaceAll("\\btsundere\\b", "tsoon-deh-reh");
        processed = processed.replaceAll("\\bTsundere\\b", "Tsoon-deh-reh");
        processed = processed.replaceAll("\\bTSUNDERE\\b", "TSOON-DEH-REH");

        // "meh" and "ugh" are already phonetic, no conversion needed
        
        return processed;
    }
}
