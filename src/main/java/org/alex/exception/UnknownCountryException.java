package org.alex.exception;

/**
 * Thrown when an {@code IsoCode} given as origin or destination is not a
 * known country in the current {@code Borders} dataset.
 *
 * Distinct from {@link NoRouteFoundException}: this means the code itself
 * isn't recognized at all, not that the code is valid but disconnected from
 * the other endpoint. Both currently map to HTTP 400 in the controller, per
 * the spec's mandate that "no land crossing" returns 400 — kept as a
 * separate exception type so the two situations remain distinguishable
 * internally (e.g. in error messages or logs) even though they share a
 * status code today.
 */
public class UnknownCountryException extends RuntimeException {

    public UnknownCountryException(String message) {
        super(message);
    }
}

