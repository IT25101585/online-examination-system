package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Result;
import com.examSystem.online_exam_system.model.Role;
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

    // ---- STUDENT HISTORY ----
    // students only see approved results
    @GetMapping("/student")
    public String studentHistory(HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (!loggedInUser.getRole().name().equals("STUDENT")) {
            return "redirect:/history/admin";
        }

        // students only see teacher-approved results
        List<Result> results =
                resultService.getApprovedResultsByStudent(loggedInUser);

        return buildHistoryModel(
                model, loggedInUser, results, null);
    }

    // ---- TEACHER/ADMIN HISTORY OVERVIEW ----
    @GetMapping("/admin")
    public String adminHistory(HttpSession session,
                               Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            return "redirect:/history/student";
        }
        model.addAttribute("exams",
                examService.getAllExams());
        model.addAttribute("loggedInUser", loggedInUser);
        if (loggedInUser.getRole().name().equals("ADMIN")) {
            model.addAttribute("students",
                    userService.getUsersByRole(Role.STUDENT));
        }
        return "history/admin";
    }

    // ---- VIEW SPECIFIC STUDENT HISTORY (admin) ----
    @GetMapping("/student/{studentId}")
    public String viewStudentHistory(
            @PathVariable Long studentId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error",
                    "Access denied! Admins only.");
            return "users/accessdenied";
        }

        User student = userService.getUserById(studentId);
        // admin can see all results for this student
        // including unapproved ones
        List<Result> results =
                resultService.getResultsByStudent(student);

        return buildHistoryModel(
                model, SessionUtils.getLoggedInUser(session),
                results, student);
    }

    // ---- SHARED HELPER ----
    // builds model attributes for history pages
    // handles null lists and zero-division safely
    private String buildHistoryModel(Model model,
                                     User loggedInUser,
                                     List<Result> results,
                                     User student) {
        int totalExams = results == null ? 0 : results.size();

        long passCount = 0;
        long failCount = 0;
        double avgPercentage = 0;

        if (results != null && !results.isEmpty()) {
            passCount = results.stream()
                    .filter(r -> r.getPassed() != null &&
                            r.getPassed())
                    .count();
            failCount = results.stream()
                    .filter(r -> r.getPassed() != null &&
                            !r.getPassed())
                    .count();
            avgPercentage = results.stream()
                    .filter(r -> r.getPercentage() != null)
                    .mapToDouble(Result::getPercentage)
                    .average()
                    .orElse(0);
            avgPercentage =
                    Math.round(avgPercentage * 10.0) / 10.0;
        }

        model.addAttribute("results",
                results != null ? results : List.of());
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("totalExams", totalExams);
        model.addAttribute("avgPercentage", avgPercentage);

        // student attribute for admin viewing another student
        if (student != null) {
            model.addAttribute("student", student);
        }

        return "history/student";
    }
}