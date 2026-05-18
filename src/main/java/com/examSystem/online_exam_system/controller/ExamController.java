package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.service.*;
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

    @Autowired
    private ModuleService moduleService;

    // ---- VIEW ALL EXAMS ----
    @GetMapping
    public String getAllExams(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        List<Exam> exams;
        switch (loggedInUser.getRole()) {
            case ADMIN:
                exams = examService.getAllExams();
                break;
            case TEACHER:
                exams = examService.getExamsByCreator(loggedInUser);
                break;
            default:
                exams = examService.getPublishedExams();
        }
        model.addAttribute("exams", exams);
        model.addAttribute("loggedInUser", loggedInUser);
        return "exams/all";
    }

    // ---- VIEW SINGLE EXAM ----
    @GetMapping("/{id}")
    public String viewExam(
            @PathVariable Long id,
            @RequestParam(required = false) String submitted,
            @RequestParam(required = false) String approved,
            @RequestParam(required = false) String rejected,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        Exam exam = examService.getExamById(id);
        model.addAttribute("exam", exam);
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        if (submitted != null) {
            model.addAttribute("successMsg",
                    "✅ Exam submitted for approval! " +
                            "Admin will review it shortly.");
        }
        if (approved != null) {
            model.addAttribute("successMsg",
                    "✅ Exam approved and published!");
        }
        if (rejected != null) {
            model.addAttribute("successMsg",
                    "❌ Exam rejected. Teacher can revise and resubmit.");
        }
        return "exams/view";
    }

    // ---- SHOW CREATE FORM (teachers only) ----
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (!loggedInUser.getRole().name().equals("TEACHER")) {
            model.addAttribute("error",
                    "Only teachers can create exams.");
            return "users/accessdenied";
        }
        model.addAttribute("exam", new Exam());
        model.addAttribute("modules", moduleService.getAllModules());
        return "exams/create";
    }

    // ---- HANDLE CREATE FORM ----
    @PostMapping("/create")
    public String createExam(
            @Valid @ModelAttribute Exam exam,
            BindingResult result,
            @RequestParam(required = false) Long moduleId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            return "exams/create";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (moduleId != null) {
            exam.setModule(moduleService.getModuleById(moduleId));
        }
        examService.createExam(exam, loggedInUser);
        return "redirect:/exams";
    }

    // ---- SHOW EDIT FORM ----
    // locked if exam is PENDING or PUBLISHED
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        Exam exam = examService.getExamById(id);
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // only creator can edit
        if (!exam.getCreatedBy().getId().equals(loggedInUser.getId())) {
            model.addAttribute("error",
                    "Only the exam creator can edit this exam.");
            return "users/accessdenied";
        }

        // locked if pending or published
        if (exam.getStatus() == ExamStatus.PENDING ||
                exam.getStatus() == ExamStatus.PUBLISHED) {
            model.addAttribute("error",
                    "This exam cannot be edited while it is " +
                            exam.getStatus().name() + ". " +
                            (exam.getStatus() == ExamStatus.PENDING ?
                                    "Wait for admin review." :
                                    "Ask admin to unpublish first."));
            return "users/accessdenied";
        }

        model.addAttribute("exam", exam);
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("loggedInUser", loggedInUser);
        return "exams/edit";
    }

    // ---- HANDLE EDIT FORM ----
    @PostMapping("/edit/{id}")
    public String updateExam(
            @PathVariable Long id,
            @Valid @ModelAttribute Exam exam,
            BindingResult result,
            @RequestParam(required = false) Long moduleId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("loggedInUser",
                    SessionUtils.getLoggedInUser(session));
            return "exams/edit";
        }
        examService.updateExam(id, exam, moduleId);
        return "redirect:/exams/" + id;
    }

    // ---- SUBMIT FOR APPROVAL (teacher) ----
    @GetMapping("/submit/{id}")
    public String submitForApproval(@PathVariable Long id,
                                    HttpSession session,
                                    Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (!loggedInUser.getRole().name().equals("TEACHER")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        try {
            examService.submitForApproval(id);
            return "redirect:/exams/" + id + "?submitted=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    // ---- APPROVE EXAM (admin only) ----
    @GetMapping("/approve/{id}")
    public String approveExam(@PathVariable Long id,
                              HttpSession session,
                              Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        try {
            examService.approveExam(id);
            return "redirect:/exams/" + id + "?approved=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    // ---- REJECT EXAM (admin only) ----
    @GetMapping("/reject/{id}")
    public String rejectExam(@PathVariable Long id,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        try {
            examService.rejectExam(id);
            return "redirect:/exams/" + id + "?rejected=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    // ---- FORCE UNPUBLISH (admin only) ----
    @GetMapping("/forceunpublish/{id}")
    public String forceUnpublish(@PathVariable Long id,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        examService.forceUnpublish(id);
        return "redirect:/exams/" + id;
    }

    // ---- DELETE EXAM ----
    // admin can delete any exam
    // teacher can only delete their own DRAFT or REJECTED exams
    @GetMapping("/delete/{id}")
    public String deleteExam(@PathVariable Long id,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        Exam exam = examService.getExamById(id);

        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }

        // teacher can only delete their own draft/rejected exams
        if (loggedInUser.getRole().name().equals("TEACHER")) {
            if (!exam.getCreatedBy().getId()
                    .equals(loggedInUser.getId())) {
                model.addAttribute("error",
                        "You can only delete your own exams.");
                return "users/accessdenied";
            }
            if (exam.getStatus() == ExamStatus.PENDING ||
                    exam.getStatus() == ExamStatus.PUBLISHED) {
                model.addAttribute("error",
                        "You cannot delete an exam that is " +
                                exam.getStatus().name() + ".");
                return "users/accessdenied";
            }
        }

        examService.deleteExam(id);
        return "redirect:/exams";
    }
}