package hu.szakdolgozat.borbely.serviceoffering;

import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingRequest;
import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * A szolgáltatások REST-végpontjai ({@code /api/services}):
 * listázás, lekérdezés, létrehozás, módosítás és kikapcsolás.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferings;

    public ServiceOfferingController(ServiceOfferingService serviceOfferings) {
        this.serviceOfferings = serviceOfferings;
    }

    @GetMapping
    public List<ServiceOfferingResponse> list(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return serviceOfferings.list(includeInactive);
    }

    @GetMapping("/{id}")
    public ServiceOfferingResponse get(@PathVariable("id") Long id) {
        return serviceOfferings.get(id);
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingResponse> create(@Valid @RequestBody ServiceOfferingRequest request) {
        ServiceOfferingResponse created = serviceOfferings.create(request);
        return ResponseEntity.created(URI.create("/api/services/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ServiceOfferingResponse update(
            @PathVariable("id") Long id, @Valid @RequestBody ServiceOfferingRequest request) {
        return serviceOfferings.update(id, request);
    }

    /**
     * Nem töröl: csak kikapcsolja a szolgáltatást,
     * hogy a korábbi foglalások hivatkozása megmaradjon.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") Long id) {
        serviceOfferings.deactivate(id);
    }
}
