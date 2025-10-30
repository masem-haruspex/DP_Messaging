package com.mm_mk.Messaging.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.mm_mk.Messaging.service.JpaUserDetailsService;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final ActuatorUserAgentFilter actuatorUserAgentFilter;
    private final FrontendProperties frontendProperties;
    private final CorrelationFilter correlationFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JpaUserDetailsService jpaUserDetailsService;

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        logger.info("Configuring actuator security");

        http
                .securityMatcher("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(jpaUserDetailsService)
                .addFilterBefore(correlationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(actuatorUserAgentFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("Actuator security configured");
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain guestEndpointsSecurity(HttpSecurity http) throws Exception {
        logger.info("Configuring guest endpoints security");

        http
                .securityMatcher(
                        "/api/messages/**",
                        "/ws/**"
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/messages/**",
                                "/ws/**"
                        )
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/messages/**",
                                "/ws/**"
                        ).permitAll()
                )
                .addFilterBefore(correlationFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("Guest endpoints security configured - all messaging and WebSocket endpoints are public");
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain fallbackSecurity(HttpSecurity http) throws Exception {
        logger.info("Configuring fallback security");

        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(correlationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        logger.info("Fallback security configured - all other endpoints require authentication");
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            logger.info("Configuring JWT decoder with shared public key");
            RSAPublicKey publicKey = loadPublicKey();
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            logger.error("Failed to configure JWT decoder", e);
            throw new RuntimeException("JWT decoder configuration failed", e);
        }
    }

    private RSAPublicKey loadPublicKey() throws Exception {
        logger.debug("Loading public key from certifications/public.pem");
        var resource = new ClassPathResource("certifications/public.pem");
        try (InputStream is = resource.getInputStream()) {
            String key = new String(is.readAllBytes())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS with allowed origins: {}", frontendProperties.getUrls());

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(frontendProperties.getUrls());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN", "X-Correlation-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}