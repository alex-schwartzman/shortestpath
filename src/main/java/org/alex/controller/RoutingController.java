package org.alex.controller;

import org.alex.model.IsoCode;
import org.alex.model.out.RouteResponse;
import org.alex.service.RoutingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes GET /routing/{origin}/{destination}, per spec.
 *
 * Contains no routing logic itself — only the HTTP-facing concerns: parsing
 * path variables into IsoCode (where malformed input fails fast via
 * IsoCode's own validation), delegating to RoutingService, and wrapping the
 * result into the response shape the spec defines. All error-to-status-code
 * translation lives in GlobalExceptionHandler, not here.
 */
@RestController
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/routing/{origin}/{destination}")
    public RouteResponse getRoute(@PathVariable String origin, @PathVariable String destination) {
        IsoCode originCode = new IsoCode(origin);
        IsoCode destinationCode = new IsoCode(destination);

        var route = routingService.findRoute(originCode, destinationCode);

        return new RouteResponse(route);
    }
}