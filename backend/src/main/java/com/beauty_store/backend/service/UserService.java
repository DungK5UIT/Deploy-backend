package com.beauty_store.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beauty_store.backend.dto.LoginRequest;
import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.UserRepository;
import com.beauty_store.backend.response.AuthResponse;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Transactional
    public AuthResponse registerUser(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        if (!isStrongPassword(userDTO.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
        }

        String hashedPassword = passwordEncoder.encode(userDTO.getPassword());
        User newUser = new User();
        newUser.setFullName(userDTO.getFullName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setPassword(hashedPassword);
        newUser.setRole("USER");
        newUser.setIsOnline(false); // Đảm bảo is_online = false khi đăng ký

        User savedUser = userRepository.save(newUser);
        String token = jwtService.generateToken(savedUser);
        logger.info("User registered successfully: {}", savedUser.getEmail());
        return new AuthResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole(), token);
    }

    @Transactional
    public AuthResponse loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));

        // Kiểm tra trạng thái is_online
        if (user.isOnline()) {
            throw new IllegalArgumentException("Tài khoản đang được đăng nhập ở nơi khác.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }

        // Cập nhật is_online = true
        user.setIsOnline(true);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        logger.info("User logged in successfully: {}", user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), token);
    }

    @Transactional
    public void logoutUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
        
        // Cập nhật is_online = false
        user.setIsOnline(false);
        userRepository.save(user);
        
        logger.info("User logged out: {}", user.getEmail());
        // Nếu cần blacklist token, thêm logic ở đây
    }

    private boolean isStrongPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$");
    }
}