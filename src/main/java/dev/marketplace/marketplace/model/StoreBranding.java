package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "store_branding")
@Data
@EqualsAndHashCode(exclude = {"business"})
@ToString(exclude = {"business"})
public class StoreBranding {
    
    @Id
    @Column(name = "business_id")
    private UUID businessId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "business_id")
    private Business business;

    @Column(name = "logo_url")
    private String logoUrl;
    
    @Column(name = "banner_url")
    private String bannerUrl;
    
    @Column(name = "theme_color")
    private String themeColor;
    
    private String about;
    
    @Column(name = "store_name")
    private String storeName;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "light_or_dark")
    private String lightOrDark;

    @Column(name = "text_color")
    private String textColor;

    @Column(name = "card_text_color")
    private String cardTextColor;

    @Column(name = "background_color")
    private String backgroundColor;
}
