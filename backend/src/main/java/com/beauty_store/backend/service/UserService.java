package com.beauty_store.backend.service;

import com.beauty_store.backend.dto.UserDTO;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(UserDTO userDTO) throws Exception {
        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new Exception("Passwords do not match");
        }

        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new Exception("Email already exists");
        }

        User user = new User();
        user.setFullName(userDTO.getFullName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword()); // Store plain password for now
        user.setRememberMe(userDTO.isRememberMe());

        return userRepository.save(user);
    }

    public User loginUser(UserDTO userDTO) throws Exception {
        User user = userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new Exception("User not found"));

        if (!user.getPassword().equals(userDTO.getPassword())) {
            throw new Exception("Invalid password");
        }

        return user;
    }
}