package org.refit.refitbackend.global.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;
    private final String issuer;
    private final long accessExpMs;
    private final long refreshExpMs;
    private SecretKey secretKey;
    private JwtParser parser;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.issuer}") String issuer,
            @Value("${spring.jwt.access-exp-ms}") long accessExpMs,
            @Value("${spring.jwt.refresh-exp-ms}") long refreshExpMs
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build();
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, accessExpMs, false);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshExpMs, true);
    }

    public boolean isValid(String token) {
        try {
            parser.parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parser.parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public Long getUserId(String token) {
        try {
            String sub = parser.parseSignedClaims(token).getPayload().getSubject();
            return Long.parseLong(sub);
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (JwtException | NumberFormatException e) {
            throw new CustomException(ExceptionType.AUTH_INVALID_TOKEN);
        }
    }

    public boolean isRefresh(String token) {
        String typ = parser.parseSignedClaims(token).getPayload().get("typ", String.class);
        return "refresh".equals(typ);
    }

    public Date getExpiration(String token) {
        return parser.parseSignedClaims(token).getPayload().getExpiration();
    }

    public static String extractBearer(String authorization) {
        if (authorization == null) return null;
        String prefix = "Bearer ";
        return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : null;
    }

    public Long extractUserIdAllowExpired(String token) {
        try {
            return getUserId(token);
        } catch (ExpiredJwtException e) {
            return Long.valueOf(e.getClaims().getSubject());
        }
    }

    private String createToken(Long userId, long expMs, boolean isRefresh) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .claim("typ", isRefresh ? "refresh" : "access")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}