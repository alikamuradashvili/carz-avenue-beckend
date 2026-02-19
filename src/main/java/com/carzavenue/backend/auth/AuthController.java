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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final PasswordResetService passwordResetService;
    private final Environment environment;

    public AuthController(AuthService authService,
                          GoogleOAuthService googleOAuthService,
                          PasswordResetService passwordResetService,
                          Environment environment) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
        this.passwordResetService = passwordResetService;
        this.environment = environment;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
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
            if (environment.acceptsProfiles(Profiles.of("local", "debug"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Google OAuth not configured (local mode)"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(required = false) String code,
                                               @RequestParam(required = false) String state,
                                               @RequestParam(required = false) String error,
                                               @RequestParam(required = false, name = "error_description") String errorDescription,
                                               jakarta.servlet.http.HttpServletRequest request) {
        log.info("Google callback hit: uri={} query={}", request.getRequestURI(), request.getQueryString());
        log.info("Google callback params -> code: {}, state: {}, error: {}, error_description: {}", code, state, error, errorDescription);
        String baseUrl = googleOAuthService.getFrontendCallbackUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (error != null) {
            builder.queryParam("error", error);
            if (errorDescription != null) {
                builder.queryParam("message", errorDescription);
            }
            return ResponseEntity.status(302).location(builder.encode().build().toUri()).build();
        }
        try {
            TokenResponse tokens = googleOAuthService.handleCallback(code, state);
            builder.queryParam("token", tokens.getAccessToken());
            builder.queryParam("refreshToken", tokens.getRefreshToken());
            return ResponseEntity.status(302).location(builder.encode().build().toUri()).build();
        } catch (Exception ex) {
            log.warn("Google OAuth callback failed", ex);
            builder.queryParam("error", "oauth_failed");
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = "OAuth failed";
            }
            builder.queryParam("message", message);
            return ResponseEntity.status(302).location(builder.encode().build().toUri()).build();
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> googleCallbackAlias(@RequestParam(required = false) String code,
                                                    @RequestParam(required = false) String state,
                                                    @RequestParam(required = false) String error,
                                                    @RequestParam(required = false, name = "error_description") String errorDescription,
                                                    jakarta.servlet.http.HttpServletRequest request) {
        return googleCallback(code, state, error, errorDescription, request);
    }
}
