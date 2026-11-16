package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.barber.Barber;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A borbély szünete vagy távolléte két időpont között (például ebédszünet vagy szabadság).
 * Ebben az időszakban nem foglalható.
 */
@Entity
@Table(name = "time_off")
public class TimeOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(length = 200)
    private String reason;

    protected TimeOff() {
    }

    public TimeOff(Barber barber, Instant startAt, Instant endAt, String reason) {
        this.barber = barber;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Barber getBarber() {
        return barber;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getReason() {
        return reason;
    }
}
