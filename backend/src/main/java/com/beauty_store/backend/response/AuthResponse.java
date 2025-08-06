package com.beauty_store.backend.response;

import lombok.Data;
@Data
public class AuthResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role; // Thêm trường role
    private String token;

    public AuthResponse(Long id, String email, String fullName, String role, String token) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
    }
}
