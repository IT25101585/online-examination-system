package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Result;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.ExamService;
import com.examSystem.online_exam_system.service.ResultService;
import com.examSystem.online_exam_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/history")
public class ExamHistoryController {

    @Autowired
    private ResultService resultService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExamService examService;

    // ---- STUDENT EXAM HISTORY ----
    // shows a student their own exam history with stats
    @GetMapping("/student")
    public String studentHistory(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // only students can access this page
        if (!loggedInUser.getRole().name().equals("STUDENT")) {
            return "redirect:/history/admin";
        }

        List<Result> results = resultService.getResultsByStudent(loggedInUser);
        long passCount = resultService.getPassCount(loggedInUser);
        long failCount = resultService.getFailCount(loggedInUser);
        int totalExams = results.size();

        // calculate average percentage if student has taken exams
        double avgPercentage = 0;
        if (totalExams > 0) {
            avgPercentage = results.stream()
                    .mapToDouble(Result::getPercentage)
                    .average()
                    .orElse(0);
            avgPercentage = Math.round(avgPercentage * 10.0) / 10.0;
        }

        model.addAttribute("results", results);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("totalExams", totalExams);
        model.addAttribute("avgPercentage", avgPercentage);
        return "history/student";
    }

    // ---- ADMIN/TEACHER HISTORY VIEW ----
    // shows all students' results across all exams
    @GetMapping("/admin")
    public String adminHistory(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // students cannot access this page
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            return "redirect:/history/student";
        }

        // get all exams and all results
        model.addAttribute("exams", examService.getAllExams());
        model.addAttribute("loggedInUser", loggedInUser);

        // if admin, also show all students
        if (loggedInUser.getRole().name().equals("ADMIN")) {
            List<User> students = userService.getUsersByRole(
                    com.examSystem.online_exam_system.model.Role.STUDENT
            );
            model.addAttribute("students", students);
        }

        return "history/admin";
    }

    // ---- VIEW SPECIFIC STUDENT'S HISTORY (admin only) ----
    @GetMapping("/student/{studentId}")
    public String viewStudentHistory(@PathVariable Long studentId,
                                     HttpSession session,
                                     Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        User student = userService.getUserById(studentId);
        List<Result> results = resultService.getResultsByStudent(student);
        long passCount = resultService.getPassCount(student);
        long failCount = resultService.getFailCount(student);

        model.addAttribute("results", results);
        model.addAttribute("student", student);
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
        return "history/student";
    }
}