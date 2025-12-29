package com.carzavenue.backend.auth;

import com.carzavenue.backend.auth.dto.*;
import com.carzavenue.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Validated @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Validated @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Validated @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Validated @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserDetails principal,
                                                            @Validated @RequestBody ChangePasswordRequest request) {
        authService.changePassword(((com.carzavenue.backend.security.SecurityUser) principal).getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(@AuthenticationPrincipal UserDetails principal,
                                                         @Validated @RequestBody ChangeEmailRequest request) {
        authService.changeEmail(((com.carzavenue.backend.security.SecurityUser) principal).getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
