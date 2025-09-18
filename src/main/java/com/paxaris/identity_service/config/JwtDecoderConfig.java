package com.paxaris.identity_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            // Decode JWT payload
            String[] parts = token.split("\\.");
            if (parts.length < 2) throw new JwtException("Invalid JWT");

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

            String issuer;
            try {
                issuer = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                        .build()
                        .readTree(payload)
                        .get("iss")
                        .asText();
            } catch (Exception e) {
                throw new JwtException("Failed to parse JWT issuer", e);
            }

            try {
                // Fetch public keys from the issuer dynamically
                return NimbusJwtDecoder.withJwkSetUri(issuer + "/protocol/openid-connect/certs")
                        .build()
                        .decode(token);
            } catch (Exception e) {
                throw new JwtException("Failed to decode JWT from issuer: " + issuer, e);
            }
        };
    }
}
