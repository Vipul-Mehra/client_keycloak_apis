package com.example.client_keycloak_apis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbRoleRequest {
    private String realmName;
    private String productName;
    private String roleName;
    private String url;
    private String uri;
}
