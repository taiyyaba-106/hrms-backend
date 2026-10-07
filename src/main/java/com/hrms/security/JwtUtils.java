package com.hrms.security;

import com.hrms.config.JwtProperties;
import com.hrms.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final JwtProperties jwtProperties;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateTokenForUser(User user) {
        List<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList())
                : Collections.emptyList();

        Set<String> permissionsSet = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(perm -> permissionsSet.add(perm.getName()));
                }
            });
        }
        List<String> permissions = new ArrayList<>(permissionsSet);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

            List<String> roles = principal.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .collect(Collectors.toList());

            List<String> permissions = principal.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> !a.startsWith("ROLE_"))
                    .distinct()
                    .collect(Collectors.toList());

            return Jwts.builder()
                    .subject(principal.getEmail())
                    .claim("userId", principal.getId())
                    .claim("email", principal.getEmail())
                    .claim("roles", roles)
                    .claim("permissions", permissions)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(key(), Jwts.SIG.HS256)
                    .compact();
        } else {
            return generateTokenFromUsername(authentication.getName());
        }
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(username)
                .claim("email", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
