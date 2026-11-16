package hu.szakdolgozat.borbely.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * A {@link StrongPassword} szabály ellenőrzése. A 72 bájtos korlát a BCrypt miatt kell: az ennél
 * hosszabb jelszó végét a BCrypt nem dolgozná fel, egy ékezetes betű pedig UTF-8-ban 2 bájt.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 64;
    static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String problem = problem(value);
        if (problem == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problem).addConstraintViolation();
        return false;
    }

    /** A jelszó első hibájának leírása, vagy {@code null}, ha a jelszó megfelel a szabálynak. */
    public static String problem(String value) {
        if (value == null || value.isBlank()) {
            return "A jelszó megadása kötelező.";
        }
        if (value.length() < MIN_LENGTH) {
            return "A jelszó legalább " + MIN_LENGTH + " karakter legyen.";
        }
        if (value.length() > MAX_LENGTH || value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return "A jelszó túl hosszú: legfeljebb " + MAX_LENGTH + " karakter lehet, ékezetes betűkkel kevesebb.";
        }
        boolean hasLetter = value.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = value.codePoints().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return "A jelszóban legyen legalább egy betű és egy számjegy.";
        }
        return null;
    }
}
