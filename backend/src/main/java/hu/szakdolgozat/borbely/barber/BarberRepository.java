package hu.szakdolgozat.borbely.barber;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberRepository extends JpaRepository<Barber, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "services"})
    List<Barber> findAll();

    @EntityGraph(attributePaths = {"user", "services"})
    List<Barber> findAllByActiveTrue();

    @EntityGraph(attributePaths = {"user", "services"})
    Optional<Barber> findWithDetailsById(Long id);
}
