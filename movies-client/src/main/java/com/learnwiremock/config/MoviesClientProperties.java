package com.learnwiremock.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "movies.client")
public record MoviesClientProperties(
        @NotBlank String baseUrl,
        @Positive int connectTimeoutMs,
        @Positive int responseTimeoutSeconds
) {}
