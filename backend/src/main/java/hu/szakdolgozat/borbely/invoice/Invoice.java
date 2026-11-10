package hu.szakdolgozat.borbely.invoice;

import hu.szakdolgozat.borbely.booking.Booking;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "external_number", unique = true, length = 50)
    private String externalNumber;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "amount_huf", nullable = false)
    private int amountHuf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    protected Invoice() {
    }

    public Invoice(Booking booking) {
        this.booking = booking;
        this.itemName = booking.getServiceOffering().getName();
        this.amountHuf = booking.getPriceHuf();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markIssued(String externalNumber, String pdfUrl) {
        this.status = InvoiceStatus.ISSUED;
        this.externalNumber = externalNumber;
        this.pdfUrl = pdfUrl;
        this.issuedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = InvoiceStatus.FAILED;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getExternalNumber() {
        return externalNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public int getAmountHuf() {
        return amountHuf;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
