package com.carzavenue.backend.user.dto;

import com.carzavenue.backend.user.Role;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserProfileResponse {
    Long id;
    String email;
    String name;
    String phoneNumber;
    Role role;
    boolean isBlocked;
    Instant createdAt;
    Instant updatedAt;
}
