package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A borbély-végpontok integrációs tesztjei valódi PostgreSQL-adatbázissal.
 * Minden teszt üres adatbázisból indul.
 * A kérések tulajdonosi ({@code OWNER}) jogosultsággal futnak.
 */
@IntegrationTest
@WithMockUser(roles = "OWNER")
class BarberApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE barber_service, barber, app_user, service_offering RESTART IDENTITY CASCADE");
    }

    @Test
    void createReturns201WithServicesAndLowercaseEmail() throws Exception {
        createService("Hajvágás");
        createService("Szakálligazítás");

        mockMvc.perform(post("/api/barbers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "Kovacs.Peter@Example.com", "fullName": "Kovács Péter", "initialPassword": "Kezdo1234", "serviceIds": [1, 2]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/barbers/1"))
                .andExpect(jsonPath("$.email").value("kovacs.peter@example.com"))
                .andExpect(jsonPath("$.minLeadMinutes").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.services", hasSize(2)));
    }

    @Test
    void listFiltersByServiceAndSortsInHungarianOrder() throws Exception {
        createService("Hajvágás");
        createService("Szakálligazítás");
        createBarber("balogh.bela@example.com", "Balogh Béla", "[1, 2]");
        createBarber("abraham.adam@example.com", "Ábrahám Ádám", "[1]");
        createBarber("cser.csaba@example.com", "Cser Csaba", "[2]");

        mockMvc.perform(get("/api/barbers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[2].id").value(3));

        mockMvc.perform(get("/api/barbers").param("serviceId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void updateChangesFields() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[]");

        mockMvc.perform(put("/api/barbers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Kovács Péter", "bio": "Klasszikus vágások", "minLeadMinutes": 120}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLeadMinutes").value(120))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void replaceServicesSwapsTheWholeSet() throws Exception {
        createService("Hajvágás");
        createService("Szakálligazítás");
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[1]");

        mockMvc.perform(put("/api/barbers/1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceIds": [2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(1)))
                .andExpect(jsonPath("$.services[0].id").value(2));
    }

    @Test
    void replaceServicesWithUnknownIdReturns404AndChangesNothing() throws Exception {
        createService("Hajvágás");
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[1]");

        mockMvc.perform(put("/api/barbers/1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceIds": [1, 999]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("999")));

        mockMvc.perform(get("/api/barbers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(1)));
    }

    @Test
    void duplicateEmailIgnoringCaseReturns409() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[]");

        mockMvc.perform(post("/api/barbers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "KOVACS.PETER@example.com", "fullName": "Másik Péter", "initialPassword": "Kezdo1234"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivatedBarberIsHiddenFromDefaultList() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[]");
        createBarber("szabo.anna@example.com", "Szabó Anna", "[]");

        mockMvc.perform(delete("/api/barbers/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/barbers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));

        mockMvc.perform(get("/api/barbers").param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void invalidRequestReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nem-email", "fullName": "", "minLeadMinutes": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.initialPassword").exists())
                .andExpect(jsonPath("$.errors.minLeadMinutes").exists());
    }

    @Test
    void unknownBarberReturns404() throws Exception {
        mockMvc.perform(get("/api/barbers/999"))
                .andExpect(status().isNotFound());
    }

    private void createService(String name) throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "durationMinutes": 30, "priceHuf": 5000}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }

    private void createBarber(String email, String fullName, String serviceIds) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": %s}
                                """.formatted(email, fullName, serviceIds)))
                .andExpect(status().isCreated());
    }
}
