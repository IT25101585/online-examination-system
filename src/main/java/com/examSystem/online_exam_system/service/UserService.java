package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Role;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// @Service tells Spring Boot "this class contains business logic"
// Spring Boot will manage this class automatically
@Service
public class UserService {

    // @Autowired tells Spring Boot to automatically create and inject
    // the UserRepository here — we don't need to do "new UserRepository()"
    // Spring Boot handles that for us
    @Autowired
    private UserRepository userRepository;

    // ---- REGISTER ----
    // saves a new user to the database
    // first checks if the email is already taken
    public User registerUser(User user) {
        // check if email is already taken
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }
        // check if password and confirm password match
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match!");
        }
        return userRepository.save(user);
    }

    // ---- LOGIN ----
    // checks if email and password match a user in the database
    // returns the user if found, throws error if not
    public User loginUser(String email, String password) {
        // Optional means "this might or might not exist"
        Optional<User> user = userRepository.findByEmail(email);

        // if user exists AND password matches, return the user
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        throw new RuntimeException("Invalid email or password!");
    }

    // ---- GET ALL USERS ----
    // returns every user in the database
    // only the admin should be able to call this
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---- GET USER BY ID ----
    // finds one specific user by their id
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    // ---- GET USERS BY ROLE ----
    // returns all users with a specific role
    // e.g. getAllStudents() would call this with Role.STUDENT
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    // ---- UPDATE USER ----
    // finds existing user, updates their details, saves back to database
    public User updateUser(Long id, User updatedUser) {
        // first find the existing user
        User existingUser = getUserById(id);

        // update only the fields that are allowed to change
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());

        // save and return the updated user
        return userRepository.save(existingUser);
    }

    // ---- DELETE USER ----
    // removes a user from the database permanently
    public void deleteUser(Long id) {
        // check user exists first before trying to delete
        getUserById(id);
        userRepository.deleteById(id);
    }
}