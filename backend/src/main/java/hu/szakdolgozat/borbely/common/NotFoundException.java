package hu.szakdolgozat.borbely.common;

/** A keresett erőforrás nem létezik. A válasz 404 Not Found. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
