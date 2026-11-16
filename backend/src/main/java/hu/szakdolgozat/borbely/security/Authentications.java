package hu.szakdolgozat.borbely.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Segédmetódusok a bejelentkezett felhasználó szerepkörének és azonosítójának lekérdezéséhez.
 */
public final class Authentications {

    private Authentications() {
    }

    /** Igaz, ha a kérés tulajdonosként ({@code ROLE_OWNER}) hitelesített; névtelen kérésnél hamis. */
    public static boolean isOwner(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));
    }

    /**
     * A bejelentkezett felhasználó azonosítója a token {@code sub} mezőjéből. Névtelen kérésnél,
     * vagy ha a {@code sub} nem szám, null.
     */
    public static Long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                return Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
