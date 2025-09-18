package com.paxaris.identity_service.service;

import com.paxaris.identity_service.dto.RoleCreationRequest;

public interface CrudKeycloakService {

    boolean createRole(String realm, String clientName, RoleCreationRequest request, String authHeader);

    boolean updateRole(String realm, String clientName, String roleName, RoleCreationRequest request, String authHeader);

    boolean deleteRole(String realm, String clientName, String roleName, String authHeader);

}
