package hu.szakdolgozat.borbely.security;

import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A bejelentkezéshez betölti a felhasználót az e-mail-címe alapján.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email = AppUser.normalizeEmail(username);
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Nincs ilyen felhasználó."));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }
}
