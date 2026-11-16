package hu.szakdolgozat.borbely.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** A felhasználók adatelérése. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmail(String email);

    Optional<AppUser> findByEmail(String email);

    boolean existsByRole(Role role);
}
