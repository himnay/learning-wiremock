package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static com.learnwiremock.constants.MovieAppConstants.*;

@Slf4j
@RequiredArgsConstructor
public class MoviesRestClient {

    private final WebClient webClient;

    /**
     * GET all movies
     *
     * @return
     */
    public List<Movie> retrieveAllMovies() {
        try {
            return webClient
                    .get()
                    .uri(GET_ALL_MOVIES_V1)
                    .retrieve()                 // actual call is made to the api
                    .bodyToFlux(Movie.class)    //body is converted to flux(Represents multiple items)
                    .collectList()              // collecting the httpResponse as a list
                    .block();                   // This call makes the Webclient to behave as a synchronous client.
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException - Error Message is : {} ", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * GET a movie
     *
     * @param movieId
     * @return
     */
    public Movie retrieveMovieById(Integer movieId) {
        try {
            return webClient
                    .get()
                    .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)     //mapping the movie id to the url
                    .retrieve()
                    .bodyToMono(Movie.class)                    //body is converted to Mono(Represents single item)
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException - Error Message is : {} ", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * GET list of movies by name
     *
     * @param movieName
     * @return
     */
    public List<Movie> retrieveMovieByName(String movieName) {

        String retrieveByNameUri = UriComponentsBuilder
                .fromUriString(MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movie_name", movieName)
                .buildAndExpand()
                .toUriString();

        try {
            return webClient.get().uri(retrieveByNameUri)
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException in retrieveMovieByName - Error Message is : {} ", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * GET list of movies based on year
     *
     * @param year - Integer (Example : 2012,2013 etc.,)
     * @return - List<Movie>
     */
    public List<Movie> retrieveMovieByYear(Integer year) {
        String retrieveByYearUri = UriComponentsBuilder
                .fromUriString(MOVIE_BY_YEAR_QUERY_PARAM_V1)
                .queryParam("year", year)
                .buildAndExpand()
                .toUriString();

        try {
            return webClient
                    .get()
                    .uri(retrieveByYearUri)
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException in retrieveMovieByYear() - Error Message is : {} ", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * POST a movie
     *
     * @param newMovie
     * @return
     */
    public Movie addNewMovie(Movie newMovie) {
        try {
            return webClient
                    .post()
                    .uri(ADD_MOVIE_V1)
                    .syncBody(newMovie)
                    .retrieve()
                    .bodyToMono(Movie.class)
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException - Error Message is : {} , and the Error Response Body is {}", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * PUT a movie
     *
     * @param movieId
     * @param movie
     * @return
     */
    public Movie updateMovie(Integer movieId, Movie movie) {
        try {
            return webClient
                    .put()
                    .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                    .syncBody(movie)
                    .retrieve()
                    .bodyToMono(Movie.class)
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException - Error Message is : {}", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }

    /**
     * DELETE a movie
     */
    public String deleteMovieById(Integer movieId) {
        try {
            return webClient
                    .delete()
                    .uri(MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException webClientResponseException) {
            log.error("WebClientResponseException - Error Message is : {}", webClientResponseException, webClientResponseException.getResponseBodyAsString());
            throw new MovieErrorResponseException(webClientResponseException.getStatusText(), webClientResponseException);
        } catch (Exception exception) {
            log.error("Exception - The Error Message is {} ", exception.getMessage());
            throw new MovieErrorResponseException(exception);
        }
    }
}
