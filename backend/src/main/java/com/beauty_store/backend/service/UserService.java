package com.beauty_store.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beauty_store.backend.dto.LoginRequest;
import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.exception.ResourceNotFoundException;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        String hashedPassword = passwordEncoder.encode(userDTO.getPassword());
        User newUser = new User(
                userDTO.getFullName(),
                userDTO.getEmail(),
                hashedPassword,
                userDTO.isRememberMe()
        );

        return userRepository.save(newUser);
    }

    @Transactional
    public User loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }

        if (user.isOnline()) {
            throw new IllegalArgumentException("Tài khoản hiện đã có người đăng nhập.");
        }

        user.setOnline(true);
        return userRepository.save(user);
    }

    @Transactional
    public void logoutUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        
        user.setOnline(false);
        userRepository.save(user);
    }
}
