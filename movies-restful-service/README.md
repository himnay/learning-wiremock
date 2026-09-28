# <span style="color:hsl(249,80%,58%)">Movies RestFul WebService</span>

## <span style="color:hsl(27,80%,58%)">How to Run the app?</span>

- Two builds of the same service (Spring Boot 2.1, in-memory H2): `movies-restful-service-java8.jar` needs
  Java 8 or higher, `movies-restful-service-beyond-java8.jar` Java 11 or higher. `docker compose up -d` in
  the repo root runs the latter on Java 27.

```
java -jar movies-restful-service-beyond-java8.jar
```

## <span style="color:hsl(164,80%,58%)">Swagger Link</span>

The below link will launch the swagger of the movies-restful-web-service.

http://localhost:8081/movieservice/swagger-ui.html#/
