package hu.szakdolgozat.borbely.slot;

import hu.szakdolgozat.borbely.slot.dto.SlotResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * A foglalási tölcsér nyilvános végpontjai: melyik napokon és mikor lehet egy borbélyhoz
 * egy adott szolgáltatásra foglalni. Bejelentkezés nélkül is elérhetők.
 */
@RestController
@RequestMapping("/api/barbers/{barberId}")
public class SlotController {

    private final SlotService slots;

    public SlotController(SlotService slots) {
        this.slots = slots;
    }

    /** Egy nap szabad kezdőpontjai, például {@code /api/barbers/1/slots?serviceId=2&date=2026-12-07}. */
    @GetMapping("/slots")
    public List<SlotResponse> slots(
            @PathVariable("barberId") Long barberId,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slots.findSlots(barberId, serviceId, date);
    }

    /** A foglalható napok két dátum között, például a naptár egy hónapjára. */
    @GetMapping("/available-days")
    public List<LocalDate> availableDays(
            @PathVariable("barberId") Long barberId,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return slots.findAvailableDays(barberId, serviceId, from, to);
    }
}
