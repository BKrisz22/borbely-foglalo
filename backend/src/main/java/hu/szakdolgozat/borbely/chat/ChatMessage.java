package hu.szakdolgozat.borbely.chat;

import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/** Egy foglaláshoz tartozó csevegőüzenet a vendég és a borbély között. */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected ChatMessage() {
    }

    public ChatMessage(Booking booking, AppUser sender, String body) {
        this.booking = booking;
        this.sender = sender;
        this.body = body.trim();
    }

    @PrePersist
    void onCreate() {
        sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public AppUser getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
