package hu.szakdolgozat.borbely.auth;

import hu.szakdolgozat.borbely.auth.dto.AuthResponse;
import hu.szakdolgozat.borbely.auth.dto.ChangePasswordRequest;
import hu.szakdolgozat.borbely.auth.dto.LoginRequest;
import hu.szakdolgozat.borbely.auth.dto.RegisterRequest;
import hu.szakdolgozat.borbely.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Hitelesítési végpontok ({@code /api/auth}). A regisztráció és a belépés nyilvános,
 * a saját adatok és a jelszócsere bejelentkezést kér.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Vendég regisztrációja; a válasz már tokent is tartalmaz, így nem kell külön belépni (201). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = auth.register(request);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    /** A bejelentkezett felhasználó saját adatai; az azonosító a token {@code sub} claimjéből jön. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(userId(jwt));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(userId(jwt), request);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
