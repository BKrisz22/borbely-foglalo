package hu.szakdolgozat.borbely.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Jelszószabály: 8–64 karakter, legalább egy betűvel és egy számjeggyel, UTF-8-ban legfeljebb 72 bájt.
 * Az ellenőrzést a {@link StrongPasswordValidator} végzi, mindig a konkrét hibát megnevezve.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface StrongPassword {

    String message() default "Érvénytelen jelszó.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
