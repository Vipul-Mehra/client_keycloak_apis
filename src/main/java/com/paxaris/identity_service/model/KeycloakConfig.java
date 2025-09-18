package com.paxaris.identity_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {
    private String baseUrl = "http://localhost:8080";
    private String realm = "master";
    private String adminUsername;
    private String adminPassword;
}