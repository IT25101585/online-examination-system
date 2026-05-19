package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Role;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // needed to cascade delete teacher's exams and
    // student's attempts/results before deleting user
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private SessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ResultRepository resultRepository;

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match!");
        }
        return userRepository.save(user);
    }

    public User loginUser(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() &&
                user.get().getPassword().equals(password)) {
            User loggedIn = user.get();
            loggedIn.setLastLogin(LocalDateTime.now());
            return userRepository.save(loggedIn);
        }
        throw new RuntimeException("Invalid email or password!");
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getRecentLogins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getLastLogin() != null)
                .sorted(Comparator.comparing(
                        User::getLastLogin).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    public long countByRole(Role role) {
        return userRepository.findByRole(role).size();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found!"));
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        if (updatedUser.getPassword() != null &&
                !updatedUser.getPassword().trim().isEmpty()) {
            existingUser.setPassword(updatedUser.getPassword());
        }
        return userRepository.save(existingUser);
    }

    // ---- DELETE USER ----
    // cascades differently based on role:
    // TEACHER → delete their exams (sessions, questions, results,
    //           attempts) then the teacher
    // STUDENT → delete their attempts and results then the student
    // ADMIN   → just delete (admins don't own exams or attempts)
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);

        if (user.getRole() == Role.TEACHER) {
            // get all exams this teacher created
            List<com.examSystem.online_exam_system.model.Exam>
                    exams = examRepository.findByCreatedBy(user);

            for (com.examSystem.online_exam_system.model.Exam
                    exam : exams) {
                // delete session questions then sessions
                List<com.examSystem.online_exam_system.model
                        .ExamSession> sessions =
                        examSessionRepository.findByExam(exam);
                for (com.examSystem.online_exam_system.model
                        .ExamSession session : sessions) {
                    sessionQuestionRepository.deleteAll(
                            sessionQuestionRepository
                                    .findBySession(session));
                }
                examSessionRepository.deleteAll(sessions);

                // delete results and attempts for this exam
                resultRepository.deleteAll(
                        resultRepository.findByExam(exam));
                examAttemptRepository.deleteAll(
                        examAttemptRepository.findByExam(exam));
            }

            // delete all exams
            examRepository.deleteAll(exams);

        } else if (user.getRole() == Role.STUDENT) {
            // delete student's results first then attempts
            resultRepository.deleteAll(
                    resultRepository.findByStudent(user));
            examAttemptRepository.deleteAll(
                    examAttemptRepository.findByStudent(user));
        }

        // finally delete the user
        userRepository.deleteById(id);
    }
}