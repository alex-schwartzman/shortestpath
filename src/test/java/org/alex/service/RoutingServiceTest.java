package org.alex.service;

import org.alex.exception.NoRouteFoundException;
import org.alex.exception.UnknownCountryException;
import org.alex.model.Borders;
import org.alex.model.IsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses entirely fictional 3-letter codes (Hogwarts, Narnia, Shire, Mordor,
 * Gondor, Oz, Wonderland...) rather than real cca3 codes like CZE/AUT/ITA.
 *
 * This is deliberate, not decorative: it makes it visually unmistakable that
 * this fixture is a small, hand-built, synthetic graph constructed to be
 * easy to verify by eye — not an excerpt of the real countries.json data.
 * (The real-data equivalent of this test — proving the spec's actual
 * CZE -> AUT -> ITA example holds against the live dataset — belongs in an
 * integration test that calls the real CountryDataService, not here.)
 */
class RoutingServiceTest {

    private final IsoCode hog = new IsoCode("HOG"); // Hogwarts
    private final IsoCode nar = new IsoCode("NAR"); // Narnia
    private final IsoCode shi = new IsoCode("SHI"); // Shire
    private final IsoCode gon = new IsoCode("GON"); // Gondor
    private final IsoCode mor = new IsoCode("MOR"); // Mordor
    private final IsoCode ozl = new IsoCode("OZL"); // Oz
    private final IsoCode won = new IsoCode("WON"); // Wonderland, also borders GON/NAR

    // A fictional landlocked-by-magic place with zero borders to anyone,
    // mirroring the real shape of an island like Aruba in the actual data.
    private final IsoCode atl = new IsoCode("ATL"); // Atlantis

    // Mirrors the real, verified asymmetry in countries.json:
    // Sri Lanka -> India exists, India -> Sri Lanka does not.
    private final IsoCode neverland = new IsoCode("NEV"); // Neverland: one-way out
    private final IsoCode wonderlandReverse = new IsoCode("WND"); // never lists Neverland back

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        // A small, hand-checkable graph shaped like the real CZE/AUT/ITA
        // neighborhood (HOG/NAR/SHI standing in for CZE/DEU/POL/SVK-style
        // landlocked neighbors; GON/MOR/OZL/WON standing in for the
        // AUT/ITA/CHE-style cluster one hop further south), plus a
        // disconnected island (ATL) and an asymmetric pair (NEV/WND).
        //
        // GON -> OZL is a direct edge, making HOG -> GON -> OZL the unique
        // 2-hop shortest path, distinct from the longer 3-hop alternatives
        // via MOR or WON (HOG -> GON -> MOR -> OZL / HOG -> GON -> WON -> OZL).
        // Traced by hand before relying on it this time.
        Borders borders = new Borders(Map.of(
                hog, Set.of(nar, shi, gon),
                nar, Set.of(hog, shi),
                shi, Set.of(hog, nar),
                gon, Set.of(hog, mor, won, ozl),
                mor, Set.of(gon, ozl),
                ozl, Set.of(mor, won, gon),
                won, Set.of(gon, ozl),
                atl, Set.of(),
                neverland, Set.of(wonderlandReverse),
                wonderlandReverse, Set.of()
        ));

        CountryDataService mockDataService = Mockito.mock(CountryDataService.class);
        Mockito.when(mockDataService.getBorders()).thenReturn(borders);

        routingService = new RoutingService(mockDataService);
    }

    @Test
    void findsShortestRouteAcrossMultipleHops() {
        // HOG -> GON -> OZL: 2 hops, the unique shortest path (GON has a
        // direct edge to OZL). Longer 3-hop alternatives also exist in the
        // graph (HOG -> GON -> MOR -> OZL, HOG -> GON -> WON -> OZL), so
        // this proves BFS returns the genuinely shortest one, not just any
        // valid path that happens to terminate at OZL.
        assertThat(routingService.findRoute(hog, ozl))
                .containsExactly(hog, gon, ozl);
    }

    @Test
    void findsShortestRouteNotJustAnyRoute() {
        assertThat(routingService.findRoute(hog, ozl)).hasSize(3);
    }

    @Test
    void originEqualsDestinationReturnsSingleElementRoute() {
        assertThat(routingService.findRoute(hog, hog)).containsExactly(hog);
    }

    @Test
    void throwsNoRouteFoundWhenDestinationIsDisconnectedIsland() {
        // ATL (mirroring real Aruba) has zero land borders, so nothing can
        // reach it and it can reach nothing.
        assertThatThrownBy(() -> routingService.findRoute(hog, atl))
                .isInstanceOf(NoRouteFoundException.class);
    }

    @Test
    void throwsUnknownCountryExceptionForUnrecognizedOrigin() {
        IsoCode bogus = new IsoCode("ZZZ");

        assertThatThrownBy(() -> routingService.findRoute(bogus, ozl))
                .isInstanceOf(UnknownCountryException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void throwsUnknownCountryExceptionForUnrecognizedDestination() {
        IsoCode bogus = new IsoCode("ZZZ");

        assertThatThrownBy(() -> routingService.findRoute(hog, bogus))
                .isInstanceOf(UnknownCountryException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void respectsDirectionalityForwardEdgeWorks() {
        // NEV -> WND is listed, so a route in this direction must succeed.
        assertThat(routingService.findRoute(neverland, wonderlandReverse))
                .containsExactly(neverland, wonderlandReverse);
    }

    @Test
    void respectsDirectionalityReverseEdgeIsNotInferred() {
        // WND does not list NEV back, so BFS must NOT find a route in
        // reverse just because the forward edge exists. If this test fails,
        // it means the algorithm is treating Borders as undirected somehow
        // (e.g. via an accidental symmetric lookup), which would be a real
        // correctness bug given Borders is documented and tested as directed.
        assertThatThrownBy(() -> routingService.findRoute(wonderlandReverse, neverland))
                .isInstanceOf(NoRouteFoundException.class);
    }
}