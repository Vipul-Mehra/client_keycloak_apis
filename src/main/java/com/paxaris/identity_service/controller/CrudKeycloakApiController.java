package com.paxaris.identity_service.controller;

import com.paxaris.identity_service.dto.RoleCreationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/keycloak")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class CrudKeycloakApiController {

    private final RestTemplate restTemplate = new RestTemplate();

    // ---------------- CREATE ROLE ----------------
    @PostMapping("/roles/{realm}/{client}")
    public ResponseEntity<String> createRole(
            @PathVariable String realm,
            @PathVariable String client,
            @RequestBody RoleCreationRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String gatewayUrl = "http://localhost:8087/project/roles/" + realm + "/" + client;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authHeader);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    gatewayUrl,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error creating role via API Gateway: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error: " + e.getMessage());
        }
    }

    // ---------------- UPDATE ROLE ----------------
    @PutMapping("/roles/{realm}/{client}/{roleName}")
    public ResponseEntity<String> updateRole(
            @PathVariable String realm,
            @PathVariable String client,
            @PathVariable String roleName,
            @RequestBody RoleCreationRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String gatewayUrl = "http://localhost:8087/project/roles/" + realm + "/" + client + "/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authHeader);

            ResponseEntity<String> response = restTemplate.exchange(
                    gatewayUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error updating role via API Gateway: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error: " + e.getMessage());
        }
    }

    // ---------------- DELETE ROLE ----------------
    @DeleteMapping("/roles/{realm}/{client}/{roleName}")
    public ResponseEntity<String> deleteRole(
            @PathVariable String realm,
            @PathVariable String client,
            @PathVariable String roleName,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String gatewayUrl = "http://localhost:8087/project/roles/" + realm + "/" + client + "/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);

            ResponseEntity<String> response = restTemplate.exchange(
                    gatewayUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error deleting role via API Gateway: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Error: " + e.getMessage());
        }
    }
}
