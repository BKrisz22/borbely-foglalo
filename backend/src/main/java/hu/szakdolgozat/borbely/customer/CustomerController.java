package hu.szakdolgozat.borbely.customer;

import hu.szakdolgozat.borbely.customer.dto.CustomerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vendégkereső ({@code GET /api/customers?query=…}). Csak a borbély és a tulajdonos érheti el
 * (lásd SecurityConfig), mert vendégek nevét és e-mail-címét adja vissza.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    public List<CustomerResponse> search(@RequestParam("query") String query) {
        return customers.search(query);
    }
}
