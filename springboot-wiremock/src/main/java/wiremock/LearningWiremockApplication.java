package wiremock;

import com.github.tomakehurst.wiremock.core.Options;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@AutoConfigureWireMock()
public class LearningWiremockApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningWiremockApplication.class, args);
    }

    @Bean
    public Options wireMockOptions() {
        return WireMockSpring
                .options()
                .port(8081);
    }

}
