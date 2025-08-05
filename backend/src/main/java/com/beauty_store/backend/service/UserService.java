package com.beauty_store.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(UserDTO userDTO) throws Exception {
        // Kiểm tra email đã tồn tại
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new Exception("Email đã tồn tại");
        }

        User user = new User();
        user.setFullName(userDTO.getFullName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword()); // Lưu plain text theo yêu cầu
        user.setRememberMe(userDTO.isRememberMe());
        user.setOnline(false); // User mới mặc định offline

        return userRepository.save(user);
    }

    public User loginUser(UserDTO userDTO) throws Exception {
        User user = userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new Exception("Không tìm thấy người dùng"));

        if (!user.getPassword().equals(userDTO.getPassword())) {
            throw new Exception("Mật khẩu không đúng");
        }

        if (user.isOnline()) {
            throw new Exception("Tài khoản hiện đã có người đăng nhập");
        }

        user.setOnline(true); // Đặt trạng thái online
        return userRepository.save(user); // Lưu trạng thái
    }

    public void logoutUser(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Không tìm thấy người dùng"));
        user.setOnline(false); // Đặt trạng thái offline
        userRepository.save(user);
    }
}