package org.alex.exception;

/**
 * Thrown when origin and destination are both known, valid country codes,
 * but no land route exists connecting them (e.g. one or both are islands
 * with no land border, or they sit in entirely disconnected landmasses).
 *
 * Mapped to HTTP 400 by the controller, per spec.
 */
public class NoRouteFoundException extends RuntimeException {

    public NoRouteFoundException(String message) {
        super(message);
    }
}
