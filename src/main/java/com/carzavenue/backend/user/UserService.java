package com.carzavenue.backend.user;

import com.carzavenue.backend.car.CarService;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.user.dto.UpdateProfileRequest;
import com.carzavenue.backend.user.dto.UserProfileResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CarService carService;

    public UserService(UserRepository userRepository, CarService carService) {
        this.userRepository = userRepository;
        this.carService = carService;
    }

    public UserProfileResponse me(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse update(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);
        return toProfile(user);
    }

    public List<CarResponse> myListings(Long userId) {
        return carService.listByOwner(userId);
    }

    private UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isBlocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
