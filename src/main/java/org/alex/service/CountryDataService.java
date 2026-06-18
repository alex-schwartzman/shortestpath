package org.alex.service;

import org.alex.exception.CountryDataUnavailableException;
import org.alex.model.Borders;
import org.alex.model.IsoCode;
import org.alex.model.in.Country;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches country border data from the upstream GitHub source and exposes
 * it as an immutable {@link Borders} graph.
 * <p>
 * The fetch + deserialize + graph-building pipeline runs at most once per
 * cache entry: {@link #getBorders()} is cached via {@code @Cacheable}, so
 * subsequent calls return the same already-built {@link Borders} instance
 * without re-fetching. Only {@link Borders} — already verified immutable —
 * ever crosses this method's boundary; the intermediate {@code List<Country>}
 * and {@code Map} built along the way are local to this method and never
 * shared, so there is nothing mutable for callers to accidentally corrupt.
 * <p>
 * A failed fetch or parse throws {@link CountryDataUnavailableException}
 * rather than returning null or an empty graph. Spring's {@code @Cacheable}
 * does not cache thrown exceptions, so a transient upstream failure does not
 * permanently poison the cache — the next request will simply retry the fetch.
 * <p>
 * NOTE on response Content-Type: raw.githubusercontent.com always serves
 * file contents as "text/plain", regardless of the actual file type — this
 * is documented, deliberate GitHub behavior, not a misconfiguration, and not
 * something fixable by sending an Accept header (GitHub ignores it). Without
 * intervention, RestClient's default Jackson converter refuses to handle a
 * "text/plain" response (it's only registered for "application/json" and
 * similar), throwing UnknownContentTypeException. The fix here registers a
 * Jackson converter that explicitly also accepts "text/plain" as JSON input,
 * scoped to this one RestClient instance only — not a global MVC config
 * change — since this quirk is specific to this one upstream source.
 */
@Service
public class CountryDataService {

    private final RestClient restClient;
    private final String countriesJsonUrl;

    public CountryDataService(
            RestClient.Builder restClientBuilder,
            @Value("${countries.data.url:https://raw.githubusercontent.com/mledoze/countries/master/countries.json}")
            String countriesJsonUrl
    ) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN
        ));

        this.restClient = restClientBuilder
                .messageConverters(converters -> converters.add(0, jsonConverter))
                .build();
        this.countriesJsonUrl = countriesJsonUrl;
    }

    @Cacheable("borders")
    public Borders getBorders() {
        List<Country> countries = fetchCountries();
        Map<IsoCode, Set<IsoCode>> adjacency = buildAdjacency(countries);
        return new Borders(adjacency);
    }

    private List<Country> fetchCountries() {
        try {
            List<Country> countries = restClient.get()
                    .uri(countriesJsonUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Country>>() {
                    });

            if (countries == null) {
                throw new CountryDataUnavailableException(
                        "Upstream country data response body was empty", null);
            }
            return countries;

        } catch (RestClientException e) {
            // Covers both connection failures (host unreachable, timeout) and
            // non-2xx HTTP responses (RestClient throws on those by default).
            throw new CountryDataUnavailableException(
                    "Failed to fetch country data from " + countriesJsonUrl, e);
        }
    }

    private Map<IsoCode, Set<IsoCode>> buildAdjacency(List<Country> countries) {
        Map<IsoCode, Set<IsoCode>> adjacency = new HashMap<>();
        for (Country country : countries) {
            adjacency.put(country.id(),
                    country.neighbors() != null ? Set.copyOf(country.neighbors()) : Set.of());
        }
        return adjacency;
    }
}

