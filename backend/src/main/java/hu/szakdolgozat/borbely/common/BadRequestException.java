package hu.szakdolgozat.borbely.common;

/**
 * A kérés formailag helyes, de üzleti szabályba ütközik (például hibás a jelenlegi jelszó).
 * A válasz 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
