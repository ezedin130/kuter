package com.kuter.common;

import java.util.regex.Pattern;

/**
 * Utility for normalizing and validating Ethiopian mobile phone numbers.
 * The canonical stored format is: +2519XXXXXXXX (or +2517XXXXXXXX).
 */
public final class EthiopianPhoneNumberUtils {

    public static final String CANONICAL_PREFIX = "+251";
    private static final Pattern ETH_MOBILE_PATTERN = Pattern.compile("^[79]\\d{8}$");

    private EthiopianPhoneNumberUtils() {
        // Prevent instantiation
    }

    /**
     * Normalizes an input phone number into canonical Ethiopian mobile format (+2519XXXXXXXX).
     *
     * Permitted input examples:
     * - "982112233"      -> "+251982112233"
     * - "0982112233"     -> "+251982112233"
     * - "+251982112233"  -> "+251982112233"
     * - "251982112233"   -> "+251982112233"
     * - "+251 982-112233" -> "+251982112233"
     *
     * @param input Raw phone number entered by user
     * @return Canonical E.164 phone string starting with +251 followed by 9 digits
     * @throws BadRequestException if the number is invalid
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        // 1. Trim & remove whitespace, hyphens, parentheses, and dots
        String cleaned = input.trim().replaceAll("[\\s\\-\\.\\(\\)]+", "");

        // 2. Remove international / national prefix variations
        if (cleaned.startsWith("+251")) {
            cleaned = cleaned.substring(4);
        } else if (cleaned.startsWith("00251")) {
            cleaned = cleaned.substring(5);
        } else if (cleaned.startsWith("251")) {
            cleaned = cleaned.substring(3);
        }

        // 3. Remove single leading zero if supplied
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        // 4. Validate remaining digits (must be exactly 9 digits starting with 9 or 7)
        if (!ETH_MOBILE_PATTERN.matcher(cleaned).matches()) {
            throw new BadRequestException("Invalid Ethiopian mobile phone number: '" + input +
                    "'. Expected a 9-digit number starting with 9 (e.g. 982112233 or 0982112233).");
        }

        // 5. Canonical format: +251 followed by the 9 subscriber digits
        return CANONICAL_PREFIX + cleaned;
    }

    /**
     * Returns true if the input can be successfully normalized into a valid Ethiopian mobile number.
     */
    public static boolean isValid(String input) {
        try {
            return normalize(input) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalizes an input if it is a valid Ethiopian number; otherwise returns the trimmed input if non-blank.
     */
    public static String normalizeIfValid(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return normalize(input);
        } catch (Exception e) {
            return input.trim();
        }
    }
}
