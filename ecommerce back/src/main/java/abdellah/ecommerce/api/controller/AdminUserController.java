package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.admin.AdminAuditLogResponse;
import abdellah.ecommerce.api.dto.admin.AdminCreateUserRequest;
import abdellah.ecommerce.api.dto.admin.AdminUserResponse;
import abdellah.ecommerce.api.dto.admin.AdminUserUpdateRequest;
import abdellah.ecommerce.api.dto.admin.AssignRolesRequest;
import abdellah.ecommerce.api.dto.admin.RoleAssignmentResponse;
import abdellah.ecommerce.api.dto.user.UserResponse;
import abdellah.ecommerce.security.UserPrincipal;
import abdellah.ecommerce.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminUserService.listUsers(search));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminAuditLogResponse>> listAuditLogs(@RequestParam(required = false) String entityName) {
        return ResponseEntity.ok(adminUserService.listRecentAuditLogs(entityName));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal,
                                                   HttpServletRequest httpServletRequest) {
        UserResponse response = adminUserService.createUser(
                request,
                principal,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> updateUser(@PathVariable Long userId,
                                                        @Valid @RequestBody AdminUserUpdateRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal,
                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(adminUserService.updateUser(
                userId,
                request,
                principal,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> deactivateUser(@PathVariable Long userId,
                                                            @AuthenticationPrincipal UserPrincipal principal,
                                                            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(adminUserService.deactivateUser(
                userId,
                principal,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<RoleAssignmentResponse> assignRoles(@PathVariable Long userId,
                                                              @Valid @RequestBody AssignRolesRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal,
                                                              HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(adminUserService.assignRoles(
                userId,
                request,
                principal,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        ));
    }
}
