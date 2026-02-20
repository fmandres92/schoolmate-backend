package com.schoolmate.api.security;

import com.schoolmate.api.enums.Rol;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    public String generateToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .claim("id", userPrincipal.getId() != null ? userPrincipal.getId().toString() : null)
                .claim("rol", userPrincipal.getRol().name())
                .claim("profesorId", userPrincipal.getProfesorId() != null ? userPrincipal.getProfesorId().toString() : null)
                .claim("apoderadoId", userPrincipal.getApoderadoId() != null ? userPrincipal.getApoderadoId().toString() : null)
                .claim("nombre", userPrincipal.getNombre())
                .claim("apellido", userPrincipal.getApellido())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = parseClaims(token);
        Rol rol = Rol.valueOf(claims.get("rol", String.class));

        return new UserPrincipal(
                parseUuid(claims.get("id", String.class)),
                claims.getSubject(),
                "",
                rol,
                parseUuid(claims.get("profesorId", String.class)),
                parseUuid(claims.get("apoderadoId", String.class)),
                claims.get("nombre", String.class),
                claims.get("apellido", String.class)
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return UUID.fromString(rawValue);
    }
}
