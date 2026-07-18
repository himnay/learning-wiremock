package com.learnwiremock.movies.service;

import com.learnwiremock.movies.domain.Movie;
import com.learnwiremock.movies.exception.MovieNotFoundException;
import com.learnwiremock.movies.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository repository;

    public List<Movie> getAllMovies() {
        var movies = repository.findAll();
        log.debug("Retrieved {} movies", movies.size());
        return movies;
    }

    public Movie getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Movie not found: id={}", id);
                    return new MovieNotFoundException("No Movie Available with the given Id - " + id);
                });
    }

    public List<Movie> getByName(String name) {
        List<Movie> movies = repository.findByNameContaining(name);
        if (movies.isEmpty()) {
            log.warn("No movies found for name='{}'", name);
            throw new MovieNotFoundException("No Movie Available with the given name - " + name);
        }
        log.debug("Found {} movies matching name='{}'", movies.size(), name);
        return movies;
    }

    public List<Movie> getByYear(Integer year) {
        List<Movie> movies = repository.findByYear(year);
        if (movies.isEmpty()) {
            log.warn("No movies found for year={}", year);
            throw new MovieNotFoundException("No Movie Available with the given year - " + year);
        }
        log.debug("Found {} movies for year={}", movies.size(), year);
        return movies;
    }

    /** Adds movie. */
    public Movie addMovie(Movie movie) {
        Movie saved = repository.save(movie);
        log.info("Created movie: id={}, name='{}'", saved.getMovie_id(), saved.getName());
        return saved;
    }

    /** Updates movie. */
    public Movie updateMovie(Long id, Movie movie) {
        Movie existing = getById(id);
        existing.setName(movie.getName() != null ? movie.getName() : existing.getName());
        existing.setYear(movie.getYear() != null ? movie.getYear() : existing.getYear());
        existing.setCast(movie.getCast() != null ? movie.getCast() : existing.getCast());
        existing.setRelease_date(movie.getRelease_date() != null ? movie.getRelease_date() : existing.getRelease_date());
        Movie updated = repository.save(existing);
        log.info("Updated movie: id={}, name='{}'", updated.getMovie_id(), updated.getName());
        return updated;
    }

    /** Deletes by id. */
    public String deleteById(Long id) {
        if (!repository.deleteById(id)) {
            log.warn("Delete failed — movie not found: id={}", id);
            throw new MovieNotFoundException("No Movie Available with the given Id - " + id);
        }
        log.info("Deleted movie: id={}", id);
        return "Movie Deleted Successfully";
    }

    /** Deletes by name. */
    public String deleteByName(String name) {
        if (!repository.deleteByName(name)) {
            log.warn("Delete failed — no movie found for name='{}'", name);
            throw new MovieNotFoundException("No Movie Available with the given name - " + name);
        }
        log.info("Deleted movie(s) by name='{}'", name);
        return "Movie Deleted Successfully";
    }
}
