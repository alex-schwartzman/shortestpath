package org.alex.model.out;

import org.alex.model.IsoCode;

import java.util.List;

/**
 * JSON-out DTO matching the spec's exact success response shape:
 * {"route": ["CZE", "AUT", "ITA"]}
 * <p>
 * IsoCode serializes as a bare JSON string (see IsoCode's @JsonValue), so
 * this produces a plain array of strings, not an array of objects.
 */
public record RouteResponse(List<IsoCode> route) {
}
