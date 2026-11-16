package hu.szakdolgozat.borbely.security;

import org.springframework.security.core.Authentication;

/**
 * Segédmetódusok a bejelentkezett felhasználó szerepkörének vizsgálatához a vezérlőkben.
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
}
