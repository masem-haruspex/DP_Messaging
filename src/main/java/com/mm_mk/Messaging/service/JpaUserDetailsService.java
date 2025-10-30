package com.mm_mk.Messaging.service;

import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.repository.AdminUserRepository;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JpaUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(JpaUserDetailsService.class);

    private final LocalUserRepository localUserRepository;
    private final AdminUserRepository adminUserRepository;

    public JpaUserDetailsService(LocalUserRepository localUserRepository, AdminUserRepository adminUserRepository) {
        this.localUserRepository = localUserRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);

        // For Basic Auth, we need to find the user by username
        // Since you have admin user with ID 11111111-1111-1111-1111-111111111111
        // Let's assume the username is "admin" for this user

        if ("admin".equals(username)) {
            UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            LocalUser localUser = localUserRepository.findById(adminUserId)
                    .orElseThrow(() -> {
                        logger.error("Admin user not found in local_users with ID: {}", adminUserId);
                        return new UsernameNotFoundException("Admin user not found");
                    });

            logger.debug("Admin user found: {}", localUser.getUsername());

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            boolean isAdmin = adminUserRepository.isUserAdmin(localUser.getId());
            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                logger.debug("User {} granted ROLE_ADMIN", localUser.getUsername());
            }

            // For Basic Auth, we need to provide a password
            // Since you're using UUID-based auth, we can use a fixed password or the UUID
            return new User(
                    localUser.getUsername(),
                    "{noop}admin123", // Using {noop} for no password encoding
                    authorities
            );
        }

        logger.warn("User not found: {}", username);
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
