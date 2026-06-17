package org.alex.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An immutable, directed adjacency graph of land border relationships
 * between countries, keyed by IsoCode.
 * <p>
 * Directionality is deliberate and preserved exactly as given by the source
 * data: a border listed for country X pointing to Y does NOT imply Y also
 * lists X as a neighbor. Some borders in the real-world data are only
 * traversable in one direction (e.g. due to geopolitical restrictions), so
 * this type never symmetrizes or validates mutuality — neighborsOf(X) and
 * neighborsOf(Y) are entirely independent lookups.
 * <p>
 * Immutability is enforced structurally, not just by convention: the
 * constructor defensively copies into an unmodifiable map (and unmodifiable
 * sets), so no caller — including one holding a reference to the original
 * mutable map used to build this — can mutate shared state after construction.
 * This matters because instances of this type are held by a single shared
 *
 * @Cacheable entry across every concurrent request.
 */
public final class Borders {

    private final Map<IsoCode, Set<IsoCode>> adjacency;

    public Borders(Map<IsoCode, Set<IsoCode>> adjacency) {
        // Map.copyOf alone only protects the outer map's structure (keys/entries).
        // It copies each value reference as-is — it does NOT deep-copy the Set
        // instances themselves. Without the explicit Set.copyOf below, a caller
        // retaining a reference to one of the original mutable Sets could mutate
        // it after construction, and that mutation would leak through into this
        // supposedly-immutable Borders instance. Verified by a failing test
        // before this fix; do not remove the per-value copy as a "simplification".
        Map<IsoCode, Set<IsoCode>> deepCopy = new HashMap<>();
        for (Map.Entry<IsoCode, Set<IsoCode>> entry : adjacency.entrySet()) {
            deepCopy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        this.adjacency = Map.copyOf(deepCopy);
    }

    /**
     * @return true if {@code code} is a known country in this dataset,
     * regardless of whether it has any neighbors.
     */
    public boolean contains(IsoCode code) {
        return adjacency.containsKey(code);
    }

    /**
     * @return the set of countries directly reachable by land from {@code code},
     * in the direction X -> neighbor only. Returns an empty set both
     * for unknown codes and for known codes with no land borders
     * (e.g. islands) — use {@link #contains} to distinguish the two
     * if that distinction matters to the caller.
     */
    public Set<IsoCode> neighborsOf(IsoCode code) {
        return adjacency.getOrDefault(code, Set.of());
    }
}