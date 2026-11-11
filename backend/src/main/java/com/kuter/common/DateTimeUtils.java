package com.kuter.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Standard utility for deterministic UTC date-range parsing and validation.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    /**
     * Parses an ISO-8601 UTC timestamp or YYYY-MM-DD date string.
     *
     * @param input   the raw input string
     * @param isStart true if parsing the start boundary (inclusive 00:00:00 UTC), false for end boundary (inclusive 23:59:59.999999999 UTC)
     * @return Instant or null if input is null/blank
     */
    public static Instant parseIsoOrDate(String input, boolean isStart) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        try {
            if (trimmed.contains("T")) {
                return Instant.parse(trimmed);
            }
            LocalDate date = LocalDate.parse(trimmed);
            if (isStart) {
                return date.atStartOfDay(ZoneOffset.UTC).toInstant();
            } else {
                return date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid date format: '" + input + "'. Expected format: YYYY-MM-DD or ISO-8601 UTC timestamp");
        }
    }

    /**
     * Validates that the start instant is not after the end instant.
     */
    public static void validateDateRange(Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Start date (" + start + ") cannot be after end date (" + end + ")");
        }
    }
}
