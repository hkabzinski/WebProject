package com.example.store.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistrationService userRegistrationService;
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    public UserController(UserRegistrationService userRegistrationService,
                          AuthenticationService authenticationService,
                          PasswordResetService passwordResetService) {
        this.userRegistrationService = userRegistrationService;
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.ok(userRegistrationService.registerUser(request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.ok(LoginResponse.of(authenticationService.login(request.email(), request.password())));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetTokenResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request.email()));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmation confirmation) {
        passwordResetService.resetPassword(confirmation.token(), confirmation.newPassword());
        return ResponseEntity.noContent().build();
    }
}
