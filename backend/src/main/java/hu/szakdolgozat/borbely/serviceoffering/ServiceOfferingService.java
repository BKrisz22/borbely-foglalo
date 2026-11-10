package hu.szakdolgozat.borbely.serviceoffering;

import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingRequest;
import hu.szakdolgozat.borbely.serviceoffering.dto.ServiceOfferingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository repository;

    public ServiceOfferingService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> list(boolean includeInactive) {
        List<ServiceOffering> services = includeInactive
                ? repository.findAllByOrderByNameAsc()
                : repository.findAllByActiveTrueOrderByNameAsc();
        return services.stream().map(ServiceOfferingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceOfferingResponse get(Long id) {
        return ServiceOfferingResponse.from(find(id));
    }

    @Transactional
    public ServiceOfferingResponse create(ServiceOfferingRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        ServiceOffering service = new ServiceOffering(
                name, blankToNull(request.description()), request.durationMinutes(), request.priceHuf());
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return ServiceOfferingResponse.from(repository.save(service));
    }

    @Transactional
    public ServiceOfferingResponse update(Long id, ServiceOfferingRequest request) {
        ServiceOffering service = find(id);
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        service.setName(name);
        service.setDescription(blankToNull(request.description()));
        service.setDurationMinutes(request.durationMinutes());
        service.setPriceHuf(request.priceHuf());
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return ServiceOfferingResponse.from(service);
    }

    @Transactional
    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    private ServiceOffering find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen szolgáltatás: " + id));
    }

    private static ConflictException duplicateName(String name) {
        return new ConflictException("Már létezik ilyen nevű szolgáltatás: " + name);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
