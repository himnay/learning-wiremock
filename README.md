# Learning WireMock

Spring Boot multi-module Maven project for learning [WireMock 3.x](https://wiremock.org/) — HTTP service virtualisation for testing REST clients.

## Modules

| Module | Description |
|---|---|
| `movies-client` | REST client for the movies service, with full WireMock test suite |

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| HTTP client | Spring WebFlux `WebClient` |
| Mocking | WireMock 3.x (via `spring-cloud-contract-wiremock`) |
| Testing | JUnit 5 · TestContainers |
| Observability | Spring Actuator · Micrometer · Prometheus · Grafana |
| Build | Maven 3.9 (parent: `super-pom`) |

## Project structure

```
learning-wiremock/
├── pom.xml                           # aggregator (packaging=pom)
├── docker-compose.yml                # movies-service + Prometheus + Grafana
├── observability/
│   └── prometheus.yml
├── insomnia-collection.json          # import into Insomnia to test the movies service
├── movies-restful-service/           # pre-built JARs of the movies REST API (port 8081)
│   ├── movies-restful-service-beyond-java8.jar
│   └── movies-restful-service-java8.jar
└── movies-client/
    ├── pom.xml
    └── src/
        ├── main/java/com/learnwiremock/
        │   ├── LearnWiremockApplication.java
        │   ├── config/
        │   │   ├── MoviesClientProperties.java   # @ConfigurationProperties record
        │   │   └── WebClientConfig.java          # WebClient factory bean (Factory Method)
        │   ├── constants/MovieAppConstants.java
        │   ├── dto/Movie.java                    # Lombok @Builder
        │   ├── exception/MovieErrorResponseException.java
        │   └── service/MoviesRestClient.java     # Template Method (GoF)
        ├── main/resources/
        │   ├── application.yml
        │   ├── banner.txt
        │   └── logback-spring.xml
        └── test/
            ├── java/com/learnwiremock/service/
            │   ├── MoviesRestClientTest.java           # happy-path + error stubs
            │   └── MoviesRestClientServerErrorTest.java # faults + timeouts
            └── resources/__files/                      # WireMock response body files
```

## Running everything

### Start movies-service + observability stack

```bash
docker compose up -d
```

The movies REST service starts on **port 8081** via Docker (Java 21 image, `beyond-java8` JAR).

| Service | URL |
|---|---|
| Movies API | `http://localhost:8081/movieservice/v1/allMovies` |
| Swagger UI | `http://localhost:8081/movieservice/swagger-ui.html` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` (admin / admin) |

### Start the movies-client Spring Boot app

```bash
cd movies-client
mvn spring-boot:run
```

App starts on **port 8083** and connects to the movies service at `http://localhost:8081`.

| Endpoint | URL |
|---|---|
| Health | `http://localhost:8083/actuator/health` |
| Info | `http://localhost:8083/actuator/info` |
| Prometheus metrics | `http://localhost:8083/actuator/prometheus` |

### Run the tests

```bash
# all modules
mvn test

# movies-client only
mvn test -pl movies-client
```

WireMock starts on a **random port** per test class — no port conflicts, parallel-safe.

---

## WireMock 3.x — features covered

### 1. JUnit 5 native extension (best practice)

Use `WireMockExtension` with `@RegisterExtension` — replaces the legacy JUnit 4 `@Rule WireMockRule`.

```java
@RegisterExtension
static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
                .dynamicPort()                  // random port — no conflicts
                .notifier(new ConsoleNotifier(false))
                .templatingEnabled(true)        // WireMock 3.x built-in templating
                .globalTemplating(true))        // apply to every stub automatically
        .build();
```

> **WireMock 3.x note:** Do NOT manually register `ResponseTemplateTransformer` via `.extensions()`.
> Use `.templatingEnabled(true).globalTemplating(true)` — it is built in.

### 2. URL & method matching

```java
// exact path
wireMock.stubFor(get(urlPathEqualTo("/movieservice/v1/allMovies"))...);

// regex path
wireMock.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))...);

// query parameter
wireMock.stubFor(get(urlPathEqualTo("/movieservice/v1/movieByName"))
        .withQueryParam("movie_name", equalTo("Avengers"))...);
```

### 3. Request body matching

```java
wireMock.stubFor(post(urlPathEqualTo("/movieservice/v1/movie"))
        .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals")))
        .withRequestBody(matchingJsonPath("$.cast", containing("Salma")))...);
```

### 4. Response body from file (`__files/`)

Files in `src/test/resources/__files/` are served as-is.

```java
wireMock.stubFor(get(urlPathEqualTo("/movieservice/v1/allMovies"))
        .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("all-movies.json")));
```

### 5. Response templates (Handlebars)

With `.templatingEnabled(true).globalTemplating(true)`, any `{{...}}` in a body file is evaluated:

| Expression | What it resolves to |
|---|---|
| `{{request.path.[3]}}` | 4th path segment (e.g. the `{id}` in `/movie/9`) |
| `{{request.query.movie_name.[0]}}` | First value of query param `movie_name` |
| `{{jsonPath request.body '$.name'}}` | JSON field from the request body |
| `{{randomValue length=10 type='ALPHANUMERIC'}}` | Random alphanumeric string |

```json
// movie.json — injects path segment as movie_id
{ "movie_id": {{request.path.[3]}}, "name": "Batman Begins" }

// add-movie-byTemplate.json — echoes request body fields
{ "movie_id": {{randomValue length=5 type='NUMERIC'}},
  "name": "{{jsonPath request.body '$.name'}}" }
```

### 6. HTTP error simulation

```java
wireMock.stubFor(get(anyUrl()).willReturn(serverError()));          // 500
wireMock.stubFor(get(anyUrl()).willReturn(serviceUnavailable()));   // 503
wireMock.stubFor(get(anyUrl()).willReturn(aResponse()
        .withStatus(404).withBodyFile("404-movieid.json")));
```

### 7. Network fault simulation

```java
wireMock.stubFor(get(anyUrl())
        .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));          // TCP close before response
wireMock.stubFor(get(anyUrl())
        .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));  // garbled bytes + close
```

### 8. Latency & timeout simulation

```java
// fixed delay
wireMock.stubFor(get(anyUrl()).willReturn(aResponse().withFixedDelay(10_000)));   // 10 s

// random delay in range
wireMock.stubFor(get(anyUrl()).willReturn(aResponse().withUniformRandomDelay(6_000, 10_000)));
```

Pair with a client-side `responseTimeout` to verify timeout handling:

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofSeconds(5));
```

### 9. Selective proxying

Forward everything to the real service, then override specific paths with stubs:

```java
// pass-through proxy
wireMock.stubFor(any(anyUrl())
        .willReturn(aResponse().proxiedFrom("http://localhost:8081")));

// override just one endpoint
wireMock.stubFor(get(urlPathEqualTo("/movieservice/v1/movie/1"))
        .willReturn(aResponse().withBodyFile("movie.json")));
```

### 10. Request verification (WireMock spy)

Assert that your client actually made the expected HTTP calls:

```java
wireMock.verify(exactly(1), postRequestedFor(urlPathEqualTo("/movieservice/v1/movie"))
        .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals2"))));

wireMock.verify(exactly(1),
        deleteRequestedFor(urlEqualTo("/movieservice/v1/movieByName?movieName=Eternals2")));
```

---

## WireMock 3.x best practices

| Practice | Why |
|---|---|
| `@RegisterExtension static` | Shares one WireMock server per test class; resets stubs between tests |
| `.dynamicPort()` | Avoids port conflicts in parallel test execution |
| `.templatingEnabled(true).globalTemplating(true)` | WireMock 3.x built-in — no manual `ResponseTemplateTransformer` needed |
| Instance `wireMock.stubFor()` not static `stubFor()` | Scoped to the extension; static method uses a global client and causes confusion in multi-server setups |
| Body files in `__files/` | Separates test data from test code; reusable across stubs |
| `withBodyFile()` over inline `.withBody()` | Cleaner for large JSON; supports Handlebars templating from the file |
| `wireMock.verify()` after the act | Adds spy-level assurance that the client actually called the stub |
| Use `matchingJsonPath` for partial request matching | More resilient than full JSON equality; tolerates field ordering |

---

## Design patterns used

| Pattern | Where |
|---|---|
| **Template Method** (GoF) | `MoviesRestClient.executeRequest()` — invariant error handling, variant HTTP calls |
| **Builder** (GoF) | `Movie` via Lombok `@Builder` |
| **Factory Method** (GoF) | `WebClientConfig.moviesWebClient()` — constructs the `WebClient` bean |
| **Singleton** (GoF) | All `@Service` / `@Configuration` Spring beans |

---

## Observability (Prometheus + Grafana)

In Grafana:
1. Add datasource → Prometheus → `http://prometheus:9090`
2. Import dashboard **4701** (JVM Micrometer)

Prometheus scrapes `host.docker.internal:8083/actuator/prometheus` every 10 s.
