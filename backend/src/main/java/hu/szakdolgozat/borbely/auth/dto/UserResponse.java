package hu.szakdolgozat.borbely.auth.dto;

import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.Role;

/**
 * A bejelentkezett felhasználó adatai. A {@code mustChangePassword} jelzi a frontendnek,
 * hogy a felhasználót a jelszócsere oldalra kell irányítania. A {@code barberId} a felhasználó
 * borbélyprofiljának azonosítója (a borbély ezzel éri el a saját beosztását); vendégnél null.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean mustChangePassword,
        Long barberId
) {

    public static UserResponse from(AppUser user, Long barberId) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isMustChangePassword(),
                barberId
        );
    }
}
