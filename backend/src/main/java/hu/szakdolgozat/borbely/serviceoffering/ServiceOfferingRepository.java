package hu.szakdolgozat.borbely.serviceoffering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * A szolgáltatások adatelérése. A név egyediségét kis- és nagybetűtől
 * függetlenül ellenőrzi.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findAllByOrderByNameAsc();

    List<ServiceOffering> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
