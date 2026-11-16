package hu.szakdolgozat.borbely.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A munkaidő-sávok adatelérése.
 */
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    /** A borbély munkaidő-sávjai két nap között (mindkettőt beleértve), időrendben. */
    List<Availability> findByBarberIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
            Long barberId, LocalDate from, LocalDate to);

    /** Egy sáv, de csak akkor, ha a megadott borbélyé. */
    Optional<Availability> findByIdAndBarberId(Long id, Long barberId);
}
