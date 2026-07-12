package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Consumer side of Spring Cloud Contract — unlike the hand-written WireMock
 * stubs in {@link MoviesRestClientTest}, the stubs here are generated from the
 * contracts in movies-service and only exist because the producer's build
 * verified them against the real {@code MovieController}.
 * <p>
 * Requires the producer stubs jar in the local repository first:
 * {@code mvn install -pl movies-service} (produces movies-service-*-stubs.jar).
 */
@SpringBootTest
@AutoConfigureStubRunner(
        ids = "com.learnwiremock:movies-service:+:stubs:8091",   // group:artifact:version:classifier:port
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)        // resolve stubs jar from ~/.m2
@TestPropertySource(properties = "movies.client.base-url=http://localhost:8091")
class MoviesRestClientContractIntgTest {

    @Autowired
    private MoviesRestClient moviesRestClient;

    @Test
    void retrieveMovieById_againstProducerVerifiedStub() {
        Movie movie = moviesRestClient.retrieveMovieById(1);

        assertNotNull(movie);
        assertEquals(1L, movie.getMovie_id());
        assertEquals("Batman Begins", movie.getName());
        assertEquals(2005, movie.getYear());
        assertEquals("Christian Bale, Katie Holmes , Liam Neeson", movie.getCast());
    }
}
