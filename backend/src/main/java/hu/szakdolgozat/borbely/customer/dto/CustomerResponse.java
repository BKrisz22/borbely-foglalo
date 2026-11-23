package hu.szakdolgozat.borbely.customer.dto;

import hu.szakdolgozat.borbely.user.AppUser;

/**
 * Egy vendég a vendégkereső találatai között: ennyi kell ahhoz, hogy a személyzet kiválassza,
 * kinek a nevére foglal.
 */
public record CustomerResponse(
        Long id,
        String fullName,
        String email
) {

    public static CustomerResponse from(AppUser user) {
        return new CustomerResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
