package com.example.client_keycloak_apis.service;

import com.example.client_keycloak_apis.dto.SignupRequest;

import java.util.List;
import java.util.Map;

public interface KeycloakClientService {

    Map<String, Object> getMyRealmToken(String username, String password, String clientId, String clientSecret, String realm);

    void createRealm(String realmName, String token);

    void signup(SignupRequest request);

    List<Map<String, Object>> getAllRealms(String token);

    String createClient(String realm, String clientId, boolean isPublicClient, String token);

    List<Map<String, Object>> getAllClients(String realm, String token);

    String createUser(String realm, String token, Map<String, Object> userPayload);

    List<Map<String, Object>> getAllUsers(String realm, String token);

    void createClientRole(String realm, String clientId, String clientSecret, String roleName, String token);

    void createRealmRole(String realm, String roleName, String clientId, String token);

    List<Map<String, Object>> getAllRoles(String realm, String clientId, String token);

    void assignClientRole(String realm, String userIdentifier, String clientIdentifier, String roleId, String roleName, String token);

    void assignClientRoleToUser(String realm, String userId, String clientUUID, String roleId, String roleName, String token);

    String getClientSecret(String realm, String clientId, String token);
}