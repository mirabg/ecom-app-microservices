package com.ecom.user.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressDTO address;
    /**
     * Initial login password. Only ever forwarded to Keycloak when creating
     * the account - never persisted in the local User document.
     */
    private String password;
}
