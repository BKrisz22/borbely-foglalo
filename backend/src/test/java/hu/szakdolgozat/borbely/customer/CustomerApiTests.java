package hu.szakdolgozat.borbely.customer;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A vendégkereső integrációs tesztjei: név és e-mail szerinti keresés, csak vendégek a találatok
 * között, és csak a személyzet használhatja.
 */
@IntegrationTest
class CustomerApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private String vilmaToken;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
        vilmaToken = register("vilma@example.com", "Vendég Vilma");
        register("bela@example.com", "Teszt Béla");
        register("kata_100@example.com", "Kiss Kata");
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "kovacs.peter@example.com", "fullName": "Kovács Péter",
                                 "initialPassword": "Kezdo1234", "serviceIds": []}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void findsCustomersByNameIgnoringCase() throws Exception {
        mockMvc.perform(search("VENDÉG").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Vendég Vilma"))
                .andExpect(jsonPath("$[0].email").value("vilma@example.com"));
    }

    @Test
    void findsCustomersByEmail() throws Exception {
        mockMvc.perform(search("bela@").with(owner()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Teszt Béla"));
    }

    @Test
    void wildcardCharactersAreTakenLiterally() throws Exception {
        // Az aláhúzás a LIKE-ban „bármely karakter” lenne; itt csak a valódi aláhúzásra talál.
        mockMvc.perform(search("a_1").with(owner()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Kiss Kata"));
        // Escape nélkül az „a_e” a vilma@example.com és a bela@example.com „a@e” részére is illeszkedne.
        mockMvc.perform(search("a_e").with(owner()))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(search("%%").with(owner()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void barbersAreNotListedAsCustomers() throws Exception {
        mockMvc.perform(search("kovács").with(owner()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void onlyStaffCanSearchAndTheQueryMustBeLongEnough() throws Exception {
        String barberToken = login("kovacs.peter@example.com", "Kezdo1234");

        mockMvc.perform(search("vil").header("Authorization", "Bearer " + barberToken))
                .andExpect(status().isOk());
        mockMvc.perform(search("vil").header("Authorization", "Bearer " + vilmaToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(search("vil"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(search(" v ").with(owner()))
                .andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder search(String query) {
        return get("/api/customers").param("query", query);
    }

    private static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private String register(String email, String fullName) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "password": "Titkos123"}
                                """.formatted(email, fullName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }
}
