package hu.szakdolgozat.borbely.booking.dto;

import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.booking.BookingStatus;

import java.time.Instant;

/**
 * Egy foglalás, ahogy a vendég, a borbély és a tulajdonos látja. Az időpontok UTC-ben mennek ki;
 * a felület alakítja budapesti időre.
 */
public record BookingResponse(
        Long id,
        Long barberId,
        String barberName,
        Long customerId,
        String customerName,
        Long serviceId,
        String serviceName,
        Instant startAt,
        Instant endAt,
        int priceHuf,
        BookingStatus status,
        Instant cancelledAt
) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBarber().getId(),
                booking.getBarber().getUser().getFullName(),
                booking.getCustomer().getId(),
                booking.getCustomer().getFullName(),
                booking.getServiceOffering().getId(),
                booking.getServiceOffering().getName(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getPriceHuf(),
                booking.getStatus(),
                booking.getCancelledAt()
        );
    }
}
