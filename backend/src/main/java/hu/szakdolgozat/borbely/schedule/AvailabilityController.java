package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.schedule.dto.AvailabilityCreateRequest;
import hu.szakdolgozat.borbely.schedule.dto.AvailabilityResponse;
import hu.szakdolgozat.borbely.schedule.dto.AvailabilityUpdateRequest;
import hu.szakdolgozat.borbely.schedule.dto.CopyWeekRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * A borbély munkaidő-sávjainak végpontjai ({@code /api/barbers/{barberId}/availability}).
 * A borbély a sajátját, a tulajdonos bárkiét kezelheti. A vendég ezt nem látja;
 * neki majd a szabad kezdőpontok végpontja szól.
 */
@RestController
@RequestMapping("/api/barbers/{barberId}/availability")
public class AvailabilityController {

    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    /** A sávok két nap között (mindkettőt beleértve), például {@code ?from=2026-12-01&to=2026-12-31}. */
    @GetMapping
    public List<AvailabilityResponse> list(
            @PathVariable("barberId") Long barberId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return availability.list(barberId, from, to, authentication);
    }

    /** Egy vagy több sáv felvétele; vagy mind bekerül, vagy egyik sem. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<AvailabilityResponse> create(@PathVariable("barberId") Long barberId,
                                             @Valid @RequestBody AvailabilityCreateRequest request,
                                             Authentication authentication) {
        return availability.create(barberId, request.slots(), authentication);
    }

    /** Az egyik hét sávjainak átmásolása egy másik hétre („előző hét másolása”). */
    @PostMapping("/copy-week")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AvailabilityResponse> copyWeek(@PathVariable("barberId") Long barberId,
                                               @Valid @RequestBody CopyWeekRequest request,
                                               Authentication authentication) {
        return availability.copyWeek(barberId, request, authentication);
    }

    @PutMapping("/{id}")
    public AvailabilityResponse update(@PathVariable("barberId") Long barberId,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody AvailabilityUpdateRequest request,
                                       Authentication authentication) {
        return availability.update(barberId, id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("barberId") Long barberId,
                       @PathVariable("id") Long id,
                       Authentication authentication) {
        availability.delete(barberId, id, authentication);
    }
}
