package org.alex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Application entry point.
 *
 * @EnableCaching is required for @Cacheable (used on
 * CountryDataService.getBorders()) to actually take effect — without it,
 * the annotation is silently ignored and every call re-fetches and
 * re-builds the Borders graph from scratch.
 */
@SpringBootApplication
@EnableCaching
public class CountryRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CountryRoutingApplication.class, args);
    }
}