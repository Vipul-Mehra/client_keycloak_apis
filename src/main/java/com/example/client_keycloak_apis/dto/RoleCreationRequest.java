package com.example.client_keycloak_apis.dto;

import lombok.Data;

@Data
public class RoleCreationRequest {
    private String name;
    private String description;
    private String url;
    private String uri;
}
