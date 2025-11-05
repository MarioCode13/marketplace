package dev.marketplace.marketplace.mapper;

import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.model.User;

public class UserMapper {
    public static UserDTO toDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setBio(user.getBio());
        dto.setCityId(user.getCity() != null ? user.getCity().getId() : null);
        dto.setCityName(user.getCity() != null ? user.getCity().getName() : null);
        dto.setCustomCity(user.getCustomCity());
        dto.setContactNumber(user.getContactNumber());
        dto.setIdNumber(user.getIdNumber());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public static User toEntity(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setBio(dto.getBio());
        user.setCustomCity(dto.getCustomCity());
        user.setContactNumber(dto.getContactNumber());
        user.setIdNumber(dto.getIdNumber());
        user.setCreatedAt(dto.getCreatedAt());
        // city mapping intentionally omitted - service layer should resolve City by id
        return user;
    }
}

