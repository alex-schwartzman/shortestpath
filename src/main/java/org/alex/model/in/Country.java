package org.alex.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.alex.model.IsoCode;

import java.util.List;

/**
 * Deserialization target for a single entry in countries.json.
 * Only the two fields actually used by the routing algorithm are mapped;
 * every other field in the source JSON (name, currencies, flags, ...) is ignored.
 * <p>
 * Field names are domain vocabulary (id, neighbors) rather than the wire-format
 * names (cca3, borders); @JsonProperty bridges the two so the rest of the
 * codebase never has to know what the upstream JSON calls things.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Country(
        @JsonProperty("cca3") IsoCode id,
        @JsonProperty("borders") List<IsoCode> neighbors
) {
}
