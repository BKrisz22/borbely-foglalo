package hu.szakdolgozat.borbely.common;

/**
 * Ütközés a meglévő adatokkal (például foglalt e-mail-cím vagy szolgáltatásnév).
 * A válasz 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
