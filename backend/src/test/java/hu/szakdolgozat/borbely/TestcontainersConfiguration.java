package hu.szakdolgozat.borbely;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A tesztekhez használt PostgreSQL-konténer. A {@code @ServiceConnection} miatt a Spring
 * automatikusan ehhez csatlakozik; az image verziója megegyezik a {@code docker-compose.yml}-ben használttal.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }
}
