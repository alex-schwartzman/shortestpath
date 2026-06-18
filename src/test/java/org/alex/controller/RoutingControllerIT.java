package org.alex.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full REST API test: a real embedded server, real HTTP requests over a
 * real socket, real RoutingController + GlobalExceptionHandler
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoutingControllerIT {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void getRoutingCzeToItaReturnsExpectedRouteOverRealHttp() {
        String url = "http://localhost:" + port + "/routing/CZE/ITA";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"route\":[\"CZE\",\"AUT\",\"ITA\"]}");
    }

    @Test
    void getRoutingToDisconnectedIslandReturns400WithErrorBody() {
        // ABW (Aruba) is in the fixture with zero borders.
        String url = "http://localhost:" + port + "/routing/CZE/ABW";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("error");
    }

    @Test
    void getRoutingWithMalformedCodeReturns400() {
        String url = "http://localhost:" + port + "/routing/CZ/ITA"; // 2 letters, invalid shape

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    @Test
    void getRoutingBetweenUnconnectedContinentsReturns400() {
        // FRA and ARG are both real, richly-connected countries (verified
        // against the live dataset: FRA borders 8 countries, ARG borders 5)
        // sitting in entirely separate, large connected components — France
        // is part of the mainland Europe/Asia/Africa landmass, Argentina is
        // part of South America, with no land bridge between them in
        // reality or in this dataset.
        //
        // This is a meaningfully stronger test than the island case (ABW)
        // used elsewhere: an island has zero neighbors, so BFS's queue
        // empties almost immediately. Here, BFS must actually exhaust the
        // ENTIRE reachable component from FRA — dozens of countries across
        // multiple continents connected by land — before correctly
        // concluding no route exists. This exercises queue/visited-set
        // behavior under a large, fully-traversed search space, not just
        // the trivial empty-neighbor-list case.
        String url = "http://localhost:" + port + "/routing/FRA/ARG";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("error");
    }

}