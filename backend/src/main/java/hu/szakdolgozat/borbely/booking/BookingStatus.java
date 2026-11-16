package hu.szakdolgozat.borbely.booking;

/**
 * A foglalás állapotai: {@code BOOKED} (élő), {@code COMPLETED} (teljesült),
 * {@code CANCELLED} (lemondva), {@code NO_SHOW} (a vendég nem jelent meg).
 */
public enum BookingStatus {
    BOOKED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
