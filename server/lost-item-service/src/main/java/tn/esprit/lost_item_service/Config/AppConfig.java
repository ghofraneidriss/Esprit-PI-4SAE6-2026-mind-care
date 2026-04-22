package tn.esprit.lost_item_service.Config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    /**
     * Applies a 30-second read timeout to the RestClient used by Spring AI.
     * Without this the Groq LLM call can hang indefinitely.
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);  // 10s connect
            factory.setReadTimeout(30_000);     // 30s read
            builder.requestFactory(factory);
        };
    }
}
