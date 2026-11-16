package hu.szakdolgozat.borbely.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Egységes hibakezelés: minden hibát RFC 9457 szerinti {@code ProblemDetail}
 * JSON-ként, magyar üzenettel ad vissza.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Nem található", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Hibás kérés", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Ütközés", ex.getMessage());
    }

    /**
     * Adatbázis-megkötés megsértése (például egyedi mező vagy átfedő foglalás).
     * A részleteket nem adja ki a kliensnek.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Ütközés", "A művelet ütközik a meglévő adatokkal.");
    }

    /**
     * Hibás jelszó, ismeretlen e-mail-cím vagy kikapcsolt fiók. Szándékosan mindháromra ugyanaz
     * a válasz, hogy a hibaüzenetből ne derüljön ki, létezik-e a fiók.
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ProblemDetail handleBadCredentials(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Sikertelen bejelentkezés", "Hibás e-mail-cím vagy jelszó.");
    }

    /** Hiányzó, hibás vagy lejárt token (401). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Bejelentkezés szükséges",
                "Ehhez a művelethez be kell jelentkezned.");
    }

    /** A felhasználó be van jelentkezve, de a szerepköre nem engedi a műveletet (403). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Hozzáférés megtagadva", "Nincs jogosultságod ehhez a művelethez.");
    }

    /** Validációs hibák: mezőnként az első hibaüzenet kerül az {@code errors} tulajdonságba. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Érvénytelen adatok", "Egy vagy több mező hibás.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
