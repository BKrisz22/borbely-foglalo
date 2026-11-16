package hu.szakdolgozat.borbely.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A távollétek adatelérése.
 */
public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {

    /** A borbély távollétei, amelyek átfedik a [from, to) időszakot, időrendben. */
    @Query("""
            select t from TimeOff t
            where t.barber.id = :barberId
              and t.startAt < :to and t.endAt > :from
            order by t.startAt
            """)
    List<TimeOff> findOverlapping(@Param("barberId") Long barberId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    /** Egy távollét, de csak akkor, ha a megadott borbélyé. */
    Optional<TimeOff> findByIdAndBarberId(Long id, Long barberId);
}
