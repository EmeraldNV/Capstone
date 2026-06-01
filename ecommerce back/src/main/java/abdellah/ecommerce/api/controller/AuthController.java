package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.auth.AuthResponse;
import abdellah.ecommerce.api.dto.auth.LoginRequest;
import abdellah.ecommerce.api.dto.auth.RegisterRequest;
import abdellah.ecommerce.api.dto.auth.RegistrationResponse;
import abdellah.ecommerce.api.dto.auth.VerifyEmailResponse;
import abdellah.ecommerce.service.AuthService;
import abdellah.ecommerce.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(emailVerificationService.verifyEmail(token));
    }
}
