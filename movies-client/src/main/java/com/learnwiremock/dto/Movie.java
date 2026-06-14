package com.learnwiremock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Builder (GoF) — Lombok @Builder provides a fluent construction API
 * so callers never need to juggle a 5-argument constructor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    private Long movie_id;
    private String name;
    private Integer year;
    private String cast;
    private LocalDate release_date;
}
