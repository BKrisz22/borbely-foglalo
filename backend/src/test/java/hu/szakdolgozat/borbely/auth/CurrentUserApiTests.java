package hu.szakdolgozat.borbely.auth;

import com.jayway.jsonpath.JsonPath;
import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A saját adatok ({@code /api/auth/me}) és a belépési válasz tartalmazza a borbélyprofil azonosítóját,
 * hogy a borbély felülete tudja, melyik beosztást kezelje; vendégnél ez null.
 */
@IntegrationTest
class CurrentUserApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void aBarberSeesTheirBarberIdAtLoginAndInMe() throws Exception {
        createBarber("kovacs.peter@example.com", "Kovács Péter");
        createBarber("szabo.anna@example.com", "Szabó Anna");

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "szabo.anna@example.com", "password": "Kezdo1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.barberId").value(2))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BARBER"))
                .andExpect(jsonPath("$.barberId").value(2));
    }

    @Test
    void aCustomerHasNoBarberId() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vilma@example.com", "fullName": "Vendég Vilma", "password": "Titkos123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.barberId").doesNotExist());
    }

    private void createBarber(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": []}
                                """.formatted(email, fullName)))
                .andExpect(status().isCreated());
    }
}
