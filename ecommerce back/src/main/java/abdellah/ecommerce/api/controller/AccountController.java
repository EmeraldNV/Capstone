package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.account.UpdateProfileRequest;
import abdellah.ecommerce.api.dto.account.UserProfileResponse;
import abdellah.ecommerce.security.UserPrincipal;
import abdellah.ecommerce.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateProfile(principal.getId(), request));
    }
}
