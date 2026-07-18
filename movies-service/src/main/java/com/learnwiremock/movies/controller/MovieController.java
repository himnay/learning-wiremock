package com.learnwiremock.movies.controller;

import com.learnwiremock.movies.domain.Movie;
import com.learnwiremock.movies.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/movieservice/v1")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/allMovies")
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    @GetMapping("/movie/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieService.getById(id);
    }

    @GetMapping("/movieName")
    public List<Movie> getMoviesByName(@RequestParam("movie_name") String movieName) {
        return movieService.getByName(movieName);
    }

    @GetMapping("/movieYear")
    public List<Movie> getMoviesByYear(@RequestParam Integer year) {
        return movieService.getByYear(year);
    }

    @PostMapping("/movie")
    @ResponseStatus(HttpStatus.CREATED)
    public Movie addMovie(@RequestBody @Valid Movie movie) {
        return movieService.addMovie(movie);
    }

    @PutMapping("/movie/{id}")
    public Movie updateMovie(@PathVariable Long id, @RequestBody Movie movie) {
        return movieService.updateMovie(id, movie);
    }

    @DeleteMapping("/movie/{id}")
    public String deleteMovieById(@PathVariable Long id) {
        return movieService.deleteById(id);
    }

    @DeleteMapping("/movieName")
    public String deleteMovieByName(@RequestParam String movieName) {
        return movieService.deleteByName(movieName);
    }
}
