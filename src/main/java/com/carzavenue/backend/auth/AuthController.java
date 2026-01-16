package com.carzavenue.backend.auth;

import com.carzavenue.backend.auth.dto.*;
import com.carzavenue.backend.common.ApiResponse;
import com.carzavenue.backend.security.SecurityUser;
import com.carzavenue.backend.user.dto.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

    public AuthController(AuthService authService, GoogleOAuthService googleOAuthService) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(@AuthenticationPrincipal SecurityUser principal) {
        var user = principal.getUser();
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isBlocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build()));
    }

    @GetMapping("/google/url")
    public ResponseEntity<ApiResponse<String>> googleAuthUrl() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(googleOAuthService.buildAuthorizationUrl()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(required = false) String code,
                                               @RequestParam(required = false) String state,
                                               @RequestParam(required = false) String error,
                                               @RequestParam(required = false, name = "error_description") String errorDescription) {
        String baseUrl = googleOAuthService.getFrontendCallbackUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (error != null) {
            builder.queryParam("error", error);
            if (errorDescription != null) {
                builder.queryParam("message", errorDescription);
            }
            return ResponseEntity.status(302).location(builder.build(true).toUri()).build();
        }
        try {
            TokenResponse tokens = googleOAuthService.handleCallback(code, state);
            builder.queryParam("token", tokens.getAccessToken());
            builder.queryParam("refreshToken", tokens.getRefreshToken());
            return ResponseEntity.status(302).location(builder.build(true).toUri()).build();
        } catch (Exception ex) {
            builder.queryParam("error", "oauth_failed");
            builder.queryParam("message", ex.getMessage());
            return ResponseEntity.status(302).location(builder.build(true).toUri()).build();
        }
    }
}
