package hu.szakdolgozat.borbely.barber.dto;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingResponse;

import java.util.Comparator;
import java.util.List;

/**
 * A borbély nyilvános adatai, bejelentkezés nélkül is láthatók: e-mail-cím és belső beállítások
 * nélkül, csak az aktív szolgáltatásokkal, név szerint rendezve.
 */
public record BarberPublicResponse(
        Long id,
        String fullName,
        String bio,
        List<ServiceOfferingResponse> services
) implements BarberView {

    public static BarberPublicResponse from(Barber barber) {
        List<ServiceOfferingResponse> services = barber.getServices().stream()
                .filter(ServiceOffering::isActive)
                .sorted(Comparator.comparing(ServiceOffering::getName))
                .map(ServiceOfferingResponse::from)
                .toList();
        return new BarberPublicResponse(barber.getId(), barber.getUser().getFullName(), barber.getBio(), services);
    }
}
