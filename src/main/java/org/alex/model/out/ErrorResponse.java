package org.alex.model.out;

/**
 * JSON-out DTO for error responses (400/503), e.g. {"error": "..."}.
 *
 * Deliberately as minimal as RouteResponse: a single flat field, no nested
 * structure, no error codes or timestamps — the spec doesn't ask for a rich
 * error schema, so this doesn't invent one.
 */
public record ErrorResponse(String error) {
}


