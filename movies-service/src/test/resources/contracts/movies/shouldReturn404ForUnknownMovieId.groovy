package contracts.movies

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return 404 with an error body for GET /movieservice/v1/movie/{id} when the id is unknown"
    request {
        method GET()
        urlPath "/movieservice/v1/movie/100"
    }
    response {
        status NOT_FOUND()
        headers {
            contentType applicationJson()
        }
        body(
                timestamp: "2026-01-01T00:00:00Z",
                status: 404,
                error: "Not Found",
                message: "No Movie Available with the given Id - 100",
                path: "/movieservice/v1/movie/100"
        )
        bodyMatchers {
            // timestamp varies per request — only assert it is present on the producer side
            jsonPath('$.timestamp', byRegex(nonEmpty()))
        }
    }
}
