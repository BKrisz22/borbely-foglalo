package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.schedule.dto.TimeOffCreateRequest;
import hu.szakdolgozat.borbely.schedule.dto.TimeOffResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * A borbély távolléteinek végpontjai ({@code /api/barbers/{barberId}/time-off}).
 * A borbély a sajátját, a tulajdonos bárkiét kezelheti.
 */
@RestController
@RequestMapping("/api/barbers/{barberId}/time-off")
public class TimeOffController {

    private final TimeOffService timeOffs;

    public TimeOffController(TimeOffService timeOffs) {
        this.timeOffs = timeOffs;
    }

    /** A két nap (helyi idő szerint, mindkettőt beleértve) közé eső távollétek. */
    @GetMapping
    public List<TimeOffResponse> list(
            @PathVariable("barberId") Long barberId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return timeOffs.list(barberId, from, to, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeOffResponse create(@PathVariable("barberId") Long barberId,
                                  @Valid @RequestBody TimeOffCreateRequest request,
                                  Authentication authentication) {
        return timeOffs.create(barberId, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("barberId") Long barberId,
                       @PathVariable("id") Long id,
                       Authentication authentication) {
        timeOffs.delete(barberId, id, authentication);
    }
}
