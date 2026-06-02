package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.entities.UserStatus;
import com.example.demo.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
    public Long findUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElse(null); // Returns null if no user is found
    }
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email); // Returns null if no user is found
    }
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    public Long findProjectIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId())   // assumes User has a getProjectId() field
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }}
