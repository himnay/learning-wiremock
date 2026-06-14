package com.learnwiremock.service;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.learnwiremock.exception.MovieErrorResponseException;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.GET_ALL_MOVIES_V1;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates WireMock fault simulation, connection timeouts, and
 * response timeouts using the modern Reactor Netty {@link HttpClient} API
 * (replaces the deprecated {@code TcpClient} approach).
 */
class MoviesRestClientServerErrorTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
                    .templatingEnabled(true)
                    .globalTemplating(true))
            .build();

    private MoviesRestClient moviesRestClient;

    @BeforeEach
    void setup() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5));

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(wireMock.baseUrl())
                .build();

        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    @DisplayName("500 Internal Server Error — GET all movies")
    void retrieveAllMovies500Test() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(serverError()));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies());
        assertEquals("Internal Server Error", ex.getMessage());
    }

    @Test
    @DisplayName("503 Service Unavailable — GET all movies")
    void retrieveAllMovies503Test() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(serviceUnavailable()));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies());
        assertEquals("Service Unavailable", ex.getMessage());
    }

    @Test
    @DisplayName("Fault: EMPTY_RESPONSE — connection closed before response is sent")
    void retrieveAllMoviesFaultEmptyResponseTest() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        var ex = assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies());
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("Fault: RANDOM_DATA_THEN_CLOSE — garbled bytes then connection close")
    void retrieveAllMoviesFaultRandomDataTest() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies());
    }

    @Test
    @DisplayName("Timeout: fixed 10-second delay exceeds 5-second responseTimeout")
    void latencyAndTimeoutFixedDelayTest() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withFixedDelay(10_000)));  // 10 s > 5 s client timeout

        assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies(),
                "Timeout should trigger MovieErrorResponseException");
    }

    @Test
    @DisplayName("Timeout: random delay 6–10 seconds exceeds 5-second responseTimeout")
    void latencyAndTimeoutRandomDelayTest() {
        wireMock.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withUniformRandomDelay(6_000, 10_000)));

        assertThrows(MovieErrorResponseException.class,
                () -> moviesRestClient.retrieveAllMovies(),
                "Timeout should trigger MovieErrorResponseException");
    }
}
