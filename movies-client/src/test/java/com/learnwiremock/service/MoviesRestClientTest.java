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
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock 3.x — uses WireMock's own JUnit 5 {@link WireMockExtension}
 * (replaces the legacy jenspiegsa extension).
 * <p>
 * ResponseTemplateTransformer is registered globally so every stub that
 * references {@code {{...}}} Handlebars expressions is handled automatically.
 */
class MoviesRestClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
                    .templatingEnabled(true)    // enable WireMock 3.x built-in response templating
                    .globalTemplating(true))    // apply templates to every stub automatically
            .build();

    private MoviesRestClient moviesRestClient;

    @BeforeEach
    void setup() {
        WebClient webClient = WebClient.create(wireMock.baseUrl());
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    @DisplayName("GET all movies")
    void retrieveAllMoviesTest() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        var movies = moviesRestClient.retrieveAllMovies();

        assertNotNull(movies);
        assertEquals(10, movies.size());
        movies.forEach(movie -> assertNotNull(movie.getMovie_id()));
    }

    @Test
    @DisplayName("GET a movie by id")
    void retrieveMovieByIdTest() {
        wireMock.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        var movie = moviesRestClient.retrieveMovieById(1);

        assertEquals("Batman Begins", movie.getName());
        assertEquals(2005, movie.getYear());
        assertEquals("Christian Bale, Katie Holmes , Liam Neeson", movie.getCast());
    }

    @Test
    @DisplayName("GET a movie by id — response template injects path param as movie_id")
    void retrieveMovieByIdWithDynamicResponseTest() {
        wireMock.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        var movie = moviesRestClient.retrieveMovieById(9);

        assertEquals(9L, movie.getMovie_id());
        assertEquals("Batman Begins", movie.getName());
    }

    @Test
    @DisplayName("GET a movie by id — 404 not found")
    void retrieveMovieByIdNotFoundTest() {
        wireMock.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieid.json")));

        assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveMovieById(new Random().nextInt(1000)),
                "MovieErrorResponseException expected for 404");
    }

    @Test
    @DisplayName("GET movies by name")
    void retrieveMovieByNameTest() {
        wireMock.stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo("Avengers"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        var movies = moviesRestClient.retrieveMovieByName("Avengers");

        assertEquals(4, movies.size());
        movies.forEach(movie -> assertTrue(movie.getName().contains("Avengers")));
    }

    @Test
    @DisplayName("GET movies by name — response template injects query param into movie names")
    void retrieveMovieByNameUsingTemplateTest() {
        wireMock.stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo("Avengers"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-byName-template.json")));

        var movies = moviesRestClient.retrieveMovieByName("Avengers");

        assertEquals(4, movies.size());
        movies.forEach(movie -> assertTrue(movie.getName().contains("Avengers")));
    }

    @Test
    @DisplayName("GET movies by year")
    void retrieveMovieByYearTest() {
        wireMock.stubFor(get(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1))
                .withQueryParam("year", equalTo("2012"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movies-byYear.json")));

        var movies = moviesRestClient.retrieveMovieByYear(2012);

        assertEquals(2, movies.size());
        movies.forEach(movie -> assertEquals(2012, movie.getYear()));
    }

    @Test
    @DisplayName("GET movies by year — 404 not found")
    void retrieveMovieByYearNotFoundTest() {
        wireMock.stubFor(get(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1))
                .withQueryParam("year", equalTo("2050"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieyear.json")));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveMovieByYear(2050),
                "MovieErrorResponseException expected for 404");
        assertEquals("Not Found", ex.getMessage());
    }

    @Test
    @DisplayName("POST add new movie — static stub body")
    void addNewMovieTest() {
        wireMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals")))
                .withRequestBody(matchingJsonPath("$.cast", containing("Salma")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        Movie movie = new Movie(12L, "Eternals", 2021,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2021, 10, 18));

        Movie created = moviesRestClient.addNewMovie(movie);

        assertNotNull(created);
        assertEquals("Eternals", created.getName());
        assertEquals(2021, created.getYear());
    }

    @Test
    @DisplayName("POST add new movie — response template echoes request body fields")
    void addNewMovieDynamicTemplateTest() {
        wireMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals")))
                .withRequestBody(matchingJsonPath("$.cast", containing("Salma")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-byTemplate.json")));

        Movie movie = new Movie(12L, "Eternals", 2021,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2021, 10, 18));

        Movie created = moviesRestClient.addNewMovie(movie);

        assertNotNull(created);
        assertEquals("Eternals", created.getName());
        assertEquals(2021, created.getYear());
    }

    @Test
    @DisplayName("POST add movie with missing name — 400 bad request")
    void addNewMovieMissingNameTest() {
        wireMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("400-createMovie-badRequest.json")));

        Movie movie = new Movie(12L, "", 2021,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2021, 10, 18));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.addNewMovie(movie));
        assertEquals("Bad Request", ex.getMessage());
    }

    @Test
    @DisplayName("PUT update movie")
    void updateMovieTest() {
        wireMock.stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("updateMovie.json")));

        Movie movie = new Movie(11L, "Eternals", 2021, "Lia McHugh", LocalDate.of(2021, 10, 18));

        Movie updated = moviesRestClient.updateMovie(11, movie);

        assertNotNull(updated);
        assertEquals(11L, updated.getMovie_id());
        assertEquals("Eternals", updated.getName());
        assertTrue(updated.getCast().contains("Lia McHugh"));
    }

    @Test
    @DisplayName("PUT update movie — 404 wrong id")
    void updateMovieWrongIdTest() {
        wireMock.stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath("$.cast", containing("ABC")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieid.json")));

        Movie movie = new Movie(11L, "Eternals", 2021, "ABC", LocalDate.of(2021, 10, 18));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.updateMovie(22, movie));
        assertEquals("Not Found", ex.getMessage());
    }

    @Test
    @DisplayName("DELETE movie by id")
    void deleteMovieByIdTest() {
        wireMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Eternals")))
                .withRequestBody(matchingJsonPath("$.cast", containing("Salma")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-byTemplate.json")));

        wireMock.stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("Movie Deleted Successfully")));

        Movie movie = new Movie(15L, "Eternals", 2021,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2021, 10, 18));
        moviesRestClient.addNewMovie(movie);

        var result = moviesRestClient.deleteMovieById(15);
        assertEquals("Movie Deleted Successfully", result);
    }

    @Test
    @DisplayName("DELETE movie by id — 404 invalid id")
    void deleteMovieInvalidIdTest() {
        wireMock.stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieid.json")));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.deleteMovieById(100));
        assertEquals("Not Found", ex.getMessage());
    }

    @Test
    @DisplayName("DELETE movie by name — verifies WireMock call count (spy verification)")
    void deleteMovieByNameTest() {
        String movieName = "Eternals2";

        wireMock.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(movieName)))
                .withRequestBody(matchingJsonPath("$.cast", containing("Salma")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-byTemplate.json")));

        wireMock.stubFor(delete(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movieName=" + movieName))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("Movie Deleted Successfully")));

        Movie movie = new Movie(null, movieName, 2024,
                "Angelina Jolie, Salma Hayek, Gemma Chan", LocalDate.of(2024, 10, 18));
        moviesRestClient.addNewMovie(movie);

        var result = moviesRestClient.deleteMovieByName(movieName);
        assertEquals("movie " + movieName + " deleted successfully", result);

        // WireMock spy — verify exact call counts
        wireMock.verify(exactly(1), postRequestedFor(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(movieName))));
        wireMock.verify(exactly(1),
                deleteRequestedFor(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movieName=" + movieName)));
    }
}
