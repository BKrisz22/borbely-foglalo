package hu.szakdolgozat.borbely.auth.dto;

import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.Role;

/**
 * A bejelentkezett felhasználó adatai. A {@code mustChangePassword} jelzi a frontendnek,
 * hogy a felhasználót a jelszócsere oldalra kell irányítania.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean mustChangePassword
) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isMustChangePassword()
        );
    }
}
