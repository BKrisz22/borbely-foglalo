package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A nyilvános borbélylista integrációs tesztjei: bejelentkezés nélkül nem látszik az e-mail-cím,
 * a kikapcsolt borbély és a kikapcsolt szolgáltatás; a tulajdonos mindent lát.
 */
@IntegrationTest
class BarberPublicApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE barber_service, barber, app_user, service_offering RESTART IDENTITY CASCADE");
    }

    @Test
    void publicListHidesEmailAndInactiveBarbers() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[]");
        createBarber("szabo.anna@example.com", "Szabó Anna", "[]");
        mockMvc.perform(delete("/api/barbers/2").with(owner())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/barbers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Kovács Péter"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void publicViewShowsOnlyActiveServices() throws Exception {
        createService("Hajvágás");
        createService("Szakálligazítás");
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[1, 2]");
        mockMvc.perform(delete("/api/services/2").with(owner())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/barbers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(1)))
                .andExpect(jsonPath("$.services[0].name").value("Hajvágás"));
    }

    @Test
    void includeInactiveIsOnlyForTheOwner() throws Exception {
        mockMvc.perform(get("/api/barbers").param("includeInactive", "true"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/barbers").param("includeInactive", "true").with(owner()))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveBarberIsHiddenFromThePublicButNotFromTheOwner() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter", "[]");
        mockMvc.perform(delete("/api/barbers/1").with(owner())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/barbers/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/barbers/1").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kovacs.peter@example.com"));
    }

    private static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private void createService(String name) throws Exception {
        mockMvc.perform(post("/api/services")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "durationMinutes": 30, "priceHuf": 5000}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }

    private void createBarber(String email, String fullName, String serviceIds) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": %s}
                                """.formatted(email, fullName, serviceIds)))
                .andExpect(status().isCreated());
    }
}
