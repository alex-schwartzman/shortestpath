package org.alex.exception;

/**
 * Thrown when the upstream countries.json data cannot be fetched (network
 * failure, non-2xx response) or cannot be parsed (malformed JSON).
 *
 * Deliberately distinct from NoRouteFoundException: this represents the
 * service being unable to answer ANY routing question right now, not a
 * specific origin/destination pair having no land route. Mapped to HTTP 503
 * (Service Unavailable) by the controller's exception handling, not the 400
 * the spec reserves for "no land crossing exists".
 */
public class CountryDataUnavailableException extends RuntimeException {

    public CountryDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
