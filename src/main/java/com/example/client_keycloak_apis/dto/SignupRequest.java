package com.example.client_keycloak_apis.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String realmName;
    private String clientId;
    private boolean publicClient = true;

    private AdminUser adminUser;

}