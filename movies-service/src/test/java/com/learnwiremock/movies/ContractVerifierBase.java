package com.learnwiremock.movies;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for the JUnit 5 tests that spring-cloud-contract-maven-plugin
 * generates from the Groovy contracts in {@code src/test/resources/contracts}.
 * <p>
 * Boots the full application context (real {@code MovieController},
 * {@code MovieService}, seeded in-memory {@code MovieRepository} and the
 * {@code GlobalExceptionHandler}) and points RestAssured's MockMvc module at
 * it — so the generated tests verify the contracts against the REAL
 * controller behaviour, not hand-written expectations.
 */
@SpringBootTest
public abstract class ContractVerifierBase {

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
