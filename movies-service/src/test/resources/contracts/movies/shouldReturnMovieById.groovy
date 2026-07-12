package contracts.movies

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return movie 1 (Batman Begins) for GET /movieservice/v1/movie/{id}"
    request {
        method GET()
        urlPath "/movieservice/v1/movie/1"
    }
    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
                movie_id: 1,
                name: "Batman Begins",
                year: 2005,
                cast: "Christian Bale, Katie Holmes , Liam Neeson",
                release_date: "2005-06-15"
        )
    }
}
