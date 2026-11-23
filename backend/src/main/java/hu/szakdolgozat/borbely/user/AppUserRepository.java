package hu.szakdolgozat.borbely.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

/** A felhasználók adatelérése. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmail(String email);

    Optional<AppUser> findByEmail(String email);

    boolean existsByRole(Role role);

    /**
     * A felhasználó betöltése sorzárral ({@code SELECT … FOR UPDATE}). A foglalás ezzel zárolja
     * a vendéget, így ugyanannak a vendégnek két egyidejű foglalása egymás után fut, és a második
     * már látja az elsőt. A zár a tranzakció végéig tart.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppUser> findWithLockById(Long id);
}
