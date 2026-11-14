package com.keroles.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Kafka learning module.
 * Each roadmap week adds its own package, e.g. com.keroles.kafka.week02.
 * See TODO.md for the roadmap.
 */
@SpringBootApplication
public class KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
    }
}
