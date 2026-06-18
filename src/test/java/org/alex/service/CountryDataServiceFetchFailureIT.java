package org.alex.service;

import org.alex.exception.CountryDataUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deliberately NOT mocked: points at a real but nonexistent path on GitHub's
 * raw-content host, to verify the actual 404 response (a real HTTP response,
 * not a simulated one) is correctly translated into
 * CountryDataUnavailableException rather than some other exception type or
 * a silent failure.
 */
class CountryDataServiceFetchFailureIT {

    @Test
    void throwsCountryDataUnavailableExceptionWhenUrlDoesNotExist() {
        CountryDataService service = new CountryDataService(
                RestClient.builder(),
                "https://raw.githubusercontent.com/mledoze/countries/master/this-file-does-not-exist.json"
        );

        assertThatThrownBy(service::getBorders)
                .isInstanceOf(CountryDataUnavailableException.class)
                .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
    }

    @Test
    void throwsCountryDataUnavailableExceptionWhenHostIsUnreachable() {
        CountryDataService service = new CountryDataService(
                RestClient.builder(),
                "https://this-host-does-not-exist-at-all.invalid/countries.json"
        );

        assertThatThrownBy(service::getBorders)
                .isInstanceOf(CountryDataUnavailableException.class);
    }
}