package org.alex.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * A validated ISO 3166-1 alpha-3 ("cca3") country code, e.g. "CZE".
 *
 * Wrapping the bare String prevents accidentally passing a country name,
 * a malformed path variable, or an unrelated String anywhere an IsoCode
 * is expected — the compiler enforces the distinction instead of relying
 * on convention.
 *
 * Deserialization uses an explicit static @JsonCreator factory method rather
 * than relying on Jackson's implicit single-arg-record-constructor delegation:
 * that implicit path is not reliably triggered for scalar JSON values across
 * Jackson versions, and is documented to interact unpredictably with @JsonValue
 * on the same record. The explicit factory method sidesteps both issues.
 */
public record IsoCode(String value) {

    public IsoCode {
        if (value == null || value.length() != 3) {
            throw new IllegalArgumentException(
                    "Invalid ISO cca3 code (expected exactly 3 letters): " + value);
        }
        value = value.toUpperCase(Locale.ROOT);
        if (!value.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException(
                    "Invalid ISO cca3 code (expected letters only): " + value);
        }
    }

    @JsonCreator
    public static IsoCode of(String value) {
        return new IsoCode(value);
    }

    /**
     * Serializes as a bare JSON string (e.g. "CZE") rather than as an object
     * wrapping a "value" field — so RouteResponse round-trips to exactly the
     * shape the spec expects: {"route": ["CZE", "AUT", "ITA"]}.
     */
    @JsonValue
    @Override
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}