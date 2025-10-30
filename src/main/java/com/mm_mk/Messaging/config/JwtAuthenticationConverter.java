package com.mm_mk.Messaging.config;

import com.mm_mk.Messaging.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);
    private final AdminUserRepository adminUserRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        logger.debug("Converting JWT to authentication token - claims: {}", jwt.getClaims());

        String subject = jwt.getSubject();
        if (subject == null) {
            logger.error("JWT token missing subject claim. Available claims: {}", jwt.getClaims().keySet());
            throw new RuntimeException("Missing subject claim in JWT");
        }

        try {
            // Parse UUID from subject
            UUID userId = UUID.fromString(subject);

            // Get username for logging (optional)
            String username = jwt.getClaimAsString("username");
            logger.debug("JWT conversion - userId: {}, username: {}", userId, username);

            Collection<GrantedAuthority> authorities = extractAuthorities(userId);

            logger.debug("JWT conversion successful - userId: {}, authorities: {}", userId, authorities);
            return new JwtAuthenticationToken(jwt, authorities);
        } catch (IllegalArgumentException e) {
            logger.error("JWT subject is not a valid UUID: {}. Available claims: {}", subject, jwt.getClaims().keySet());
            throw new RuntimeException("JWT subject must be a valid UUID", e);
        } catch (Exception e) {
            logger.error("Failed to convert JWT for subject: {}", subject, e);
            throw new RuntimeException("JWT conversion failed", e);
        }
    }

    private Collection<GrantedAuthority> extractAuthorities(UUID userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // All authenticated users get ROLE_USER
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Check if user is admin
        boolean isAdmin = adminUserRepository.isUserAdmin(userId);
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            logger.debug("User {} granted ROLE_ADMIN", userId);
        } else {
            logger.debug("User {} has ROLE_USER only", userId);
        }

        return authorities;
    }
}