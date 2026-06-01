package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.auth.VerifyEmailResponse;
import abdellah.ecommerce.config.FrontendProperties;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.EmailVerificationToken;
import abdellah.ecommerce.domain.enums.AppUserStatus;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.repository.EmailVerificationTokenRepository;
import abdellah.ecommerce.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final AppUserRepository appUserRepository;
    private final ResendEmailService resendEmailService;
    private final FrontendProperties frontendProperties;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    AppUserRepository appUserRepository,
                                    ResendEmailService resendEmailService,
                                    FrontendProperties frontendProperties) {
        this.tokenRepository = tokenRepository;
        this.appUserRepository = appUserRepository;
        this.resendEmailService = resendEmailService;
        this.frontendProperties = frontendProperties;
    }

    @Transactional
    public Instant createAndSendVerificationToken(AppUser user) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = hash(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);

        String verificationUrl = frontendProperties.getBaseUrl()
                + "/auth/verify-email?token="
                + rawToken;
        resendEmailService.sendVerificationEmail(user.getEmail(), verificationUrl);
        return expiresAt;
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestApiException("VERIFICATION_TOKEN_REQUIRED", "Verification token is required.");
        }

        String tokenHash = hash(rawToken.trim());
        EmailVerificationToken token = tokenRepository
                .findByTokenHashAndVerifiedAtIsNullAndExpiresAtAfter(tokenHash, Instant.now())
                .orElseThrow(() -> new BadRequestApiException("INVALID_VERIFICATION_TOKEN", "Verification token is invalid or expired."));

        AppUser user = token.getUser();
        user.setEmailVerified(Boolean.TRUE);
        user.setStatus(AppUserStatus.ACTIVE);
        appUserRepository.save(user);

        token.setVerifiedAt(Instant.now());
        tokenRepository.save(token);

        return new VerifyEmailResponse("Email verified successfully.", true);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash verification token.", ex);
        }
    }
}
