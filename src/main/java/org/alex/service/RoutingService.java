package org.alex.service;

import org.alex.exception.NoRouteFoundException;
import org.alex.exception.UnknownCountryException;
import org.alex.model.Borders;
import org.alex.model.IsoCode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Finds a land route between two countries by breadth-first search over a
 * {@link Borders} graph.
 * <p>
 * BFS is the right algorithm here, not an arbitrary choice: Borders is an
 * unweighted graph (a border crossing is a border crossing — there's no
 * notion of one being "shorter" than another), and BFS finds the shortest
 * such route (fewest crossings) in O(V + E) time, which is efficient at the
 * actual scale of this dataset (a few hundred countries, a similar number
 * of border relationships).
 * <p>
 * Respects Borders' directionality strictly: only ever follows
 * borders.neighborsOf(X) outward from X, never infers or relies on a
 * reverse edge. This matters concretely — Borders is known, from the real
 * countries.json data, to contain at least one asymmetric pair (Sri Lanka
 * lists India as a neighbor; India does not list Sri Lanka back) — so a
 * route that happens to traverse such a pair is only valid in the direction
 * the data actually states.
 */
@Service
public class RoutingService {

    private final CountryDataService countryDataService;

    public RoutingService(CountryDataService countryDataService) {
        this.countryDataService = countryDataService;
    }

    public String hops(IsoCode origin, IsoCode destination) {
        return String.valueOf(findRoute(origin, destination));
    }

    
    /**
     * @return the shortest land route from origin to destination, inclusive
     * of both endpoints. If origin equals destination, returns a
     * single-element route containing just that code.
     * @throws UnknownCountryException if origin or destination is not a
     *                                 recognized country code in the current dataset
     * @throws NoRouteFoundException   if both codes are known but no land
     *                                 route connects them
     */
    public List<IsoCode> findRoute(IsoCode origin, IsoCode destination) {
        Borders borders = countryDataService.getBorders();

        if (!borders.contains(origin)) {
            throw new UnknownCountryException("Unknown origin country code: " + origin);
        }
        if (!borders.contains(destination)) {
            throw new UnknownCountryException("Unknown destination country code: " + destination);
        }

        if (origin.equals(destination)) {
            return List.of(origin);
        }

        Map<IsoCode, IsoCode> cameFrom = new HashMap<>();
        Set<IsoCode> visited = new HashSet<>();
        Queue<IsoCode> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty()) {
            IsoCode current = queue.poll();

            if (current.equals(destination)) {
                return reconstructPath(cameFrom, origin, destination);
            }

            for (IsoCode neighbor : borders.neighborsOf(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        throw new NoRouteFoundException(
                "No land route exists from " + origin + " to " + destination);
    }

    private List<IsoCode> reconstructPath(Map<IsoCode, IsoCode> cameFrom, IsoCode origin, IsoCode destination) {
        List<IsoCode> path = new ArrayList<>();
        IsoCode step = destination;

        while (!step.equals(origin)) {
            path.add(step);
            step = cameFrom.get(step);
        }
        path.add(origin);

        Collections.reverse(path);
        return path;
    }
}
