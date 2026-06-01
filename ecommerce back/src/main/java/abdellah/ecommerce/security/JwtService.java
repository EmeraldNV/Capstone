package abdellah.ecommerce.security;

import abdellah.ecommerce.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildKey(properties.getSecret());
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    private SecretKey buildKey(String secret) {
        byte[] keyBytes;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required");
        }
        keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
