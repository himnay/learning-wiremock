package com.learnwiremock.service;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates WireMock selective proxying:
 * <ul>
 *   <li>The client talks to {@code proxyMock} (the "API gateway" layer).</li>
 *   <li>Endpoints with a local stub are served directly by {@code proxyMock}.</li>
 *   <li>All other requests are forwarded to {@code realService} (the "real" backend).</li>
 * </ul>
 *
 * This lets tests override only specific routes while relying on the actual
 * service behaviour for everything else — without needing a live deployment.
 */
class MoviesRestClientSelectiveProxyingTest {

    // Simulates the real movies backend (returns actual data for all endpoints)
    @RegisterExtension
    static WireMockExtension realService = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
                    .templatingEnabled(true)
                    .globalTemplating(true))
            .build();

    // The proxy layer: stubs override specific routes; everything else passes through
    @RegisterExtension
    static WireMockExtension proxyMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false)))
            .build();

    private MoviesRestClient moviesRestClient;

    @BeforeEach
    void setup() {
        // Wire up the client to hit the proxy layer
        moviesRestClient = new MoviesRestClient(WebClient.create(proxyMock.baseUrl()));

        // Catch-all: forward unmatched requests to the real backend
        proxyMock.stubFor(any(anyUrl())
                .atPriority(10)                    // lowest priority — only fires when nothing else matches
                .willReturn(aResponse()
                        .proxiedFrom(realService.baseUrl())));
    }

    // ── Proxy pass-through ────────────────────────────────────────────────────

    @Test
    @DisplayName("Proxy pass-through: GET all movies served by the real backend")
    void proxyPassThroughAllMoviesTest() {
        // realService stubs the full list (mirrors production behaviour)
        realService.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        // No stub on proxyMock for this path → falls through to realService
        var movies = moviesRestClient.retrieveAllMovies();

        assertNotNull(movies);
        assertEquals(10, movies.size());
        // Verify the proxy forwarded the request — realService received it
        realService.verify(1, getRequestedFor(urlPathEqualTo(GET_ALL_MOVIES_V1)));
    }

    @Test
    @DisplayName("Proxy pass-through: GET movie by year served by the real backend")
    void proxyPassThroughMovieByYearTest() {
        realService.stubFor(get(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1))
                .withQueryParam("year", equalTo("2012"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movies-byYear.json")));

        var movies = moviesRestClient.retrieveMovieByYear(2012);

        assertEquals(2, movies.size());
        movies.forEach(m -> assertEquals(2012, m.getYear()));
        realService.verify(1, getRequestedFor(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1)));
    }

    // ── Selective override ────────────────────────────────────────────────────

    @Test
    @DisplayName("Selective override: POST add movie stubbed on proxy; GET all movies from real backend")
    void selectiveOverrideAddMovieTest() {
        // Real backend has the movie list
        realService.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        // Proxy overrides POST /movie with its own stub (e.g., to inject a test-specific ID)
        proxyMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .atPriority(1)                    // highest priority — wins over pass-through
                .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        Movie movie = new Movie(12L, "Eternals", 2021,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2021, 10, 18));

        Movie created = moviesRestClient.addNewMovie(movie);
        assertEquals("Eternals", created.getName());

        var allMovies = moviesRestClient.retrieveAllMovies();
        assertEquals(10, allMovies.size());

        // POST was handled by proxy, not forwarded
        realService.verify(0, postRequestedFor(urlPathEqualTo(ADD_MOVIE_V1)));
        // GET was forwarded to real backend
        realService.verify(1, getRequestedFor(urlPathEqualTo(GET_ALL_MOVIES_V1)));
    }

    @Test
    @DisplayName("Selective override: proxy stubs a 404 for a specific movie id; other ids pass through")
    void selectiveOverrideSpecificIdTest() {
        // Real backend serves movie id=1 normally
        realService.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        // Proxy intercepts id=999 specifically and returns 404
        proxyMock.stubFor(get(urlPathEqualTo("/movieservice/v1/movie/999"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieid.json")));

        // id=1 passes through to realService
        Movie movie = moviesRestClient.retrieveMovieById(1);
        assertEquals("Batman Begins", movie.getName());

        // id=999 is intercepted by the proxy stub — throws 404
        assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveMovieById(999));

        // realService never saw the 999 request
        realService.verify(0, getRequestedFor(urlPathEqualTo("/movieservice/v1/movie/999")));
        realService.verify(1, getRequestedFor(urlPathEqualTo("/movieservice/v1/movie/1")));
    }
}
