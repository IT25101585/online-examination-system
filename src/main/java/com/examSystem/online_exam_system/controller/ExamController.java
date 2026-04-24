package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.ExamService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    // ---- VIEW ALL EXAMS ----
    // admins see all exams
    // teachers see only their own exams
    // students see only published exams
    @GetMapping
    public String getAllExams(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        List<Exam> exams;

        switch (loggedInUser.getRole()) {
            case ADMIN:
                // admin sees everything
                exams = examService.getAllExams();
                break;
            case TEACHER:
                // teacher sees only their own exams
                exams = examService.getExamsByCreator(loggedInUser);
                break;
            default:
                // student sees only published exams
                exams = examService.getPublishedExams();
        }

        model.addAttribute("exams", exams);
        model.addAttribute("loggedInUser", loggedInUser);
        return "exams/all";
    }

    // ---- VIEW SINGLE EXAM ----
    @GetMapping("/{id}")
    public String viewExam(@PathVariable Long id,
                           HttpSession session,
                           Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        Exam exam = examService.getExamById(id);
        model.addAttribute("exam", exam);
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
        return "exams/view";
    }

    // ---- SHOW CREATE FORM ----
    // only admins and teachers can create exams
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied! Only admins and teachers can create exams.");
            return "users/accessdenied";
        }
        model.addAttribute("exam", new Exam());
        return "exams/create";
    }

    // ---- HANDLE CREATE FORM SUBMIT ----
    @PostMapping("/create")
    public String createExam(@Valid @ModelAttribute Exam exam,
                             BindingResult result,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            return "exams/create";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        examService.createExam(exam, loggedInUser);
        return "redirect:/exams";
    }

    // ---- SHOW EDIT FORM ----
    // only the creator or admin can edit
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        Exam exam = examService.getExamById(id);
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // only admin or the creator can edit
        if (!loggedInUser.getRole().name().equals("ADMIN") &&
                !exam.getCreatedBy().getId().equals(loggedInUser.getId())) {
            model.addAttribute("error", "Access denied! You can only edit your own exams.");
            return "users/accessdenied";
        }
        model.addAttribute("exam", exam);
        return "exams/edit";
    }

    // ---- HANDLE EDIT FORM SUBMIT ----
    @PostMapping("/edit/{id}")
    public String updateExam(@PathVariable Long id,
                             @Valid @ModelAttribute Exam exam,
                             BindingResult result,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            return "exams/edit";
        }
        examService.updateExam(id, exam);
        return "redirect:/exams";
    }

    // ---- PUBLISH EXAM ----
    @GetMapping("/publish/{id}")
    public String publishExam(@PathVariable Long id, HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        examService.publishExam(id);
        return "redirect:/exams";
    }

    // ---- UNPUBLISH EXAM ----
    @GetMapping("/unpublish/{id}")
    public String unpublishExam(@PathVariable Long id, HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        examService.unpublishExam(id);
        return "redirect:/exams";
    }

    // ---- DELETE EXAM ----
    // only admins can delete
    @GetMapping("/delete/{id}")
    public String deleteExam(@PathVariable Long id, HttpSession session, Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        examService.deleteExam(id);
        return "redirect:/exams";
    }
}