package org.alex.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alex.model.in.Country;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesRealisticCountryEntry() throws Exception {
        // A trimmed but realistic slice of one entry from countries.json,
        // including unrelated fields that must be silently ignored.
        String json = """
            {
              "name": { "common": "Czechia" },
              "cca3": "cze",
              "borders": ["AUT", "DEU", "POL", "SVK"],
              "area": 78865
            }
            """;

        Country country = mapper.readValue(json, Country.class);

        assertThat(country.id()).isEqualTo(new IsoCode("CZE"));
        assertThat(country.neighbors())
                .containsExactly(new IsoCode("AUT"), new IsoCode("DEU"), new IsoCode("POL"), new IsoCode("SVK"));
    }

    @Test
    void lowercaseCca3IsNormalizedToUppercase() throws Exception {
        Country country = mapper.readValue("""
            {"cca3": "cze", "borders": []}
            """, Country.class);

        assertThat(country.id().value()).isEqualTo("CZE");
    }

    @Test
    void islandWithEmptyBordersArrayDeserializesToEmptyList() throws Exception {
        // Verified against real countries.json: 85 of 250 entries have an
        // empty borders array (islands, exclaves, etc.) — never a missing
        // key, never null. This must not NPE.
        Country aruba = mapper.readValue("""
            {"cca3": "ABW", "borders": []}
            """, Country.class);

        assertThat(aruba.neighbors()).isEmpty();
    }

    @Test
    void malformedCca3FailsFastDuringDeserialization() {
        // A 2-letter code should be rejected at the boundary, not silently
        // accepted and only fail later as a confusing "country not found".
        assertThatThrownBy(() -> mapper.readValue("""
                {"cca3": "XX", "borders": []}
                """, Country.class))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isoCodeSerializesAsBareJsonString() throws Exception {
        // This is the half of the round-trip RouteResponse depends on:
        // {"route": ["CZE", "AUT", "ITA"]} requires IsoCode to serialize
        // as a plain string, not as {"value": "CZE"}.
        String serialized = mapper.writeValueAsString(List.of(new IsoCode("CZE"), new IsoCode("AUT")));

        assertThat(serialized).isEqualTo("[\"CZE\",\"AUT\"]");
    }
}