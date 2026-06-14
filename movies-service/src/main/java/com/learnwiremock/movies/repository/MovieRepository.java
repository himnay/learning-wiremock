package com.learnwiremock.movies.repository;

import com.learnwiremock.movies.domain.Movie;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MovieRepository {

    private final Map<Long, Movie> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(10);

    public MovieRepository() {
        seed().forEach(m -> store.put(m.getMovie_id(), m));
    }

    public List<Movie> findAll() {
        return List.copyOf(store.values());
    }

    public Optional<Movie> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Movie> findByNameContaining(String name) {
        String lower = name.toLowerCase();
        return store.values().stream()
                .filter(m -> m.getName().toLowerCase().contains(lower))
                .toList();
    }

    public List<Movie> findByYear(Integer year) {
        return store.values().stream()
                .filter(m -> year.equals(m.getYear()))
                .toList();
    }

    public Movie save(Movie movie) {
        if (movie.getMovie_id() == null) {
            movie.setMovie_id(idSequence.incrementAndGet());
        }
        store.put(movie.getMovie_id(), movie);
        return movie;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean deleteByName(String name) {
        String lower = name.toLowerCase();
        List<Long> keys = store.values().stream()
                .filter(m -> m.getName().toLowerCase().contains(lower))
                .map(Movie::getMovie_id)
                .toList();
        keys.forEach(store::remove);
        return !keys.isEmpty();
    }

    private List<Movie> seed() {
        return List.of(
                Movie.builder().movie_id(1L).name("Batman Begins").year(2005)
                        .cast("Christian Bale, Katie Holmes , Liam Neeson").release_date(LocalDate.of(2005, 6, 15)).build(),
                Movie.builder().movie_id(2L).name("Dark Knight").year(2008)
                        .cast("Christian Bale, Heath Ledger , Michael Caine").release_date(LocalDate.of(2008, 7, 18)).build(),
                Movie.builder().movie_id(3L).name("The Dark Knight Rises").year(2012)
                        .cast("Christian Bale, Heath Ledger , Michael Caine").release_date(LocalDate.of(2012, 7, 20)).build(),
                Movie.builder().movie_id(4L).name("The Avengers").year(2012)
                        .cast("Robert Downey Jr, Chris Evans , Chris HemsWorth").release_date(LocalDate.of(2012, 5, 4)).build(),
                Movie.builder().movie_id(5L).name("Avengers: Age of Ultron").year(2015)
                        .cast("Robert Downey Jr, Chris Evans , Chris HemsWorth").release_date(LocalDate.of(2015, 5, 1)).build(),
                Movie.builder().movie_id(6L).name("Avengers: Infinity War").year(2018)
                        .cast("Robert Downey Jr, Chris Evans , Chris HemsWorth").release_date(LocalDate.of(2018, 4, 27)).build(),
                Movie.builder().movie_id(7L).name("Avengers: End Game").year(2019)
                        .cast("Robert Downey Jr, Chris Evans , Chris HemsWorth").release_date(LocalDate.of(2019, 4, 26)).build(),
                Movie.builder().movie_id(8L).name("The Hangover").year(2009)
                        .cast("Bradley Cooper, Ed Helms , Zach Galifianakis").release_date(LocalDate.of(2009, 6, 5)).build(),
                Movie.builder().movie_id(9L).name("The Imitation Game").year(2014)
                        .cast("Benedict Cumberbatch, Keira Knightley").release_date(LocalDate.of(2014, 12, 25)).build(),
                Movie.builder().movie_id(10L).name("The Departed").year(2006)
                        .cast("Leonardo DiCaprio, Matt Damon , Mark Wahlberg").release_date(LocalDate.of(2006, 10, 6)).build()
        );
    }
}
