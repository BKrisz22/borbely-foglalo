package hu.szakdolgozat.borbely.auth.dto;

import hu.szakdolgozat.borbely.common.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Vendég regisztrációja ({@code POST /api/auth/register}). Szerepkört nem lehet megadni:
 * a regisztrált felhasználó mindig vendég lesz.
 */
public record RegisterRequest(
        @NotBlank(message = "Az e-mail-cím megadása kötelező.")
        @Email(message = "Érvénytelen e-mail-cím.")
        @Size(max = 254, message = "Az e-mail-cím legfeljebb 254 karakter lehet.")
        String email,

        @NotBlank(message = "A név megadása kötelező.")
        @Size(max = 100, message = "A név legfeljebb 100 karakter lehet.")
        String fullName,

        @StrongPassword
        String password
) {
}
