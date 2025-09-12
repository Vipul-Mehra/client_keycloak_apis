package com.example.client_keycloak_apis.service.impl;

import com.example.client_keycloak_apis.dto.RoleCreationRequest;
import com.example.client_keycloak_apis.dto.SignupRequest;
import com.example.client_keycloak_apis.model.KeycloakConfig;
import com.example.client_keycloak_apis.service.KeycloakClientService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakClientServiceImpl implements KeycloakClientService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakClientServiceImpl.class);

    private final KeycloakConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ---------------- TOKEN ----------------

    private String getMasterToken() {
        String tokenUrl = "http://localhost:8080/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "admin123");


        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        System.out.println(request);
        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    @Override
    public Map<String, Object> getMyRealmToken(String username, String password, String clientId, String clientSecret, String realm) {
        String tokenUrl = config.getBaseUrl() + "/realms/" + realm + "/protocol/openid-connect/token";
        log.info("Requesting token from Keycloak:");
        log.info(" → Realm: {}", realm);
        log.info(" → Token URL: {}", tokenUrl);
        log.info(" → Client ID: {}", clientId);
        log.info(" → Client Secret Provided: {}", (clientSecret != null && !clientSecret.isEmpty()));
        log.info(" → Username: {}", username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            formData.add("client_secret", clientSecret);
        }
        formData.add("username", username);
        formData.add("password", password);

        log.debug("Form Data to Keycloak: {}", formData);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);
            log.info("Keycloak Response Status: {}", response.getStatusCode());
            log.debug("Keycloak Raw Response Body: {}", response.getBody());
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("❌ Failed to get token for realm {}. Error: {}", realm, e.getMessage(), e);
            throw new RuntimeException("Failed to get token for realm " + realm, e);
        }
    }

    // ---------------- CLIENT ----------------
    @Override
    public String createClient(String realm, String clientId, boolean isPublicClient, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients";

        Map<String, Object> body = new HashMap<>();
        body.put("clientId", clientId);
        body.put("enabled", true);
        body.put("protocol", "openid-connect");
        body.put("publicClient", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create client: " + response.getStatusCode() + " " + response.getBody());
        }
        return getClientUUID(realm, clientId, token);
    }

    @Override
    public List<Map<String, Object>> getAllClients(String realm, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse clients list", e);
            throw new RuntimeException("Failed to parse clients list", e);
        }
    }

    // ---------------- USER ----------------
    @Override
    public String createUser(String realm, String token, Map<String, Object> userMap) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userMap, headers);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String location = response.getHeaders().getFirst("Location");
            if (location != null) {
                return location.substring(location.lastIndexOf("/") + 1);
            }
        }
        throw new RuntimeException("Failed to create user. Status: " + response.getStatusCode());
    }

    @Override
    public List<Map<String, Object>> getAllUsers(String realm, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse users list", e);
            throw new RuntimeException("Failed to parse users list", e);
        }
    }

    // ---------------- ROLE ----------------
    @Override
    public void createClientRoles(String realm, String clientName, List<RoleCreationRequest> roleRequests, String token) {
        String clientUUID = getClientUUID(realm, clientName, token);
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients/" + clientUUID + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        for (RoleCreationRequest role : roleRequests) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", role.getName());
            body.put("description", role.getDescription());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            try {
                restTemplate.postForEntity(url, entity, String.class);
                log.info("✅ Created role '{}' in client '{}', url: {}, uri: {}",
                        role.getName(), clientName, role.getUrl(), role.getUri());
            } catch (Exception e) {
                log.error("❌ Failed to create role '{}' in client '{}': {}", role.getName(), clientName, e.getMessage());
            }
        }
    }

    private String getClientUUID(String realm, String clientName, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients?clientId=" + clientName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() != null && !response.getBody().isEmpty()) {
            return (String) response.getBody().get(0).get("id");
        }
        throw new RuntimeException("Client not found in Keycloak: " + clientName);
    }



    @Override
    public void createRealmRole(String realm, String roleName, String clientId, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/roles";
        Map<String, Object> body = new HashMap<>();
        body.put("name", roleName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    @Override
    public List<Map<String, Object>> getAllRoles(String realm, String clientId, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/roles";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse roles list", e);
            throw new RuntimeException("Failed to parse roles list", e);
        }
    }

    // ---------------- ROLE ASSIGNMENT ----------------
    @Override
    public void assignClientRole(String realm, String userIdentifier, String clientIdentifier, String roleId, String roleName, String token) {
        String userUUID = resolveUserId(realm, userIdentifier, token);
        String clientUUID = getClientUUID(realm, clientIdentifier, token);
        if (roleId == null || roleId.isEmpty()) {
            roleId = getClientRoleId(realm, clientUUID, roleName, token);
        }
        assignClientRoleToUser(realm, userUUID, clientUUID, roleId, roleName, token);
    }

    @Override
    public void assignClientRoleToUser(String realm, String userId, String clientUUID, String roleId, String roleName, String token) {
        if (roleId == null && roleName != null) {
            roleId = getClientRoleId(realm, clientUUID, roleName, token);
        }
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/clients/" + clientUUID;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<Map<String, Object>> roles = List.of(Map.of(
                "id", roleId,
                "name", roleName
        ));
        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(roles, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    // ---------------- UTILITY ----------------
    @Override
    public String getClientSecret(String realm, String clientId, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients/{clientId}/client-secret";
        Map<String, String> uriVars = Map.of("clientId", clientId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class, uriVars);
        try {
            Map<String, Object> result = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            return (String) result.get("value");
        } catch (Exception e) {
            log.error("Failed to get client secret", e);
            throw new RuntimeException("Failed to get client secret", e);
        }
    }

//    private String getClientUUID(String realm, String clientId, String token) {
//        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients?clientId=" + clientId;
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(token);
//        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
//                url, HttpMethod.GET, new HttpEntity<>(headers),
//                new ParameterizedTypeReference<>() {}
//        );
//        if (!response.getBody().isEmpty()) {
//            return (String) response.getBody().get(0).get("id");
//        }
//        throw new RuntimeException("Client not found");
//    }

    private String resolveUserId(String realm, String username, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/users?username=" + username;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            List<Map<String, Object>> users = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            if (users.isEmpty()) throw new RuntimeException("User not found: " + username);
            return (String) users.get(0).get("id");
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve user UUID for: " + username, e);
        }
    }

    private String getClientRoleId(String realm, String clientUUID, String roleName, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients/" + clientUUID + "/roles";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        try {
            List<Map<String, Object>> roles = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            return roles.stream()
                    .filter(r -> r.get("name").equals(roleName))
                    .map(r -> (String) r.get("id"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch client roles", e);
        }
    }

    // ---------------- REALM APIs (optional) ----------------
    @Override
    public List<Map<String, Object>> getAllRealms(String token) {
        throw new UnsupportedOperationException("Getting all realms is not supported by client service");
    }

    @Override
    public void createRealm(String realmName, String token) {
        String url = config.getBaseUrl() + "/admin/realms";
        Map<String, Object> body = Map.of("realm", realmName, "enabled", true);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    @Override
    public void signup(SignupRequest request) {
        String masterToken = getMasterToken();

        // ---------------- STEP 1: Handle product selection ----------------
        String realmName = request.getRealmName();
        String clientId = request.getClientId();

        if (clientId == null || clientId.isBlank()) {
            // 👇 If no client selected → assign dummy product
            realmName = "default-realm";
            clientId = "default-client";

            System.out.println("⚠️ No product selected → assigning dummy client: " + clientId);

            // Make sure dummy realm & client exist
            createRealm(realmName, masterToken);
            createClient(realmName, clientId, true, masterToken);
        } else {
            // 👇 Normal case → use user-selected product
            createRealm(realmName, masterToken);
            createClient(realmName, clientId, request.isPublicClient(), masterToken);
        }

        // ---------------- STEP 2: Handle client secret ----------------
        String clientUUID = getClientUUID(realmName, clientId, masterToken);
        String clientSecret = null;
        if (!request.isPublicClient()) {
            clientSecret = getClientSecret(realmName, clientUUID, masterToken);
            System.out.println("Generated client secret: " + clientSecret);
        }

        // ---------------- STEP 3: Create user ----------------
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", request.getAdminUser().getUsername());
        userMap.put("email", request.getAdminUser().getEmail());
        userMap.put("firstName", request.getAdminUser().getFirstName());
        userMap.put("lastName", request.getAdminUser().getLastName());
        userMap.put("enabled", true);
        userMap.put("emailVerified", true);

        Map<String, Object> credentialMap = new HashMap<>();
        credentialMap.put("type", "password");
        credentialMap.put("value", request.getAdminUser().getPassword());
        credentialMap.put("temporary", false);
        userMap.put("credentials", List.of(credentialMap));

        String userId = createUser(realmName, masterToken, userMap);

        // ---------------- STEP 4: Assign default roles ----------------
        List<String> builtInRoles = List.of(
                "create-client",
                "impersonation",
                "manage-authorization",
                "manage-clients",
                "manage-events",
                "manage-identity-providers",
                "manage-realm",
                "manage-users"
        );
        for (String roleName : builtInRoles) {
            assignRealmManagementRoleToUser(realmName, userId, roleName, masterToken);
        }

        if (clientSecret != null) {
            System.out.println("Use this secret for confidential client: " + clientSecret);
        }

        // ---------------- STEP 5: Notify centralized DB ----------------
        try {
            RestTemplate restTemplate = new RestTemplate();
            String centralizedUrl = "http://localhost:8087/onboard";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SignupRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(centralizedUrl, entity, String.class);

            System.out.println("✅ Onboarding info sent to centralized DB");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to notify centralized project: " + e.getMessage());
        }
    }


    public String getRealmManagementClientId(String realm, String token) {
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients?clientId=realm-management";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        if (!response.getBody().isEmpty()) {
            return (String) response.getBody().get(0).get("id");
        }
        throw new RuntimeException("realm-management client not found");
    }

    public String getRealmManagementRoleId(String realm, String roleName, String token) {
        String clientId = getRealmManagementClientId(realm, token);
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/clients/" + clientId + "/roles/" + roleName;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        try {
            Map<String, Object> role = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            return (String) role.get("id");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get role id for: " + roleName, e);
        }
    }

    public void assignRealmManagementRoleToUser(String realm, String userId, String roleName, String token) {
        String clientId = getRealmManagementClientId(realm, token);
        String roleId = getRealmManagementRoleId(realm, roleName, token);
        String url = config.getBaseUrl() + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/clients/" + clientId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<Map<String, Object>> roles = List.of(Map.of(
                "id", roleId,
                "name", roleName
        ));
        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(roles, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }
}