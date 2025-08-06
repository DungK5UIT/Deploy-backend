package com.beauty_store.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data // Tự động tạo getters, setters, toString, equals, hashCode
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe;

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    public User(String fullName, String email, String password, boolean rememberMe) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe;
        this.isOnline = false;
    }
}