package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.user.AppUser;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "barber")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "min_lead_minutes", nullable = false)
    private int minLeadMinutes = 60;

    @ManyToMany
    @JoinTable(
            name = "barber_service",
            joinColumns = @JoinColumn(name = "barber_id"),
            inverseJoinColumns = @JoinColumn(name = "service_offering_id")
    )
    private Set<ServiceOffering> services = new HashSet<>();

    protected Barber() {
    }

    public Barber(AppUser user, String bio) {
        this.user = user;
        this.bio = bio;
    }

    public void addService(ServiceOffering service) {
        services.add(service);
    }

    public void removeService(ServiceOffering service) {
        services.remove(service);
    }

    public boolean offers(ServiceOffering service) {
        return services.contains(service);
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMinLeadMinutes() {
        return minLeadMinutes;
    }

    public void setMinLeadMinutes(int minLeadMinutes) {
        this.minLeadMinutes = minLeadMinutes;
    }

    public Set<ServiceOffering> getServices() {
        return Collections.unmodifiableSet(services);
    }
}
