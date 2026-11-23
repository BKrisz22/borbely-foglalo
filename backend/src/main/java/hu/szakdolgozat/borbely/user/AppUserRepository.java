package hu.szakdolgozat.borbely.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Aktív vendégek keresése név vagy e-mail-cím részlete alapján, név szerint rendezve.
     * A {@code pattern} már kisbetűs, {@code %…%} alakú minta, a különleges karakterei (\, %, _) escape-elve.
     */
    @Query("""
            select u from AppUser u
            where u.role = hu.szakdolgozat.borbely.user.Role.CUSTOMER and u.enabled = true
              and (lower(u.fullName) like :pattern escape '\\' or u.email like :pattern escape '\\')
            order by u.fullName
            """)
    List<AppUser> searchCustomers(@Param("pattern") String pattern, Pageable pageable);
}
