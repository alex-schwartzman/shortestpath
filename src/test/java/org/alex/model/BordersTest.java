package org.alex.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BordersTest {

    private final IsoCode cze = new IsoCode("CZE");
    private final IsoCode aut = new IsoCode("AUT");
    private final IsoCode deu = new IsoCode("DEU");
    private final IsoCode abw = new IsoCode("ABW"); // Aruba: real country, no land neighbors
    private final IsoCode xyz = new IsoCode("XYZ"); // not present in any test map at all

    @Test
    void neighborsOfReturnsWhatWasGiven() {
        Borders borders = new Borders(Map.of(cze, Set.of(aut, deu)));

        assertThat(borders.neighborsOf(cze)).containsExactlyInAnyOrder(aut, deu);
    }

    @Test
    void unknownCodeReturnsEmptySetNotNullAndContainsIsFalse() {
        Borders borders = new Borders(Map.of(cze, Set.of(aut)));

        assertThat(borders.neighborsOf(xyz)).isEmpty();
        assertThat(borders.contains(xyz)).isFalse();
    }

    @Test
    void knownCodeWithNoNeighborsIsDistinctFromUnknownCode() {
        // The island case: a real, known country with zero land borders.
        // neighborsOf is empty either way, but contains() must still tell
        // "no neighbors" apart from "not a real country code".
        Borders borders = new Borders(Map.of(abw, Set.of(), cze, Set.of(aut)));

        assertThat(borders.neighborsOf(abw)).isEmpty();
        assertThat(borders.contains(abw)).isTrue();

        assertThat(borders.neighborsOf(xyz)).isEmpty();
        assertThat(borders.contains(xyz)).isFalse();
    }

    @Test
    void returnedNeighborSetIsUnmodifiable() {
        Borders borders = new Borders(Map.of(cze, Set.of(aut)));

        Set<IsoCode> neighbors = borders.neighborsOf(cze);

        assertThatThrownBy(() -> neighbors.add(deu))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mutatingRetainedReferenceToOriginalInputSetAfterConstructionDoesNotAffectBorders() {
        // immutability guards:
        // What if the caller passes in a mutable HashSet and keeps a
        // reference to it, does mutating that reference after construction
        // leak through into Borders? It must not, or the immutability
        // guarantee is hollow for exactly the case @Cacheable depends on:
        // many concurrent callers sharing one Borders instance over time.
        Set<IsoCode> mutableNeighborSet = new HashSet<>();
        mutableNeighborSet.add(aut);

        Map<IsoCode, Set<IsoCode>> inputMap = new HashMap<>();
        inputMap.put(cze, mutableNeighborSet);

        Borders borders = new Borders(inputMap);

        // Mutate the original Set *after* Borders was constructed.
        mutableNeighborSet.add(deu);

        assertThat(borders.neighborsOf(cze))
                .as("Borders must not be affected by mutating a retained reference "
                        + "to the original input Set after construction")
                .containsExactly(aut);
    }

    @Test
    void mutatingOriginalInputMapAfterConstructionDoesNotAffectBorders() {
        // Same question, but for the outer map itself rather than a nested
        // Set value: does Map.copyOf actually decouple Borders from the
        // caller's map reference?
        Map<IsoCode, Set<IsoCode>> inputMap = new HashMap<>();
        inputMap.put(cze, Set.of(aut));

        Borders borders = new Borders(inputMap);

        inputMap.put(deu, Set.of(cze)); // add an entirely new entry after construction

        assertThat(borders.contains(deu)).isFalse();
    }

    @Test
    void directionalityIsNotInferredInReverse() {
        // Mirrors the real, verified asymmetry in countries.json: Sri Lanka
        // lists India as a neighbor, but India's own list does not include
        // Sri Lanka back. Borders must preserve this exactly, never
        // inferring a reverse edge from a forward one.
        IsoCode lka = new IsoCode("LKA");
        IsoCode ind = new IsoCode("IND");

        Borders borders = new Borders(Map.of(
                lka, Set.of(ind),
                ind, Set.of() // India's real borders list doesn't include LKA
        ));

        assertThat(borders.neighborsOf(lka)).containsExactly(ind);
        assertThat(borders.neighborsOf(ind)).isEmpty();
    }
}
