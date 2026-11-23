package hu.szakdolgozat.borbely.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A foglalások adatelérése. A listázó lekérdezések a borbélyt, a vendéget és a szolgáltatást
 * egy lekérdezésben töltik be ({@code @EntityGraph}), így a válasz összeállítása nem okoz N+1 lekérdezést.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** A borbély nem lemondott foglalásai, amelyek átfedik a [from, to) időszakot, időrendben. */
    @Query("""
            select b from Booking b
            where b.barber.id = :barberId
              and b.status <> hu.szakdolgozat.borbely.booking.BookingStatus.CANCELLED
              and b.startAt < :to and b.endAt > :from
            order by b.startAt
            """)
    List<Booking> findActiveOverlapping(@Param("barberId") Long barberId,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);

    /** Van-e a vendégnek bármelyik borbélynál nem lemondott foglalása, amely átfedi a [from, to) időszakot. */
    @Query("""
            select case when count(b) > 0 then true else false end from Booking b
            where b.customer.id = :customerId
              and b.status <> hu.szakdolgozat.borbely.booking.BookingStatus.CANCELLED
              and b.startAt < :to and b.endAt > :from
            """)
    boolean existsActiveForCustomerOverlapping(@Param("customerId") Long customerId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to);

    /** A vendég foglalásai (minden állapotban), amelyek átfedik a [from, to) időszakot. */
    @EntityGraph(attributePaths = {"barber", "barber.user", "customer", "serviceOffering"})
    @Query("""
            select b from Booking b
            where b.customer.id = :customerId and b.startAt < :to and b.endAt > :from
            order by b.startAt
            """)
    List<Booking> findForCustomer(@Param("customerId") Long customerId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    /** A borbély foglalásai (minden állapotban), amelyek átfedik a [from, to) időszakot. */
    @EntityGraph(attributePaths = {"barber", "barber.user", "customer", "serviceOffering"})
    @Query("""
            select b from Booking b
            where b.barber.id = :barberId and b.startAt < :to and b.endAt > :from
            order by b.startAt
            """)
    List<Booking> findForBarber(@Param("barberId") Long barberId,
                                @Param("from") Instant from,
                                @Param("to") Instant to);

    /** Az összes foglalás (minden állapotban), amely átfedi a [from, to) időszakot; a tulajdonosnak. */
    @EntityGraph(attributePaths = {"barber", "barber.user", "customer", "serviceOffering"})
    @Query("""
            select b from Booking b
            where b.startAt < :to and b.endAt > :from
            order by b.startAt
            """)
    List<Booking> findAllBetween(@Param("from") Instant from, @Param("to") Instant to);

    @EntityGraph(attributePaths = {"barber", "barber.user", "customer", "serviceOffering"})
    Optional<Booking> findWithDetailsById(Long id);
}
