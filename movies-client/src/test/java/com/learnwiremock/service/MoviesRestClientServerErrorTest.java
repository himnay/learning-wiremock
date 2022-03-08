package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.learnwiremock.exception.MovieErrorResponseException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.GET_ALL_MOVIES_V1;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(WireMockExtension.class)
public class MoviesRestClientServerErrorTest {

    private MoviesRestClient moviesRestClient;
    private WebClient webClient;

    private TcpClient tcpClient = TcpClient
            .create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .doOnConnected(connection -> {
                connection
                        .addHandlerLast(new ReadTimeoutHandler(5))
                        .addHandlerLast(new WriteTimeoutHandler(5));
            });

    @InjectServer
    private WireMockServer wireMockServer;

    @ConfigureWireMock
    private Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    public void init() {
        String baseUrl = String.format("http://localhost:%s/", wireMockServer.port());

        webClient = WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .baseUrl(baseUrl)
                .build();

        moviesRestClient = new MoviesRestClient(webClient);
    }

    @After
    public void cleanup() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("SERVER ERROR 500 - GET all movies")
    void retrieveAllMovies500Test() {
        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(serverError()));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertEquals(exception.getMessage(), "Internal Server Error");
    }

    @Test
    @DisplayName("SERVER ERROR 503 - GET all movies")
    void retrieveAllMovies503Test() {
        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(serviceUnavailable()));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertEquals(exception.getMessage(), "Service Unavailable");
    }

    @Test
    @DisplayName("FAULT ERROR - GET all movies")
    void retrieveAllMoviesFaultErrorTest() {
        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertTrue(exception.getMessage().contains("Connection prematurely closed BEFORE response"));
    }

    @Test
    @DisplayName("FAULT ERROR AFTER RANDOM DATA - GET all movies")
    void retrieveAllMoviesFaultErrorWithRandomDataTest() {
        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertTrue(exception.getMessage().contains("Connection prematurely closed BEFORE response"));
    }

    @Test
    @DisplayName("timeout test")
    public void latencyAndTimeoutTest() {
        var response = aResponse()
                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                .withFixedDelay(10000);

        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(response));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertTrue(exception.getMessage().contains("ReadTimeoutException"));
    }

    @Test
    @DisplayName("timeout test using random delay")
    public void latencyAndTimeoutRandomDelayTest() {
        var response = aResponse()
                .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                .withUniformRandomDelay(6000, 10000);

        stubFor((get(urlPathEqualTo(GET_ALL_MOVIES_V1)))
                .willReturn(response));

        var exception = assertThrows(MovieErrorResponseException.class, () -> moviesRestClient.retrieveAllMovies(), "MovieErrorResponseException is expected");
        assertTrue(exception.getMessage().contains("ReadTimeoutException"));
    }
}
