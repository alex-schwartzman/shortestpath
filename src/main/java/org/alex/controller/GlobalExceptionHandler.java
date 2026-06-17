package org.alex.controller;

import org.alex.exception.CountryDataUnavailableException;
import org.alex.exception.NoRouteFoundException;
import org.alex.exception.UnknownCountryException;
import org.alex.model.out.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates the application's exceptions into HTTP responses.
 *
 * Three distinct exception types all map to 400, per the spec's literal
 * wording ("if there is no land crossing, return 400") — the spec doesn't
 * carve out separate statuses for malformed input vs. unknown-but-valid-
 * shaped codes vs. genuinely no route, so this doesn't invent a richer
 * status scheme than asked for. The distinction between them is preserved
 * in the error message instead, and in the exception types themselves
 * remaining distinguishable internally (e.g. for logging).
 *
 * CountryDataUnavailableException is the one genuinely different case: it
 * means the service cannot answer ANY routing question right now (upstream
 * fetch/parse failure), not that this specific origin/destination pair has
 * no route — mapped to 503, not 400.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleMalformedCountryCode(IllegalArgumentException ex) {
        // Thrown by IsoCode's own constructor validation when a path
        // variable isn't a valid-shaped 3-letter code at all (e.g. "CZ",
        // "CZECH", or non-letters) — distinct from UnknownCountryException,
        // which means the shape is valid but the code isn't a real country.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnknownCountryException.class)
    public ResponseEntity<ErrorResponse> handleUnknownCountry(UnknownCountryException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoRouteFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoRouteFound(NoRouteFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CountryDataUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCountryDataUnavailable(CountryDataUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ex.getMessage()));
    }
}