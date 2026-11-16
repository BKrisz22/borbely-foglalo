package hu.szakdolgozat.borbely.auth.dto;

import hu.szakdolgozat.borbely.common.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Jelszócsere a jelenlegi jelszó ismeretében ({@code PUT /api/auth/password}).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "A jelenlegi jelszó megadása kötelező.")
        String currentPassword,

        @StrongPassword
        String newPassword
) {
}
