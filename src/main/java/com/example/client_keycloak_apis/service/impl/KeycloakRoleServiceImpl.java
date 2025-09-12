package com.example.client_keycloak_apis.service.impl;

import com.example.client_keycloak_apis.service.CrudKeycloakService;
import com.example.client_keycloak_apis.dto.RoleCreationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakRoleServiceImpl implements CrudKeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();

    private String getClientUUID(String realm, String clientName, String authHeader) {
        try {
            String clientsUrl = "http://localhost:8080/admin/realms/" + realm + "/clients?clientId=" + clientName;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<java.util.List> response = restTemplate.exchange(clientsUrl, HttpMethod.GET, entity, java.util.List.class);
            if (response.getBody() == null || response.getBody().isEmpty()) {
                throw new RuntimeException("❌ Client not found: " + clientName);
            }

            Map<String, Object> clientData = (Map<String, Object>) response.getBody().get(0);
            return (String) clientData.get("id");
        } catch (Exception e) {
            log.error("❌ Failed to fetch client UUID: {}", e.getMessage());
            throw new RuntimeException("❌ Failed to fetch client UUID", e);
        }
    }

    @Override
    public boolean createRole(String realm, String clientName, RoleCreationRequest request, String authHeader) {
        try {
            String clientUUID = getClientUUID(realm, clientName, authHeader);
            String url = "http://localhost:8080/admin/realms/" + realm + "/clients/" + clientUUID + "/roles";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "name", request.getName(),
                    "description", request.getDescription()
            );

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("✅ Created role '{}' in client '{}'", request.getName(), clientName);
            return true;
        } catch (Exception e) {
            log.error("❌ Error creating role: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateRole(String realm, String clientName, String roleName, RoleCreationRequest request, String authHeader) {
        try {
            String clientUUID = getClientUUID(realm, clientName, authHeader);
            String url = "http://localhost:8080/admin/realms/" + realm + "/clients/" + clientUUID + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "name", request.getName(),
                    "description", request.getDescription()
            );

            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
            log.info("✅ Updated role '{}' → '{}' in client '{}'", roleName, request.getName(), clientName);
            return true;
        } catch (Exception e) {
            log.error("❌ Error updating role: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteRole(String realm, String clientName, String roleName, String authHeader) {
        try {
            String clientUUID = getClientUUID(realm, clientName, authHeader);
            String url = "http://localhost:8080/admin/realms/" + realm + "/clients/" + clientUUID + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);

            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("✅ Deleted role '{}' from client '{}'", roleName, clientName);
            return true;
        } catch (Exception e) {
            log.error("❌ Error deleting role: {}", e.getMessage());
            return false;
        }
    }
}
