package com.shop.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The same engines as production (managed PostgreSQL + Kafka), started by Testcontainers.
 * This replaces the old HSQLDB setup: we test against what we run.
 *
 * <p>Usage: {@code @Import(ProductionLikeContainers.class)} on a {@code @SpringBootTest},
 * or import only {@link Postgres} / {@link Kafka} (nested configurations are picked up automatically). Spring Boot wires the connection
 * details automatically via {@link ServiceConnection}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ProductionLikeContainers {

    /** Keep these aligned with the managed services' versions in production. */
    public static final String POSTGRES_IMAGE = "postgres:17-alpine";
    public static final String KAFKA_IMAGE = "apache/kafka:3.9.1";

    @TestConfiguration(proxyBeanMethods = false)
    public static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Kafka {
        @Bean
        @ServiceConnection
        KafkaContainer kafka() {
            return new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        }
    }
}
