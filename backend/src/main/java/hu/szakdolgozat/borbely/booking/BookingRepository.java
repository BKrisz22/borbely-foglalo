package hu.szakdolgozat.borbely.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * A foglalások adatelérése. Egyelőre a beosztás védelméhez kell: munkaidőt törölni, szűkíteni
 * vagy távollétet felvenni csak akkor lehet, ha nem esik bele foglalás.
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
}
