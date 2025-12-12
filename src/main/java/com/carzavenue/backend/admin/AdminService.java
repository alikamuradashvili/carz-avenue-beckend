package com.carzavenue.backend.admin;

import com.carzavenue.backend.car.CarListing;
import com.carzavenue.backend.car.CarListingRepository;
import com.carzavenue.backend.car.CarMapper;
import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.user.User;
import com.carzavenue.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final CarListingRepository carListingRepository;
    private final UserRepository userRepository;

    public AdminService(CarListingRepository carListingRepository, UserRepository userRepository) {
        this.carListingRepository = carListingRepository;
        this.userRepository = userRepository;
    }

    public List<CarResponse> listAllListings() {
        return carListingRepository.findAll().stream()
                .map(CarMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteListing(Long id) {
        carListingRepository.deleteById(id);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void blockUser(Long id, boolean blocked) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setBlocked(blocked);
        userRepository.save(user);
    }
}
