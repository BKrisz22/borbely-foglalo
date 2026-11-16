package hu.szakdolgozat.borbely.auth;

import hu.szakdolgozat.borbely.auth.dto.AuthResponse;
import hu.szakdolgozat.borbely.auth.dto.ChangePasswordRequest;
import hu.szakdolgozat.borbely.auth.dto.LoginRequest;
import hu.szakdolgozat.borbely.auth.dto.RegisterRequest;
import hu.szakdolgozat.borbely.auth.dto.UserResponse;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.security.IssuedToken;
import hu.szakdolgozat.borbely.security.TokenService;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regisztráció, belépés, a saját adatok lekérdezése és jelszócsere.
 * Belépéskor az e-mail-címet és a jelszót az {@link AuthenticationManager} ellenőrzi; hibás adatoknál
 * {@link BadCredentialsException} keletkezik, amelyből a GlobalExceptionHandler 401-es választ készít.
 */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    /** Új vendégfiókot hoz létre, és rögtön be is lépteti. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = AppUser.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Ezzel az e-mail-címmel már van fiók: " + email);
        }
        AppUser user = users.save(new AppUser(
                email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.CUSTOMER));
        return authResponse(user);
    }

    /** E-mail-cím és jelszó alapján beléptet; hibás adatoknál 401. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AppUser user = users.findByEmail(AppUser.normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Hibás e-mail-cím vagy jelszó."));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(find(userId));
    }

    /**
     * Jelszócsere a jelenlegi jelszó ismeretében. Sikeres csere után megszűnik a kötelező
     * jelszócsere jelzése (például a borbély kezdőjelszava után).
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = find(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("A jelenlegi jelszó hibás.");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BadRequestException("Az új jelszó nem egyezhet meg a jelenlegivel.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
    }

    private AppUser find(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen felhasználó: " + userId));
    }

    private AuthResponse authResponse(AppUser user) {
        IssuedToken token = tokenService.issue(user);
        return new AuthResponse(token.token(), token.expiresAt(), UserResponse.from(user));
    }
}
