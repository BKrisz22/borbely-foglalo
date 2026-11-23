package hu.szakdolgozat.borbely.customer;

import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.customer.dto.CustomerResponse;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Vendégkereső a személyzetnek (telefonos foglaláshoz): aktív vendégek név vagy e-mail-cím
 * részlete alapján, legfeljebb {@value #MAX_RESULTS} találattal. Kis- és nagybetűre nem érzékeny.
 */
@Service
public class CustomerService {

    static final int MIN_QUERY_LENGTH = 2;
    static final int MAX_RESULTS = 20;

    private final AppUserRepository users;

    public CustomerService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            throw new BadRequestException("A kereséshez legalább " + MIN_QUERY_LENGTH + " karaktert adj meg.");
        }
        String pattern = "%" + escapeLike(trimmed.toLowerCase(Locale.ROOT)) + "%";
        return users.searchCustomers(pattern, PageRequest.of(0, MAX_RESULTS)).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    /** A LIKE minta különleges karakterei (\, %, _) szó szerint értendők, nem helyettesítő karakterek. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
