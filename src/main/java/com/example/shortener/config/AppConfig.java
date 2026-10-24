package com.example.shortener.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

    /** Injected wherever "now" is needed, which keeps time deterministic in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** The zone that defines a calendar day for the daily statistics endpoint. */
    @Bean
    public ZoneId statsZone(AppProperties properties) {
        return ZoneId.of(properties.statsZone());
    }
}
