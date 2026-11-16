package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.barber.dto.BarberCreateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberResponse;
import hu.szakdolgozat.borbely.barber.dto.BarberServicesRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * A borbélyok REST-végpontjai ({@code /api/barbers}): listázás szolgáltatás szerinti
 * szűréssel, lekérdezés, létrehozás, módosítás, a szolgáltatások cseréje és kikapcsolás.
 */
@RestController
@RequestMapping("/api/barbers")
public class BarberController {

    private final BarberService barbers;

    public BarberController(BarberService barbers) {
        this.barbers = barbers;
    }

    @GetMapping
    public List<BarberResponse> list(
            @RequestParam(name = "serviceId", required = false) Long serviceId,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return barbers.list(serviceId, includeInactive);
    }

    @GetMapping("/{id}")
    public BarberResponse get(@PathVariable("id") Long id) {
        return barbers.get(id);
    }

    @PostMapping
    public ResponseEntity<BarberResponse> create(@Valid @RequestBody BarberCreateRequest request) {
        BarberResponse created = barbers.create(request);
        return ResponseEntity.created(URI.create("/api/barbers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BarberResponse update(@PathVariable("id") Long id, @Valid @RequestBody BarberUpdateRequest request) {
        return barbers.update(id, request);
    }

    @PutMapping("/{id}/services")
    public BarberResponse replaceServices(
            @PathVariable("id") Long id, @Valid @RequestBody BarberServicesRequest request) {
        return barbers.replaceServices(id, request);
    }

    /**
     * Nem töröl: csak kikapcsolja a borbélyt,
     * hogy a korábbi foglalásai és adatai megmaradjanak.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") Long id) {
        barbers.deactivate(id);
    }
}
