package org.alex.service;

import org.alex.model.Borders;
import org.alex.model.IsoCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deliberately NOT mocked: this hits the real, live countries.json on
 * GitHub. That makes it slower and dependent on network/GitHub availability,
 * but it is the only way to actually verify the real fetch -> deserialize ->
 * build-adjacency pipeline works end to end, rather than verifying mocked
 * stand-ins for each piece in isolation.
 *
 * If this test becomes flaky in CI due to network conditions, the fix is to
 * isolate it (e.g. a separate "integration" Maven profile / Failsafe run)
 * rather than deleting the real-network coverage it provides.
 */
@SpringBootTest
class CountryDataServiceLiveFetchTest {

    @Autowired
    private CountryDataService springManagedService;

    @Test
    void fetchesRealCountriesJsonAndBuildsCorrectBorders() {
        CountryDataService service = new CountryDataService(
                RestClient.builder(),
                "https://raw.githubusercontent.com/mledoze/countries/master/countries.json"
        );

        Borders borders = service.getBorders();

        IsoCode cze = new IsoCode("CZE");
        IsoCode aut = new IsoCode("AUT");
        IsoCode abw = new IsoCode("ABW");
        IsoCode bogus = new IsoCode("ZZZ");

        // Spot-check facts verified directly against the dataset earlier
        // in this project: CZE borders AUT (among others); ABW (Aruba) is a
        // real country with zero land borders; an invented code is unknown.
        assertThat(borders.contains(cze)).isTrue();
        assertThat(borders.neighborsOf(cze)).contains(aut);

        assertThat(borders.contains(abw)).isTrue();
        assertThat(borders.neighborsOf(abw)).isEmpty();

        assertThat(borders.contains(bogus)).isFalse();
    }

    @Test
    void secondCallReturnsSameCachedInstanceWithoutRefetching() {
        // Uses the Spring-managed bean (springManagedService), NOT a bare
        // `new CountryDataService(...)`: @Cacheable only works through
        // Spring's proxy mechanism, so constructing the class directly (as
        // the test above does, deliberately, to test the fetch logic in
        // isolation) would never exercise caching at all.
        Borders first = springManagedService.getBorders();
        Borders second = springManagedService.getBorders();

        // Reference equality, not just .equals() — this is the actual proof
        // the second call hit the cache instead of re-running getBorders()'s
        // body (which would build a structurally-equal but distinct Borders
        // instance, indistinguishable by .equals() alone).
        assertThat(second).isSameAs(first);
    }
}