package com.learnwiremock.movies.service;

import com.learnwiremock.movies.domain.Movie;
import com.learnwiremock.movies.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovieServiceTest {

    private final MovieService service = new MovieService(new MovieRepository());

    @Test
    void addMovieIgnoresAClientSuppliedIdInsteadOfOverwritingThatMovie() {
        Movie created = service.addMovie(Movie.builder().movie_id(1L).name("Eternals").year(2021).build());

        assertThat(created.getMovie_id()).isNotEqualTo(1L);
        assertThat(service.getById(1L).getName()).isEqualTo("Batman Begins");
        assertThat(service.getAllMovies()).hasSize(11);
    }

    @Test
    void aGeneratedIdNeverCollidesWithAnEarlierCreate() {
        Movie first = service.addMovie(Movie.builder().movie_id(11L).name("Eternals").build());
        Movie second = service.addMovie(Movie.builder().name("Dune").build());

        assertThat(second.getMovie_id()).isNotEqualTo(first.getMovie_id());
        assertThat(service.getAllMovies()).hasSize(12);
    }
}
