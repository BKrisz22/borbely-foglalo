package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.barber.Barber;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A borbély munkaideje egy adott napon, helyi idő szerint. A kezdés és a befejezés
 * 15 perces rácsra esik, és egy borbély munkaidő-sávjai nem fedhetik egymást;
 * ezt az adatbázis megkötései biztosítják.
 */
@Entity
@Table(name = "availability")
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected Availability() {
    }

    public Availability(Barber barber, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        this.barber = barber;
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void changeTimes(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public Barber getBarber() {
        return barber;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
