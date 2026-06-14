package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.function.Supplier;

import static com.learnwiremock.constants.MovieAppConstants.*;

/**
 * Template Method (GoF) — {@code executeRequest} captures the invariant
 * error-handling logic; each public method provides the variant HTTP call.
 * <p>
 * Singleton (GoF) — registered as a Spring {@code @Service} bean so
 * only one instance exists in the application context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoviesRestClient {

    private final WebClient webClient;

    // ── Template Method ────────────────────────────────────────────────────────

    private <T> T executeRequest(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (WebClientResponseException ex) {
            log.error("HTTP error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new MovieErrorResponseException(ex.getStatusText(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error: {}", ex.getMessage());
            throw new MovieErrorResponseException(ex);
        }
    }

    // ── CRUD operations ────────────────────────────────────────────────────────

    public List<Movie> retrieveAllMovies() {
        return executeRequest(() -> webClient.get()
                .uri(GET_ALL_MOVIES_V1)
                .retrieve()
                .bodyToFlux(Movie.class)
                .collectList()
                .block());
    }

    public Movie retrieveMovieById(Integer movieId) {
        return executeRequest(() -> webClient.get()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .retrieve()
                .bodyToMono(Movie.class)
                .block());
    }

    public List<Movie> retrieveMovieByName(String movieName) {
        String uri = UriComponentsBuilder.fromUriString(MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movie_name", movieName)
                .buildAndExpand()
                .toUriString();

        return executeRequest(() -> webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Movie.class)
                .collectList()
                .block());
    }

    public List<Movie> retrieveMovieByYear(Integer year) {
        String uri = UriComponentsBuilder.fromUriString(MOVIE_BY_YEAR_QUERY_PARAM_V1)
                .queryParam("year", year)
                .buildAndExpand()
                .toUriString();

        return executeRequest(() -> webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Movie.class)
                .collectList()
                .block());
    }

    public Movie addNewMovie(Movie newMovie) {
        return executeRequest(() -> webClient.post()
                .uri(ADD_MOVIE_V1)
                .bodyValue(newMovie)
                .retrieve()
                .bodyToMono(Movie.class)
                .block());
    }

    public Movie updateMovie(Integer movieId, Movie movie) {
        return executeRequest(() -> webClient.put()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .bodyValue(movie)
                .retrieve()
                .bodyToMono(Movie.class)
                .block());
    }

    public String deleteMovieById(Integer movieId) {
        return executeRequest(() -> webClient.delete()
                .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                .retrieve()
                .bodyToMono(String.class)
                .block());
    }

    public String deleteMovieByName(String movieName) {
        String uri = UriComponentsBuilder.fromUriString(MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movieName", movieName)
                .buildAndExpand()
                .toUriString();

        executeRequest(() -> webClient.delete()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block());

        return "movie " + movieName + " deleted successfully";
    }
}
