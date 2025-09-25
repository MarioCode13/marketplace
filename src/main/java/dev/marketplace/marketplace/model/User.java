package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = true, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.HAS_ACCOUNT;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Listing> listings = new ArrayList<>();

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "bio")
    private String bio;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "custom_city")
    private String customCity;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "proof_of_address_url")
    private String proofOfAddressUrl;

    @Column(name = "id_photo_url")
    private String idPhotoUrl;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "drivers_license_url")
    private String driversLicenseUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private StoreBranding storeBranding;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public StoreBranding getStoreBranding() { return storeBranding; }
    public void setStoreBranding(StoreBranding storeBranding) { this.storeBranding = storeBranding; }
}
