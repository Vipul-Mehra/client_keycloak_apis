package com.paxaris.identity_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakProperties {

    private String baseUrl;
    private String tokenUrlPattern;  // dynamic token URL pattern
    private String grantType;
}
