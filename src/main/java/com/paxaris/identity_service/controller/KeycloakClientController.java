package com.paxaris.identity_service.controller;

import com.paxaris.identity_service.dto.RoleCreationRequest;
import com.paxaris.identity_service.dto.SignupRequest;
import com.paxaris.identity_service.service.KeycloakClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/keycloak")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class KeycloakClientController {

    private final KeycloakClientService keycloakClientService;
    private final RestTemplate restTemplate = new RestTemplate(); // Add RestTemplate

    // ---------------- GET TOKEN ----------------
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> getMyRealmToken(
            @RequestParam(required = false) String realm,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret) {

        Map<String, Object> token = keycloakClientService.getMyRealmToken(
                username, password, clientId, clientSecret, realm);

        return ResponseEntity.ok(token);
    }

    // ---------------- CREATE CLIENT ----------------
    @PostMapping("/{realm}/clients")
    public ResponseEntity<String> createClient(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String clientId,
            @RequestParam(defaultValue = "true") boolean publicClient) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        keycloakClientService.createClient(realm, clientId, publicClient, token);
        return ResponseEntity.ok("Client created successfully");
    }

    // ---------------- GET ALL CLIENTS ----------------
    @GetMapping("/{realm}/clients")
    public ResponseEntity<?> getAllClients(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        return ResponseEntity.ok(keycloakClientService.getAllClients(realm, token));
    }

    // ---------------- CREATE USER ----------------
    @PostMapping("/{realm}/users")
    public ResponseEntity<String> createUser(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, Object> userPayload) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        String userId = keycloakClientService.createUser(realm, token, userPayload);
        return ResponseEntity.ok(userId);
    }

    // ---------------- GET ALL USERS ----------------
    @GetMapping("/{realm}/users")
    public ResponseEntity<?> getAllUsers(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        return ResponseEntity.ok(keycloakClientService.getAllUsers(realm, token));
    }

    // ---------------- CREATE CLIENT ROLES ----------------
    @PostMapping("/{realm}/clients/{clientName}/roles")
    public ResponseEntity<String> createClientRoles(
            @PathVariable String realm,
            @PathVariable String clientName,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody List<RoleCreationRequest> roleRequests) {

        // Forward roles to API Gateway
        String apiGatewayUrl = "http://localhost:8087/project/register/" + realm + "/" + clientName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<RoleCreationRequest>> entity = new HttpEntity<>(roleRequests, headers);
        restTemplate.postForEntity(apiGatewayUrl, entity, String.class);

        return ResponseEntity.ok("Roles sent to API Gateway successfully");
    }

    // ---------------- CREATE REALM ROLE ----------------
    @PostMapping("/{realm}/roles")
    public ResponseEntity<String> createRealmRole(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String roleName,
            @RequestParam String clientId) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        keycloakClientService.createRealmRole(realm, roleName, clientId, token);
        return ResponseEntity.ok("Realm role created successfully");
    }

    // ---------------- GET ALL ROLES ----------------
    @GetMapping("/{realm}/roles")
    public ResponseEntity<?> getAllRoles(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String clientId) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        return ResponseEntity.ok(keycloakClientService.getAllRoles(realm, clientId, token));
    }

    // ---------------- ASSIGN CLIENT ROLE TO USER ----------------
    @PostMapping("/{realm}/users/{userIdentifier}/clients/{clientIdentifier}/roles")
    public ResponseEntity<String> assignClientRoleToUser(
            @PathVariable String realm,
            @PathVariable String userIdentifier,
            @PathVariable String clientIdentifier,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String roleName) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        keycloakClientService.assignClientRole(realm, userIdentifier, clientIdentifier, null, roleName, token);
        return ResponseEntity.ok("Role assigned successfully");
    }

    // ---------------- GET CLIENT SECRET ----------------
    @GetMapping("/{realm}/clients/{clientId}/secret")
    public ResponseEntity<String> getClientSecret(
            @PathVariable String realm,
            @PathVariable String clientId,
            @RequestParam String token) {

        return ResponseEntity.ok(keycloakClientService.getClientSecret(realm, clientId, token));
    }

    // ---------------- SIGNUP ----------------
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
        keycloakClientService.signup(signupRequest);
        return ResponseEntity.ok("Signup completed successfully!");
    }
}
