package hu.szakdolgozat.borbely.security;

import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Az induláskori tulajdonosi fiók létrehozásának egységtesztjei, adatbázis nélkül (Mockito).
 */
class OwnerAccountInitializerTest {

    private final AppUserRepository users = mock(AppUserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void createsOwnerWhenNoneExists() {
        when(users.existsByRole(Role.OWNER)).thenReturn(false);
        when(users.existsByEmail("tulaj@example.com")).thenReturn(false);

        initializer("Tulaj@Example.com", "Tulaj1234").run(new DefaultApplicationArguments());

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("tulaj@example.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(encoder.matches("Tulaj1234", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void doesNothingWhenOwnerAlreadyExists() {
        when(users.existsByRole(Role.OWNER)).thenReturn(true);

        initializer("tulaj@example.com", "Tulaj1234").run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }

    @Test
    void onlyWarnsWhenNotConfigured() {
        when(users.existsByRole(Role.OWNER)).thenReturn(false);

        initializer("", "").run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }

    @Test
    void rejectsWeakPassword() {
        when(users.existsByRole(Role.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> initializer("tulaj@example.com", "change-me").run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OWNER_PASSWORD");
        verify(users, never()).save(any());
    }

    @Test
    void rejectsEmailOfAnExistingNonOwnerAccount() {
        when(users.existsByRole(Role.OWNER)).thenReturn(false);
        when(users.existsByEmail("vendeg@example.com")).thenReturn(true);

        assertThatThrownBy(() -> initializer("vendeg@example.com", "Tulaj1234").run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OWNER_EMAIL");
        verify(users, never()).save(any());
    }

    private OwnerAccountInitializer initializer(String email, String password) {
        return new OwnerAccountInitializer(new OwnerProperties(email, password), users, encoder);
    }
}
