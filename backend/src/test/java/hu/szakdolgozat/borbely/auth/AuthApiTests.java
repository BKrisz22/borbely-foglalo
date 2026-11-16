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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A regisztráció, a belépés, a jelszócsere és a jogosultsági szabályok integrációs tesztjei,
 * valódi tokenekkel és valódi PostgreSQL-adatbázissal. Minden teszt üres adatbázisból indul.
 */
@IntegrationTest
class AuthApiTests {

    private static final String PASSWORD = "Titkos123";
    private static final String AUTHORIZATION = "Authorization";
    private static final String SERVICE_JSON = """
            {"name": "Hajvágás", "durationMinutes": 30, "priceHuf": 5000}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE barber_service, barber, app_user, service_offering RESTART IDENTITY CASCADE");
    }

    @Test
    void registerReturns201WithTokenAndCustomerRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Vendeg.Vilma@Example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/auth/me"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.user.email").value("vendeg.vilma@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(false));
    }

    @Test
    void registerWithTakenEmailIgnoringCaseReturns409() throws Exception {
        register("vendeg@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("VENDEG@example.com", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithWeakPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("vendeg@example.com", "csakbetuk")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password")
                        .value("A jelszóban legyen legalább egy betű és egy számjegy."));
    }

    @Test
    void loginThenMeReturnsOwnData() throws Exception {
        register("vendeg@example.com", PASSWORD);
        String token = login("vendeg@example.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("vendeg@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameResponse() throws Exception {
        register("vendeg@example.com", PASSWORD);

        String wrongPassword = mockMvc.perform(loginRequest("vendeg@example.com", "Rossz12345"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String unknownEmail = mockMvc.perform(loginRequest("nincs@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        register("vendeg@example.com", PASSWORD);
        jdbc.update("UPDATE app_user SET enabled = false WHERE email = ?", "vendeg@example.com");

        mockMvc.perform(loginRequest("vendeg@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Hibás e-mail-cím vagy jelszó."));
    }

    @Test
    void ownerEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(SERVICE_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Bejelentkezés szükséges"));
    }

    @Test
    void ownerEndpointWithCustomerTokenReturns403() throws Exception {
        String token = register("vendeg@example.com", PASSWORD);

        mockMvc.perform(post("/api/services")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SERVICE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Hozzáférés megtagadva"));
    }

    @Test
    void publicListsWorkWithoutToken() throws Exception {
        mockMvc.perform(get("/api/services")).andExpect(status().isOk());
        mockMvc.perform(get("/api/barbers")).andExpect(status().isOk());
    }

    @Test
    void tokenWithForeignSignatureIsRejected() throws Exception {
        String[] first = register("elso@example.com", PASSWORD).split("\\.");
        String[] second = register("masodik@example.com", PASSWORD).split("\\.");
        // Az első felhasználó adatai a második token aláírásával: az aláírás nem illik a tartalomhoz.
        String forged = first[0] + "." + first[1] + "." + second[2];

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordChecksCurrentPasswordAndReplacesIt() throws Exception {
        String token = register("vendeg@example.com", PASSWORD);

        mockMvc.perform(changePassword(token, "Rossz12345", "UjJelszo456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A jelenlegi jelszó hibás."));

        mockMvc.perform(changePassword(token, PASSWORD, "UjJelszo456"))
                .andExpect(status().isNoContent());

        mockMvc.perform(loginRequest("vendeg@example.com", PASSWORD)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginRequest("vendeg@example.com", "UjJelszo456")).andExpect(status().isOk());
    }

    @Test
    void barberLogsInWithInitialPasswordAndMustChangeIt() throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "borbely@example.com", "fullName": "Borbély Béla", "initialPassword": "Kezdo1234"}
                                """))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(loginRequest("borbely@example.com", "Kezdo1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("BARBER"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(changePassword(token, "Kezdo1234", "Sajat5678"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    /** Regisztrál, és visszaadja a kapott tokent. */
    private String register(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    /** Belép, és visszaadja a kapott tokent. */
    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private static MockHttpServletRequestBuilder loginRequest(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private static MockHttpServletRequestBuilder changePassword(String token, String current, String next) {
        return put("/api/auth/password")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(current, next));
    }

    private static String registerJson(String email, String password) {
        return """
                {"email": "%s", "fullName": "Teszt Vendég", "password": "%s"}
                """.formatted(email, password);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
