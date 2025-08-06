package com.beauty_store.backend.controller;

import com.beauty_store.backend.dto.LoginRequest;
import com.beauty_store.backend.dto.LogoutRequest;
import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.response.AuthResponse;
import com.beauty_store.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.registerUser(userDTO);
        AuthResponse response = new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.isOnline());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userService.loginUser(loginRequest);
        AuthResponse response = new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.isOnline());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        userService.logoutUser(logoutRequest.getId());
        return ResponseEntity.ok("Đăng xuất thành công.");
    }
}