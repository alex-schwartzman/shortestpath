package org.alex.service;

import org.alex.model.IsoCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test: no mocking anywhere in this class. A real
 * CountryDataService fetches the live countries.json from GitHub, and a
 * real RoutingService runs BFS over the resulting actual Borders graph.
 *
 * This is the only test in the project that proves the spec's literal
 * example (GET /routing/CZE/ITA -> {"route": ["CZE", "AUT", "ITA"]}) holds
 * against the real, live, current upstream dataset — not a hand-built or
 * hand-verified-then-transcribed fixture. It is intentionally slower and
 * dependent on network/GitHub availability; that's an accepted tradeoff for
 * the genuine end-to-end guarantee it provides, not a flaw to "fix" by
 * mocking it away.
 *
 * Constructed directly (not via @SpringBootTest) since neither class needs
 * any other Spring-managed collaborator beyond what's passed to its own
 * constructor — RoutingService just needs a working CountryDataService, and
 * CountryDataService just needs a RestClient.Builder and a URL. Caching
 * behavior itself is covered separately in CountryDataServiceLiveFetchTest.
 */
class RoutingServiceLiveIntegrationTest {

    @Test
    void specExampleHoldsAgainstRealLiveData() {
        CountryDataService countryDataService = new CountryDataService(
                RestClient.builder(),
                "https://raw.githubusercontent.com/mledoze/countries/master/countries.json"
        );
        RoutingService routingService = new RoutingService(countryDataService);

        List<IsoCode> route = routingService.findRoute(new IsoCode("CZE"), new IsoCode("ITA"));

        assertThat(route)
                .containsExactly(new IsoCode("CZE"), new IsoCode("AUT"), new IsoCode("ITA"));
    }
}