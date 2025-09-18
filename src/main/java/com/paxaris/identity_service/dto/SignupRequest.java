package com.paxaris.identity_service.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String realmName;
    private String clientId;
    private boolean publicClient = true;

    private AdminUser adminUser;

}