package org.alex.service;

import org.alex.model.Borders;
import org.alex.model.IsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies RoutingService against the spec's literal example
 * (GET /routing/CZE/ITA -> {"route": ["CZE", "AUT", "ITA"]}), using a
 * CountryDataService mock seeded with a real, verified subset of the actual
 * countries.json adjacency data for the CZE/AUT/ITA neighborhood.
 *
 * This is deliberately NOT the same as RoutingServiceTest (which uses
 * entirely fictional codes against a synthetic fixture, to keep that test
 * unmistakably about algorithm correctness in isolation). This test exists
 * specifically to close the gap that fictional-code test cannot: proving
 * the spec's exact example holds against real-world adjacency facts.
 *
 * The adjacency below was re-verified directly against the live
 * countries.json immediately before writing this test (not transcribed
 * from memory) — see the borders lists quoted in each comment.
 *
 * Still mocked, not a full integration test: this does not hit the network.
 * CountryDataServiceLiveFetchTest (extended with RoutingService) covers the
 * full live-network path separately.
 */
class RoutingServiceRealDataTest {

    // Real cca3 codes, real borders, verified against the live dataset:
    private final IsoCode cze = new IsoCode("CZE"); // borders: AUT, DEU, POL, SVK
    private final IsoCode aut = new IsoCode("AUT"); // borders: CZE, DEU, HUN, ITA, SVK, SVN, CHE
    private final IsoCode ita = new IsoCode("ITA"); // borders: AUT, FRA, SMR, SVN, CHE, VAT
    private final IsoCode deu = new IsoCode("DEU"); // borders: AUT, BEL, CZE, DNK, FRA, LUX, NLD, POL, CHE
    private final IsoCode pol = new IsoCode("POL"); // borders: BLR, CZE, DEU, LTU, RUS, SVK, UKR
    private final IsoCode svk = new IsoCode("SVK"); // borders: AUT, CZE, HUN, POL, UKR
    private final IsoCode che = new IsoCode("CHE"); // borders: AUT, FRA, ITA, DEU
    private final IsoCode svn = new IsoCode("SVN"); // borders: AUT, HRV, ITA, HUN
    private final IsoCode hun = new IsoCode("HUN"); // borders: AUT, HRV, ROU, SRB, SVK, SVN, UKR

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        Borders borders = new Borders(Map.ofEntries(
                Map.entry(cze, Set.of(aut, deu, pol, svk)),
                Map.entry(aut, Set.of(cze, deu, hun, ita, che, svk, svn)),
                Map.entry(ita, Set.of(aut, che, svn)),
                Map.entry(deu, Set.of(aut, cze, pol, che)),
                Map.entry(pol, Set.of(cze, deu, svk)),
                Map.entry(svk, Set.of(aut, cze, hun, pol)),
                Map.entry(che, Set.of(aut, ita, deu)),
                Map.entry(svn, Set.of(aut, ita, hun)),
                Map.entry(hun, Set.of(aut, svk, svn))
        ));

        CountryDataService mockDataService = Mockito.mock(CountryDataService.class);
        Mockito.when(mockDataService.getBorders()).thenReturn(borders);

        routingService = new RoutingService(mockDataService);
    }

    @Test
    void matchesTheSpecsExactExample() {
        // GET /routing/CZE/ITA -> {"route": ["CZE", "AUT", "ITA"]}
        assertThat(routingService.findRoute(cze, ita))
                .containsExactly(cze, aut, ita);
    }

    @Test
    void czeToItaIsGenuinelyTheShortestPathNotJustAValidOne() {
        // CZE has no direct border with ITA (confirmed: CZE's real borders
        // list is AUT/DEU/POL/SVK, no ITA), so 2 hops via AUT is the floor —
        // there is no shorter path available, confirming the spec's example
        // isn't just "a" route but genuinely the shortest one.
        assertThat(routingService.findRoute(cze, ita)).hasSize(3);
    }
}