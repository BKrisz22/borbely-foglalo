package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.barber.dto.BarberCreateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberProfileRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberResponse;
import hu.szakdolgozat.borbely.barber.dto.BarberServicesRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberUpdateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberView;
import hu.szakdolgozat.borbely.security.Authentications;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

    /**
     * Borbélylista. A tulajdonos a teljes adatokat kapja (e-mail-címmel, kérésre a kikapcsoltakkal is),
     * mindenki más csak az aktív borbélyok nyilvános adatait. Bejelentkezés nélkül az authentication null.
     */
    @GetMapping
    public List<? extends BarberView> list(
            @RequestParam(name = "serviceId", required = false) Long serviceId,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
            Authentication authentication) {
        if (Authentications.isOwner(authentication)) {
            return barbers.list(serviceId, includeInactive);
        }
        if (includeInactive) {
            throw new AccessDeniedException("A kikapcsolt borbélyokat csak a tulajdonos láthatja.");
        }
        return barbers.listPublic(serviceId);
    }

    /** Egy borbély: a tulajdonosnak a teljes adatok, másnak csak az aktív borbély nyilvános adatai. */
    @GetMapping("/{id}")
    public BarberView get(@PathVariable("id") Long id, Authentication authentication) {
        return Authentications.isOwner(authentication) ? barbers.get(id) : barbers.getPublic(id);
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

    /** A borbély saját profilja (bemutatkozás, foglalási határidő); a borbély a sajátját, a tulajdonos bárkiét. */
    @PutMapping("/{id}/profile")
    public BarberResponse updateProfile(@PathVariable("id") Long id,
                                        @Valid @RequestBody BarberProfileRequest request,
                                        Authentication authentication) {
        return barbers.updateProfile(id, request, authentication);
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
