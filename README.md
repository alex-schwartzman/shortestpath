# Country Routing Service

A Spring Boot service that calculates land routes between countries, by
border crossing, using live country/border data fetched from GitHub.

## What it does

`GET /routing/{origin}/{destination}` returns the shortest land route
between two countries, identified by their ISO 3166-1 alpha-3 (`cca3`) codes.

```
GET /routing/CZE/ITA

200 OK
{"route": ["CZE", "AUT", "ITA"]}
```

If no land route exists, or either code isn't recognized, the endpoint
returns `400 Bad Request` with an error body:

```
GET /routing/CZE/ABW

400 Bad Request
{"error": "No land route exists from CZE to ABW"}
```

If the upstream country data can't be fetched at all (network failure,
malformed response), the endpoint returns `503 Service Unavailable` instead
— this is a different failure mode from "no route exists for this specific
pair," so it gets a different status code.

## Prerequisites

- **Java 21**
- **Maven** (any reasonably current version; built and tested with Maven 3.9.x)

## Building and running

Clone the repository, then from the project root:

```bash
mvn spring-boot:run
```

The service starts on port 8080 by default. Try it:

```bash
curl http://localhost:8080/routing/CZE/ITA
```

The country/border data is fetched live from
[`mledoze/countries`](https://raw.githubusercontent.com/mledoze/countries/master/countries.json)
on first request and cached in memory for the lifetime of the application
(see [Caching](#data-fetching-and-caching) below) — an internet connection
is required.

## Running the tests

### Unit tests only (fast, no network)

```bash
mvn test
```

### Full suite including integration tests (requires network)

```bash
mvn verify
```

Integration tests (the `*IT` classes) run under Maven Failsafe in the
`integration-test` phase. They hit the live GitHub data source and require
an internet connection. `mvn test` deliberately skips them so the fast,
offline feedback loop isn't blocked on network availability.

### Test coverage

`mvn test` (or `mvn verify`, which runs the `test` phase first) generates a
JaCoCo coverage report at `target/site/jacoco/index.html`. Open it in any
browser — no IDE required. (If you're working in IntelliJ, its built-in
"Run with Coverage" action is a faster way to check coverage interactively
while iterating; the Maven command is the tool-independent path.)

## API contract

| | |
|---|---|
| Endpoint | `GET /routing/{origin}/{destination}` |
| Country codes | ISO 3166-1 alpha-3 (`cca3`), e.g. `CZE`, `ITA` — case-insensitive |
| Success | `200 OK`, `{"route": [...]}`, origin and destination inclusive |
| No route / unknown code | `400 Bad Request`, `{"error": "..."}` |
| Upstream data unavailable | `503 Service Unavailable`, `{"error": "..."}` |

A few specifics worth knowing:

- **If origin equals destination**, the route is the single-element list
  containing just that code (e.g. `routing/CZE/CZE` → `{"route": ["CZE"]}`).
- **Malformed codes** (wrong length, non-letters) and **well-formed but
  unrecognized codes** (valid shape, not a real country in the dataset)
  both return `400`, per the spec's literal wording — the spec doesn't ask
  for a richer status scheme distinguishing these cases, so this doesn't
  invent one. The error *message* differs between the two internally, even
  though the status code doesn't.
- **Border data is directional.** The source data does not guarantee that
  if country A lists country B as a border-neighbor, B lists A back — see
  [Border directionality](#border-directionality) below. A returned route
  is only guaranteed valid in the direction requested.

## Architecture

```
Controller          Service               Model
RoutingController -> RoutingService     -> Borders / IsoCode   (domain)
                      CountryDataService -> Country             (model/in)
                                            RouteResponse,
                                            ErrorResponse        (model/out)
```

Packages are organized by role (`controller`, `service`, `model`,
`exception`) rather than by feature, since this is a single-feature service
— a feature-based split wouldn't have anything to separate from anything
else at this scale.

Within `model`, JSON-in and JSON-out DTOs are kept in separate subpackages
(`model.in`, `model.out`), distinct from genuine domain types (`IsoCode`,
`Borders`) at the package root. This is a deliberate, structural separation,
not just naming convention: it's easy to accidentally leak a deserialization
DTO straight out through a controller, or vice versa, if everything sits in
one flat package with nothing to distinguish "this flows in" from "this
flows out" — the directional subpackages make that distinction visible at
the package level rather than relying on a reviewer noticing it.

### Why BFS

The spec asks for an "efficient" algorithm. Read as two things, not one:

1. **Computationally efficient** — BFS runs in O(V + E) over the border
   graph, trivial at this dataset's actual scale (~250 countries, a similar
   number of border relationships).
2. **Efficient for the traveler** — BFS over an unweighted graph naturally
   returns the route with the *fewest border crossings*, not just any valid
   route. A border crossing is a border crossing; this codebase has no
   notion of one being "shorter" than another, so there's no reason to
   reach for a weighted-graph algorithm like Dijkstra's.

### Border directionality

The underlying dataset's border relationships are not guaranteed to be
mutual. Concrete, verified example: Sri Lanka (`LKA`) lists India (`IND`) as
a border-neighbor in the source data, but India's own border list does
**not** include Sri Lanka back. (Sri Lanka and India aren't even
land-connected — they're separated by the Palk Strait — so this looks like
a quirk in the upstream dataset rather than a deliberate statement about a
real border. The root cause doesn't matter for correctness: whatever the
reason, the application is built to preserve this kind of asymmetry rather
than silently "fixing" it.)

`Borders` is modeled as a **directed** graph specifically because of this:
it never symmetrizes or assumes that a listed relationship is mutual, and
the routing algorithm only ever follows a border in the direction the data
actually states.

### Data fetching and caching

Country/border data is fetched live from GitHub on first request, via
Spring's `RestClient`. One real wrinkle: GitHub's raw-content host always
serves files with `Content-Type: text/plain`, regardless of actual content
— this is documented, deliberate GitHub behavior, not a bug — so the
`RestClient` here is explicitly configured to also accept `text/plain` as
valid JSON input, scoped to this one client.

The fetched data is transformed into an immutable `Borders` graph and
cached via `@Cacheable` (`spring-boot-starter-cache`, in-memory,
`ConcurrentHashMap`-backed — no database). Only the immutable `Borders`
instance is ever cached; the raw deserialized list and intermediate map
used to build it are local to one method and never shared, so there's
nothing mutable for concurrent requests to accidentally corrupt.

`Borders`' immutability is enforced structurally, not just by convention:
its constructor defensively deep-copies both the outer map and every
per-country neighbor set. (`Map.copyOf` alone is not sufficient for this —
it protects the outer map's structure but does not deep-copy nested
collection values, which would otherwise let a caller retaining a
reference to the original input mutate it after construction, leaking
through into the supposedly-immutable cached instance. A failing test
caught exactly this during development, before the fix was added.)

A failed fetch (network error, non-2xx response, malformed JSON) throws a
dedicated exception mapped to `503`, distinct from "no route found" for a
specific pair (`400`). Spring's `@Cacheable` does not cache thrown
exceptions, so a transient upstream failure doesn't permanently break the
service — the next request simply retries the fetch.

## Testing strategy

Tests are layered deliberately, trading speed/determinism against fidelity
to the real, live dataset:

- **Unit tests** (`IsoCodeTest`, `BordersTest`, `CountryDeserializationTest`)
  — no Spring context, no network. Cover validation logic, immutability
  guarantees (including the mutation-leak case above), and JSON
  (de)serialization mechanics in isolation.
- **`RoutingServiceTest`** — BFS correctness against a small, hand-built,
  deliberately *fictional* graph (fairytale-themed codes like `HOG`/`GON`,
  rather than real country codes). Fictional codes make it visually
  unmistakable that this fixture is synthetic, not a stand-in for real
  data, while still exercising shortest-path selection, the
  origin-equals-destination case, disconnected nodes, unknown codes, and
  directional asymmetry.
- **`RoutingServiceRealDataTest`** — the same algorithm, but against a
  hand-verified real subset of the actual CZE/AUT/ITA-area border data,
  proving the spec's literal example holds against real-world adjacency
  facts, without needing the network.
- **`RoutingServiceLiveIT`** / **`CountryDataServiceLiveFetchIT`** /
  **`CountryDataServiceFetchFailureIT`** — no mocking anywhere: real GitHub
  fetch, real deserialization, real BFS. Slower and dependent on
  network/GitHub availability, but the only tests proving the system works
  against the *live*, current upstream dataset. Run under Failsafe
  (`mvn verify`), not Surefire, so they don't block the fast offline loop.
- **`RoutingControllerMockMvcTest`** — the HTTP/controller/exception-handler
  layer, via `MockMvc`, with `RoutingService` mocked. Proves
  `RoutingController` and `GlobalExceptionHandler` are wired correctly
  without needing a real network call or a real algorithm run.
- **`RoutingControllerIT`** — the complete system: a real embedded server,
  real HTTP requests, nothing mocked anywhere in the chain. Includes a
  non-percolation case (France → Argentina) that's meaningfully stronger
  than a simple disconnected-island check: both countries are real,
  richly-connected nodes in large connected components, so this exercises
  BFS actually exhausting a large search space before correctly concluding
  no route exists, rather than the trivial case of a node with zero
  neighbors at all. Runs under Failsafe (`mvn verify`).

Run `mvn test` for the fast offline suite, `mvn verify` for everything.