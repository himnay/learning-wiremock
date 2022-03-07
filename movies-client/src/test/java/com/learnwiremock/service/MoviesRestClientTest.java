package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(WireMockExtension.class)
class MoviesRestClientTest {

    private MoviesRestClient moviesRestClient;
    private WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    public void init() {
        String baseUrl = String.format("http://localhost:%s/", wireMockServer.port());
        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @AfterEach
    public void cleanup() {
    }

    @Test
    @DisplayName("GET all movies")
    void retrieveAllMoviesTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("all-movies.json");

//        stubFor(get(anyUrl())
//                .willReturn(response));

        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(response));

        var movies = moviesRestClient.retrieveAllMovies();

        assertNotNull(movies);
        assertTrue(movies.size() == 10);
        movies.forEach(movie -> assertNotNull(movie.getMovie_id()));
    }

    @Test
    @DisplayName("GET a movies by its id")
    void retrieveMovieByIdTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("movie.json");

        int movieId = 1;
        stubFor((get(urlPathMatching("/movieservice/v1/movie/[0-9]")))
                .willReturn(response));


        var movie = moviesRestClient.retrieveMovieById(movieId);

        assertEquals(movie.getName(), "Batman Begins");
        assertEquals(movie.getYear(), 2005);
        assertEquals(movie.getCast(), "Christian Bale, Katie Holmes , Liam Neeson");
    }

    @Test
    @DisplayName("GET a movies by its id")
    void retrieveMovieByIdWithDynamicResponseTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("movie.json");

        int movieId = 9;
        stubFor((get(urlPathMatching("/movieservice/v1/movie/[0-9]")))
                .willReturn(response));

        var movie = moviesRestClient.retrieveMovieById(movieId);

        assertEquals(movie.getMovie_id(), 9L);
        assertEquals(movie.getName(), "Batman Begins");
        assertEquals(movie.getYear(), 2005);
        assertEquals(movie.getCast(), "Christian Bale, Katie Holmes , Liam Neeson");
    }

    @Test
    @DisplayName("GET a movies by its id when not found")
    void retrieveMovieByIdNotFoundTest() {
        var response = aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("404-movieid.json");

        stubFor((get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))) // + support any no of character 2digit, 3digit...
                .willReturn(response));

        assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveMovieById(new Random().nextInt(1000)), "MovieErrorResponseException is expected");
    }

    @Test
    @DisplayName("GET movie by name")
    void retrieveMovieByNameTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("avengers.json");

        String movieName = "Avengers";
//        stubFor((get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name=" + movieName)))
//                .willReturn(response));

        stubFor((get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                    .withQueryParam("movie_name", equalTo(movieName)))
                    .willReturn(response));

        var movies = moviesRestClient.retrieveMovieByName(movieName);

        assertEquals(movies.size(), 4);

        movies.forEach(movie -> assertTrue(movie.getName().contains("Avengers")));
    }

    @Test
    @DisplayName("GET movie by name")
    void retrieveMovieByNameUsingNameTemplateTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("movie-byName-template.json");

        String movieName = "Avengers";
//        stubFor((get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name=" + movieName)))
//                .willReturn(response));

        stubFor((get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo(movieName)))
                .willReturn(response));

        var movies = moviesRestClient.retrieveMovieByName(movieName);

        assertEquals(movies.size(), 4);

        movies.forEach(movie -> assertTrue(movie.getName().contains("Avengers")));
    }

    @Test
    @DisplayName("GET movie by year")
    void retrieveMovieByYearTest() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("movies-byYear.json");

        int year = 2012;

        stubFor((get(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1))
                .withQueryParam("year", equalTo("2012")))
                .willReturn(response));

        List<Movie> movies = moviesRestClient.retrieveMovieByYear(year);

        assertEquals(movies.size(), 2);
        movies.forEach(movie -> assertEquals(2012, movie.getYear()));
    }

    @Test
    @DisplayName("GET movie by year NOT FOUND")
    void retrieveMovieByYearNotFoundTest() {
        var response = aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("404-movieyear.json");

        int year = 2050;

        stubFor((get(urlPathEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1))
                .withQueryParam("year", equalTo("2050")))
                .willReturn(response));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveMovieByYear(year), "MovieErrorResponseException is expected");
        assertEquals("Not Found", exception.getMessage());
    }

    @Test
    @DisplayName("POST movie https://wiremock.org/docs/request-matching/")
    void addNewMovie() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("add-movie.json");

        stubFor((post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Eternals"))) // verify if field name exists and value matches
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Salma"))) // verify if field cast exists and value matches
                .willReturn(response)));

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(12L, "Eternals", 2021, "Angelina Jolie, Salma Hayek, Gemma Chan", releaseDate);

        Movie movieCreated = moviesRestClient.addNewMovie(movie);

        assertNotNull(movieCreated);
        assertEquals(movieCreated.getName(), "Eternals");
        assertEquals(movieCreated.getYear(), 2021);
        assertEquals(movieCreated.getCast(), "Angelina Jolie, Salma Hayek, Gemma Chan");
    }

    @Test
    @DisplayName("POST movie")
    void addNewMovieUsingDynamicResponseTemplate() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("add-movie-byTemplate.json");

        stubFor((post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Eternals"))) // verify if field name exists and value matches
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Salma"))) // verify if field cast exists and value matches
                .willReturn(response)));

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(12L, "Eternals", 2021, "Angelina Jolie, Salma Hayek, Gemma Chan", releaseDate);

        Movie movieCreated = moviesRestClient.addNewMovie(movie);

        assertNotNull(movieCreated);
        assertEquals(movieCreated.getName(), "Eternals");
        assertEquals(movieCreated.getYear(), 2021);
        assertEquals(movieCreated.getCast(), "Angelina Jolie, Salma Hayek, Gemma Chan");
    }

    @Test
    @DisplayName("POST movie with missing name")
    void addNewMovieUsingWithMissingName() {
        var response = aResponse()
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("400-createMovie-badRequest.json");

        stubFor((post(urlPathEqualTo(ADD_MOVIE_V1))
                .willReturn(response)));

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(12L, "", 2021, "Angelina Jolie, Salma Hayek, Gemma Chan", releaseDate);

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.addNewMovie(movie), "MovieErrorResponseException is expected");
        assertEquals("Bad Request", exception.getMessage());
    }

    @Test
    @DisplayName("PUT movie")
    void updateMovie() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("updateMovie.json");

        stubFor((put(urlPathMatching("/movieservice/v1/movie/[0-9]+")))
                .willReturn(response));

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(11L, "Eternals", 2021, "Lia McHugh", releaseDate);

        Movie movieCreated = moviesRestClient.updateMovie(11, movie);

        assertNotNull(movieCreated);
        assertEquals(movieCreated.getMovie_id(), 11L);
        assertEquals(movieCreated.getName(), "Eternals");
        assertEquals(movieCreated.getYear(), 2021);
        assertEquals(movieCreated.getCast(), "Angelina Jolie, Salma Hayek, Gemma Chan, Lia McHugh");
    }

    @Test
    @DisplayName("PUT movie")
    void updateMovieWithWrongMovieID() {
        var response = aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("updateMovie.json");

        stubFor((put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("ABC")))
                .willReturn(response))
        );

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(11L, "Eternals", 2021, "ABC", releaseDate);

        MovieErrorResponseException movieErrorResponseException = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.updateMovie(22, movie), "MovieErrorResponse was expected");
        assertEquals("Not Found", movieErrorResponseException.getMessage());
    }

    @Test
    @DisplayName("DELETE movie")
    void deleteMovieById() {
        var response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("add-movie-byTemplate.json");

        stubFor((post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Eternals"))) // verify if field name exists and value matches
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Salma"))) // verify if field cast exists and value matches
                .willReturn(response)));

        LocalDate releaseDate = LocalDate.of(2021, 10, 18);
        Movie movie = new Movie(15L, "Eternals", 2021, "Angelina Jolie, Salma Hayek, Gemma Chan", releaseDate);

        moviesRestClient.addNewMovie(movie);

        response = aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBody("Movie Deleted Successfully");

        stubFor((delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(response)));

        var deleteResponse = moviesRestClient.deleteMovieById(15);

        assertEquals(deleteResponse, "Movie Deleted Successfully");
    }

    @Test
    @DisplayName("DELETE movie with a invalid movie id")
    void deleteMovieWithAInvalidMovieId() {
        var response = aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("404-movieid.json");

        stubFor((delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(response)));

        MovieErrorResponseException movieErrorResponseException = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.deleteMovieById(100), "MovieErrorResponse exception expected");
        assertEquals("Not Found", movieErrorResponseException.getMessage());
    }

}