package hu.szakdolgozat.borbely.serviceoffering.dto;

import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;

public record ServiceOfferingResponse(
        Long id,
        String name,
        String description,
        int durationMinutes,
        int priceHuf,
        boolean active
) {

    public static ServiceOfferingResponse from(ServiceOffering service) {
        return new ServiceOfferingResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPriceHuf(),
                service.isActive()
        );
    }
}
