package com.examSystem.online_exam_system.repository;

import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// @Repository tells Spring Boot "this interface talks to the database"
// JpaRepository<User, Long> means:
//   - User = the entity/table we're working with
//   - Long = the data type of the primary key (our id field)
// By extending JpaRepository, we automatically get save(), findAll(),
// findById(), delete() and more — no code needed!
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Boot reads the method name and generates the SQL automatically!
    // "findBy" + "Email" → SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // finds all users that have a specific role
    // e.g. findByRole(Role.STUDENT) gets all students
    List<User> findByRole(Role role);

    // checks if any user already has this email
    // returns true or false — useful for registration validation
    boolean existsByEmail(String email);
}