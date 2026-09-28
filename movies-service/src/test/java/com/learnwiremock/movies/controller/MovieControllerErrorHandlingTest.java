package com.learnwiremock.movies.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Client mistakes must come back as 4xx in the usual error body, not as the catch-all 500 that
 * {@code GlobalExceptionHandler} reserves for real server failures.
 */
@SpringBootTest
class MovieControllerErrorHandlingTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mockMvc.perform(post("/movieservice/v1/movie").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/movieservice/v1/movie"));
    }

    @Test
    void aNonNumericIdIsABadRequest() throws Exception {
        mockMvc.perform(get("/movieservice/v1/movie/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void aMissingQueryParameterIsABadRequest() throws Exception {
        mockMvc.perform(get("/movieservice/v1/movieYear"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/movieservice/v1/allMovies"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void anUnknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/movieservice/v1/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aMovieWithoutANameStillListsTheMissingField() throws Exception {
        mockMvc.perform(post("/movieservice/v1/movie").contentType(MediaType.APPLICATION_JSON).content("{\"year\":2021}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please pass all the input fields : [name]"));
    }
}
