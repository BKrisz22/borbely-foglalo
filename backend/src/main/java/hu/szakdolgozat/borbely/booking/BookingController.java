package hu.szakdolgozat.borbely.booking;

import hu.szakdolgozat.borbely.booking.dto.BookingCreateRequest;
import hu.szakdolgozat.borbely.booking.dto.BookingResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * A foglalások végpontjai ({@code /api/bookings}). Mindegyikhez bejelentkezés kell; hogy ki mit
 * láthat és tehet, azt a {@link BookingService} dönti el a szerepkör alapján.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingCreateRequest request,
                                                  Authentication authentication) {
        BookingResponse created = bookings.create(request, authentication);
        return ResponseEntity.created(URI.create("/api/bookings/" + created.id())).body(created);
    }

    /** Foglalások két nap között, például {@code ?from=2026-12-01&to=2026-12-31}, elhagyható borbélyszűrővel. */
    @GetMapping
    public List<BookingResponse> list(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "barberId", required = false) Long barberId,
            Authentication authentication) {
        return bookings.list(from, to, barberId, authentication);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable("id") Long id, Authentication authentication) {
        return bookings.get(id, authentication);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable("id") Long id, Authentication authentication) {
        return bookings.cancel(id, authentication);
    }

    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable("id") Long id, Authentication authentication) {
        return bookings.complete(id, authentication);
    }

    @PostMapping("/{id}/no-show")
    public BookingResponse noShow(@PathVariable("id") Long id, Authentication authentication) {
        return bookings.markNoShow(id, authentication);
    }
}
