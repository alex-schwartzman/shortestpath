package org.alex.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsoCodeTest {

    final IsoCode cz = new IsoCode("CZE");

    @Test
    void of() {
        // valid 3-letter code, already uppercase, passes through unchanged
        assertEquals("CZE", IsoCode.of("CZE").value());

        // lowercase input is normalized to uppercase
        assertEquals("CZE", IsoCode.of("cze").value());

        // wrong length is rejected
        assertThrows(IllegalArgumentException.class, () -> IsoCode.of("CZ"));
        assertThrows(IllegalArgumentException.class, () -> IsoCode.of("CZECH"));

        // null is rejected, not silently passed through
        assertThrows(IllegalArgumentException.class, () -> IsoCode.of(null));

        // non-letter characters are rejected (e.g. accidental digit/punctuation)
        assertThrows(IllegalArgumentException.class, () -> IsoCode.of("CZ3"));
    }

    @Test
    void value() {
        assertEquals("CZE", cz.value());
    }

    @Test
    void testToString() {
        assertEquals("CZE", cz.toString());
    }
}