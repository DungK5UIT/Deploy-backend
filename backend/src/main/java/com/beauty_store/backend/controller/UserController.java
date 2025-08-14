package com.beauty_store.backend.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.dto.LoginRequest;
import com.beauty_store.backend.dto.LogoutRequest;
import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.model.ErrorResponse;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.UserRepository;
import com.beauty_store.backend.response.AuthResponse;
import com.beauty_store.backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        try {
            logger.info("Registering user with email: {}", userDTO.getEmail());
            AuthResponse response = userService.registerUser(userDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Unexpected error during registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Logging in user with email: {}", loginRequest.getEmail());
            AuthResponse response = userService.loginUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Unexpected error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("#logoutRequest.id == authentication.principal") // Kiểm tra userId khớp với token
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        try {
            logger.info("Logging out user with ID: {}", logoutRequest.getId());
            userService.logoutUser(logoutRequest.getId());
            return ResponseEntity.ok(Collections.singletonMap("message", "Đăng xuất thành công"));
        } catch (IllegalArgumentException e) {
            logger.warn("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Không tìm thấy người dùng: " + e.getMessage(), HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            logger.error("Unexpected error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

@Autowired
private UserRepository userRepository;
@GetMapping("/users/{id}")
@PreAuthorize("#id == principal")  // Chỉ user sở hữu mới access
public ResponseEntity<?> getUserById(@PathVariable Long id) {
    try {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Trả về DTO để ẩn password
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("full_name", user.getFullName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("address", user.getAddress());
        userInfo.put("role", user.getRole());
        return ResponseEntity.ok(userInfo);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
    }
}
}