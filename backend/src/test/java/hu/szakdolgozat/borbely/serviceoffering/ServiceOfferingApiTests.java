package hu.szakdolgozat.borbely.serviceoffering;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ServiceOfferingApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE barber_service, barber, app_user, service_offering RESTART IDENTITY CASCADE");
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hajvágás", "description": "  ", "durationMinutes": 30, "priceHuf": 6000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/services/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.priceHuf").value(6000))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateChangesFields() throws Exception {
        createService("Hajvágás");

        mockMvc.perform(put("/api/services/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hajvágás", "durationMinutes": 45, "priceHuf": 7000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.priceHuf").value(7000));
    }

    @Test
    void deactivatedServiceIsHiddenFromDefaultList() throws Exception {
        createService("Hajvágás");
        createService("Szakálligazítás");

        mockMvc.perform(delete("/api/services/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));

        mockMvc.perform(get("/api/services").param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void invalidRequestReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "durationMinutes": 0, "priceHuf": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.durationMinutes").exists())
                .andExpect(jsonPath("$.errors.priceHuf").exists());
    }

    @Test
    void duplicateNameIgnoringCaseReturns409() throws Exception {
        createService("Hajvágás");

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "hajvágás", "durationMinutes": 30, "priceHuf": 6000}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownServiceReturns404() throws Exception {
        mockMvc.perform(get("/api/services/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private void createService(String name) throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "durationMinutes": 30, "priceHuf": 5000}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }
}
