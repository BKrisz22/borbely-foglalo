package hu.szakdolgozat.borbely.barber.dto;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingResponse;
import hu.szakdolgozat.borbely.user.AppUser;

import java.util.Comparator;
import java.util.List;

/** A borbély adatai az API válaszaiban; a szolgáltatásai név szerint rendezve. */
public record BarberResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String bio,
        boolean active,
        int minLeadMinutes,
        List<ServiceOfferingResponse> services
) implements BarberView {

    public static BarberResponse from(Barber barber) {
        AppUser user = barber.getUser();
        List<ServiceOfferingResponse> services = barber.getServices().stream()
                .sorted(Comparator.comparing(ServiceOffering::getName))
                .map(ServiceOfferingResponse::from)
                .toList();
        return new BarberResponse(
                barber.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                barber.getBio(),
                barber.isActive(),
                barber.getMinLeadMinutes(),
                services
        );
    }
}
