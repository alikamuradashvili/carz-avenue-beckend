package com.carzavenue.backend.user;

import com.carzavenue.backend.car.dto.CarResponse;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.security.SecurityUser;
import com.carzavenue.backend.user.dto.UpdateProfileRequest;
import com.carzavenue.backend.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.me(principal.getUser().getId())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(@AuthenticationPrincipal SecurityUser principal,
                                                                   @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(principal.getUser().getId(), request)));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<CarResponse>>> myListings(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.myListings(principal.getUser().getId())));
    }
}
