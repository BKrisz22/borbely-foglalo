package hu.szakdolgozat.borbely.demo;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A bemutató adatok tesztje. A {@code demo} profil miatt ez a teszt külön Spring-környezetben,
 * saját, üres adatbázissal indul, így az induláskor lefutó betöltés eredménye közvetlenül ellenőrizhető.
 */
@IntegrationTest
@ActiveProfiles("demo")
class DemoDataInitializerTests {

    @Autowired
    DemoDataInitializer initializer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mockMvc;

    @Test
    void demoDataIsLoadedOnceAndIsUsable() throws Exception {
        assertThat(count("service_offering")).isEqualTo(4);
        assertThat(count("barber")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE role = 'CUSTOMER'", Long.class))
                .isEqualTo(3);
        assertThat(count("availability")).isPositive();
        assertThat(count("booking")).isEqualTo(5);

        // Második futás: nem tölt be semmit újra.
        initializer.run(null);
        assertThat(count("service_offering")).isEqualTo(4);
        assertThat(count("booking")).isEqualTo(5);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vilma@example.com", "password": "%s"}
                                """.formatted(DemoDataInitializer.PASSWORD)))
                .andExpect(status().isOk());

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Budapest"));
        mockMvc.perform(get("/api/barbers/1/available-days")
                        .param("serviceId", "1")
                        .param("from", today.toString())
                        .param("to", today.plusDays(14).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
