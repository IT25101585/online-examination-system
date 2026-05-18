package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Role;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ---- REGISTER ----
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match!");
        }
        return userRepository.save(user);
    }

    // ---- LOGIN ----
    // updates lastLogin timestamp every time user successfully logs in
    public User loginUser(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            User loggedIn = user.get();
            loggedIn.setLastLogin(LocalDateTime.now());
            return userRepository.save(loggedIn);
        }
        throw new RuntimeException("Invalid email or password!");
    }

    // ---- GET ALL USERS ----
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---- GET RECENT LOGINS ----
    // returns the last 5 users to have logged in
    // used for admin dashboard activity card
    public List<User> getRecentLogins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getLastLogin() != null)
                .sorted(Comparator.comparing(User::getLastLogin).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // ---- COUNT BY ROLE ----
    public long countByRole(Role role) {
        return userRepository.findByRole(role).size();
    }

    // ---- GET USER BY ID ----
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    // ---- GET USERS BY ROLE ----
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    // ---- UPDATE USER ----
    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        // only update password if a new one is provided
        if (updatedUser.getPassword() != null &&
                !updatedUser.getPassword().trim().isEmpty()) {
            existingUser.setPassword(updatedUser.getPassword());
        }
        return userRepository.save(existingUser);
    }

    // ---- DELETE USER ----
    public void deleteUser(Long id) {
        getUserById(id);
        userRepository.deleteById(id);
    }
}