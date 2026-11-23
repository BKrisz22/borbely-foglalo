package hu.szakdolgozat.borbely.barber;

import com.jayway.jsonpath.JsonPath;
import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A borbély saját profiljának tesztjei: a borbély a sajátját látja teljes adatokkal és módosíthatja,
 * a másikét nem, a tulajdonos bárkiét; a vendég egyiket sem.
 */
@IntegrationTest
class BarberProfileApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private String barberToken;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
        createBarber("kovacs.peter@example.com", "Kovács Péter");
        createBarber("szabo.anna@example.com", "Szabó Anna");
        barberToken = token(post("/api/auth/login"), """
                {"email": "kovacs.peter@example.com", "password": "Kezdo1234"}
                """);
    }

    @Test
    void aBarberUpdatesTheirOwnProfile() throws Exception {
        mockMvc.perform(profile(1, "  Klasszikus vágások.  ", 30).header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Klasszikus vágások."))
                .andExpect(jsonPath("$.minLeadMinutes").value(30))
                .andExpect(jsonPath("$.fullName").value("Kovács Péter"));
    }

    @Test
    void aBarberSeesTheirOwnFullDetailsButOnlyThePublicDataOfOthers() throws Exception {
        mockMvc.perform(get("/api/barbers/{id}", 1).header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kovacs.peter@example.com"))
                .andExpect(jsonPath("$.minLeadMinutes").value(60));

        mockMvc.perform(get("/api/barbers/{id}", 2).header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Szabó Anna"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.minLeadMinutes").doesNotExist());

        mockMvc.perform(get("/api/barbers/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLeadMinutes").doesNotExist());
    }

    @Test
    void aBarberCannotUpdateAnotherBarbersProfile() throws Exception {
        mockMvc.perform(profile(2, "Nem az enyém.", 30).header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void theOwnerCanUpdateAnyProfileButACustomerCannot() throws Exception {
        mockMvc.perform(profile(2, null, 120).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLeadMinutes").value(120));

        String customerToken = token(post("/api/auth/register"), """
                {"email": "vilma@example.com", "fullName": "Vendég Vilma", "password": "Titkos123"}
                """);
        mockMvc.perform(profile(1, null, 30).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidLeadTimeIsRejected() throws Exception {
        mockMvc.perform(profile(1, null, 2000).header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder profile(long barberId, String bio, int minLeadMinutes) {
        String bioJson = bio == null ? "null" : "\"" + bio + "\"";
        return put("/api/barbers/{id}/profile", barberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bio": %s, "minLeadMinutes": %d}
                        """.formatted(bioJson, minLeadMinutes));
    }

    private static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private void createBarber(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": []}
                                """.formatted(email, fullName)))
                .andExpect(status().is2xxSuccessful());
    }

    /** Belépés vagy regisztráció, és a kapott token. */
    private String token(MockHttpServletRequestBuilder request, String json) throws Exception {
        String body = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }
}
