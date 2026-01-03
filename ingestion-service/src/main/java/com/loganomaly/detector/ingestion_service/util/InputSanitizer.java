package com.loganomaly.detector.ingestion_service.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing log input to prevent injection attacks.
 * Removes or escapes potentially dangerous patterns while preserving log readability.
 */
@Component
public class InputSanitizer {

    // Pattern for HTML/Script tags
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>|<[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Pattern for SQL injection keywords (basic detection)
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "('\\s*(OR|AND)\\s*')|(--)|(;\\s*(DROP|DELETE|UPDATE|INSERT|ALTER|TRUNCATE))",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for null bytes and control characters (except newline, tab)
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile(
            "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"
    );

    // Maximum consecutive special characters allowed
    private static final int MAX_CONSECUTIVE_SPECIAL = 10;

    /**
     * Sanitize a log message by removing potentially dangerous content.
     *
     * @param input The raw input string
     * @return Sanitized string safe for storage and display
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String result = input;

        // 1. Remove control characters (keep newlines and tabs for log formatting)
        result = CONTROL_CHARS_PATTERN.matcher(result).replaceAll("");

        // 2. Remove script tags and HTML
        result = SCRIPT_PATTERN.matcher(result).replaceAll("[removed]");

        // 3. Neutralize SQL injection patterns
        result = SQL_PATTERN.matcher(result).replaceAll("[filtered]");

        // 4. Limit consecutive special characters (potential attack padding)
        result = limitConsecutiveSpecialChars(result);

        return result;
    }

    /**
     * Sanitize service name - more restrictive, alphanumeric and dashes only.
     *
     * @param serviceName The raw service name
     * @return Sanitized service name
     */
    public String sanitizeServiceName(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        // Only allow alphanumeric, dashes, underscores, and dots
        return serviceName.replaceAll("[^a-zA-Z0-9\\-_.]", "");
    }

    /**
     * Limit consecutive special characters to prevent attack padding.
     */
    private String limitConsecutiveSpecialChars(String input) {
        StringBuilder result = new StringBuilder();
        int consecutiveSpecial = 0;
        
        for (char c : input.toCharArray()) {
            if (isSpecialChar(c)) {
                consecutiveSpecial++;
                if (consecutiveSpecial <= MAX_CONSECUTIVE_SPECIAL) {
                    result.append(c);
                }
                // Skip if exceeded limit
            } else {
                consecutiveSpecial = 0;
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * Check if character is a "special" character for limiting purposes.
     */
    private boolean isSpecialChar(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }
}

