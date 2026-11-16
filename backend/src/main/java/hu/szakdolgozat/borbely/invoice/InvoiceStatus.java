package hu.szakdolgozat.borbely.invoice;

/**
 * A számla állapotai: {@code PENDING} (kiállításra vár), {@code ISSUED} (kiállítva),
 * {@code FAILED} (a kiállítás sikertelen).
 */
public enum InvoiceStatus {
    PENDING,
    ISSUED,
    FAILED
}
