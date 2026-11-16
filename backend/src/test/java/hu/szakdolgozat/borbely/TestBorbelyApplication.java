package hu.szakdolgozat.borbely;

import org.springframework.boot.SpringApplication;

/**
 * Fejlesztői indítás Testcontainers-adatbázissal: a {@code main} metódus futtatásakor
 * a Spring maga indít egy PostgreSQL-konténert, így nem kell hozzá a {@code docker compose}.
 */
public class TestBorbelyApplication {

    public static void main(String[] args) {
        SpringApplication.from(BorbelyApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
