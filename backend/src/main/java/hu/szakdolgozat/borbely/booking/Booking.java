package hu.szakdolgozat.borbely.booking;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.user.AppUser;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_offering_id", nullable = false)
    private ServiceOffering serviceOffering;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "price_huf", nullable = false)
    private int priceHuf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    protected Booking() {
    }

    public Booking(Barber barber, AppUser customer, ServiceOffering serviceOffering, Instant startAt) {
        this.barber = barber;
        this.customer = customer;
        this.serviceOffering = serviceOffering;
        this.startAt = startAt;
        this.endAt = startAt.plus(Duration.ofMinutes(serviceOffering.getDurationMinutes()));
        this.priceHuf = serviceOffering.getPriceHuf();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void cancel() {
        requireBooked();
        status = BookingStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public void complete() {
        requireBooked();
        status = BookingStatus.COMPLETED;
    }

    public void markNoShow() {
        requireBooked();
        status = BookingStatus.NO_SHOW;
    }

    public void markReminderSent() {
        reminderSentAt = Instant.now();
    }

    private void requireBooked() {
        if (status != BookingStatus.BOOKED) {
            throw new IllegalStateException("A foglalás már le van zárva: " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Barber getBarber() {
        return barber;
    }

    public AppUser getCustomer() {
        return customer;
    }

    public ServiceOffering getServiceOffering() {
        return serviceOffering;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public int getPriceHuf() {
        return priceHuf;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }
}
