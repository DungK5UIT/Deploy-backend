package com.beauty_store.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email")
})
@Data
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

    @Column(name = "role", nullable = false)
    private String role = "USER";

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false; // Thêm cột is_online, mặc định là false

    @Column(name = "phone")  // Thêm phone
    private String phone;

    @Column(name = "address")  // Thêm address
    private String address;
    
    public User(String fullName, String email, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isOnline = false; // Khởi tạo is_online là false
    }

    // Thêm setter thủ công để đảm bảo tương thích nếu Lombok không hoạt động
    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }
}