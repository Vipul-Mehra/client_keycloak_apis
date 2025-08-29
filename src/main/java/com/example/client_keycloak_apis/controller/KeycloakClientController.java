package com.example.client_keycloak_apis.controller;

import com.example.client_keycloak_apis.dto.SignupRequest;
import com.example.client_keycloak_apis.service.KeycloakClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/keycloak")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class KeycloakClientController {

    private final KeycloakClientService keycloakClientService;

    // Get token from a given realm (no hardcoded realm or client values)
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
    // Create a client in a realm
    @PostMapping("/{realm}/clients")
    public ResponseEntity<String> createClient(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String clientId,
            @RequestParam(defaultValue = "true") boolean publicClient) {

        // Extract the token by removing "Bearer " prefix
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        keycloakClientService.createClient(realm, clientId, publicClient, token);
        return ResponseEntity.ok("Client created successfully");
    }

    // Get all clients in a realm
    @GetMapping("/{realm}/clients")
    public ResponseEntity<?> getAllClients(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;
        return ResponseEntity.ok(keycloakClientService.getAllClients(realm, token));
    }

    // Create a user in a realm
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

    // Get all users in a realm
    @GetMapping("/{realm}/users")
    public ResponseEntity<?> getAllUsers(
            @PathVariable String realm,
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;
        return ResponseEntity.ok(keycloakClientService.getAllUsers(realm, token));
    }

    // Create a realm role
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

    // Get all roles in a realm
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

    // Assign a client role to a user
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

        // Pass null for roleId; service will resolve it
        keycloakClientService.assignClientRole(realm, userIdentifier, clientIdentifier, null, roleName, token);

        return ResponseEntity.ok("Role assigned successfully");
    }




    // Get a client secret
    @GetMapping("/{realm}/clients/{clientId}/secret")
    public ResponseEntity<String> getClientSecret(
            @PathVariable String realm,
            @PathVariable String clientId,
            @RequestParam String token) {
        return ResponseEntity.ok(keycloakClientService.getClientSecret(realm, clientId, token));
    }


    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
        // Backend handles getting master token itself
        keycloakClientService.signup(signupRequest);
        return ResponseEntity.ok("Signup completed successfully!");
    }

}