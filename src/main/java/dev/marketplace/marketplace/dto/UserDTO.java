package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String username;
    private String email;
    private Role role;
    private String profileImageUrl;
    private String firstName;
    private String lastName;
    private String bio;
    private UUID cityId;
    private String cityName;
    private String customCity;
    private String contactNumber;
    private String idNumber;
    private LocalDateTime createdAt;
}

