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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/exams") // Base URL pathway for all exam-related web routes
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ModuleService moduleService;

    /**
     * Displays all exams based on the logged-in user's role.
     * ADMIN sees all, TEACHER sees their own creations, STUDENT sees only published ones.
     */
    @GetMapping
    public String getAllExams(HttpSession session, Model model) {
        // Security Check: Redirect to login if the user session is invalid
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        User loggedInUser = SessionUtils.getLoggedInUser(session);
        List<Exam> exams;

        // Role-based data filtering logic
        switch (loggedInUser.getRole()) {
            case ADMIN:
                exams = examService.getAllExams(); // Admin can monitor everything
                break;
            case TEACHER:
                exams = examService.getExamsByCreator(loggedInUser); // Teachers only see their own drafts/exams
                break;
            default:
                exams = examService.getPublishedExams(); // Students only see live, active exams
        }

        model.addAttribute("exams", exams);
        model.addAttribute("loggedInUser", loggedInUser);
        return "exams/all"; // Maps to Thymeleaf template: src/main/resources/templates/exams/all.html
    }

    /**
     * Views details of a specific exam and handles success notifications via request params.
     */
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
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));

        // --- Alert Message Interceptors for UI Feedback ---
        if (submitted != null) {
            model.addAttribute("successMsg", "✅ Exam submitted for approval! Admin will review it shortly.");
        }
        if (approved != null) {
            model.addAttribute("successMsg", "✅ Exam approved and published!");
        }
        if (rejected != null) {
            model.addAttribute("successMsg", "❌ Exam rejected. Teacher can revise and resubmit.");
        }

        return "exams/view"; // Maps to templates/exams/view.html
    }

    /**
     * Renders the blank "Create New Exam" form screen. Restricted strictly to TEACHER role.
     */
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        User loggedInUser = SessionUtils.getLoggedInUser(session);
        // Authorization Check: Blocks non-teachers from reaching the creation form
        if (!loggedInUser.getRole().name().equals("TEACHER")) {
            model.addAttribute("error", "Only teachers can create exams.");
            return "users/accessdenied";
        }

        model.addAttribute("exam", new Exam()); // Binds an empty model command object to the form
        model.addAttribute("modules", moduleService.getAllModules()); // Dropdown options list
        return "exams/create";
    }

    /**
     * Processes form data submission to register and save a brand new exam.
     */
    @PostMapping("/create")
    public String createExam(
            @Valid @ModelAttribute("exam") Exam exam, // @Valid triggers JSR-380 hibernate annotations validation
            BindingResult result, // Stores annotation validation failure outcomes
            @RequestParam(required = false) Long moduleId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        // If validation errors are present (e.g. empty title), return to the form with error tags
        if (result.hasErrors()) {
            model.addAttribute("exam", exam);
            model.addAttribute("modules", moduleService.getAllModules());
            return "exams/create";
        }

        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (moduleId != null) {
            exam.setModule(moduleService.getModuleById(moduleId)); // Tag to chosen course module
        }

        examService.createExam(exam, loggedInUser);
        return "redirect:/exams"; // URL redirect back to full listings grid
    }

    /**
     * Renders the edit form setup populated with existing data. Ensures authority control rules.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        Exam exam = examService.getExamById(id);
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // Security Validation 1: Only the specific creator can adjust their draft
        if (!exam.getCreatedBy().getId().equals(loggedInUser.getId())) {
            model.addAttribute("error", "Only the exam creator can edit this exam.");
            return "users/accessdenied";
        }

        // Security Validation 2: Cannot modify an exam if it is already locked in PENDING or PUBLISHED status
        if (exam.getStatus() == ExamStatus.PENDING || exam.getStatus() == ExamStatus.PUBLISHED) {
            model.addAttribute("error", "This exam cannot be edited while it is " + exam.getStatus().name() + ".");
            return "users/accessdenied";
        }

        model.addAttribute("exam", exam);
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("loggedInUser", loggedInUser);
        return "exams/edit";
    }

    /**
     * Processes incoming form revisions and pushes modifications to the database safely.
     */
    @PostMapping("/edit/{id}")
    public String updateExam(
            @PathVariable Long id,
            @Valid @ModelAttribute("exam") Exam exam,
            BindingResult result,
            @RequestParam(required = false) Long moduleId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
            return "exams/edit";
        }

        try {
            examService.updateExam(id, exam, moduleId);
            return "redirect:/exams/" + id;
        } catch (RuntimeException e) {
            // Catches operational errors and surfaces them back gracefully to the view layer
            model.addAttribute("error", e.getMessage());
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
            return "exams/edit";
        }
    }

    /**
     * Routes request from a teacher to push a finished draft forward into the admin approval workflow loop.
     */
    @GetMapping("/submit/{id}")
    public String submitForApproval(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (!loggedInUser.getRole().name().equals("TEACHER")) {
            return "users/accessdenied";
        }
        try {
            examService.submitForApproval(id);
            return "redirect:/exams/" + id + "?submitted=true"; // Appends URL param to signal view success banner
        } catch (RuntimeException e) {
            // Flash attribute stays alive across redirects for display alert purposes
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    /**
     * Administrative mapping that promotes an exam status from PENDING to live PUBLISHED status.
     */
    @GetMapping("/approve/{id}")
    public String approveExam(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        // Enforces strictly Admin clearances
        if (!SessionUtils.isAdmin(session)) {
            return "redirect:/users/accessdenied";
        }
        try {
            examService.approveExam(id);
            return "redirect:/exams/" + id + "?approved=true";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    /**
     * Administrative mapping that marks a pending exam request as REJECTED back to the creator.
     */
    @GetMapping("/reject/{id}")
    public String rejectExam(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!SessionUtils.isAdmin(session)) {
            return "redirect:/users/accessdenied";
        }
        try {
            examService.rejectExam(id);
            return "redirect:/exams/" + id + "?rejected=true";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/exams/" + id;
        }
    }

    /**
     * Administrative safety lock route to retract a currently active live exam from public student view.
     */
    @GetMapping("/forceunpublish/{id}")
    public String forceUnpublish(@PathVariable Long id, HttpSession session) {
        if (!SessionUtils.isAdmin(session)) {
            return "redirect:/users/accessdenied";
        }
        examService.forceUnpublish(id);
        return "redirect:/exams/" + id;
    }

    /**
     * Handles destructive structural requests to wipe an exam completely out of database records.
     * Implements multi-tier cascading safety validation controls based on role constraints.
     */
    @GetMapping("/delete/{id}")
    public String deleteExam(@PathVariable Long id, HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        Exam exam = examService.getExamById(id);

        // Rule 1: Students can never execute full deletion processes
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }

        // Rule 2: Sub-rules protecting teachers' internal actions
        if (loggedInUser.getRole().name().equals("TEACHER")) {
            // Guard clause: Cannot erase another lecturer's designated workspace setup
            if (!exam.getCreatedBy().getId().equals(loggedInUser.getId())) {
                model.addAttribute("error", "You can only delete your own exams.");
                return "users/accessdenied";
            }
            // Guard clause: Blocks deleting live/active exams to avoid breaking active student sessions mid-way
            if (exam.getStatus() == ExamStatus.PENDING || exam.getStatus() == ExamStatus.PUBLISHED) {
                model.addAttribute("error", "You cannot delete an exam that is " + exam.getStatus().name() + ".");
                return "users/accessdenied";
            }
        }

        examService.deleteExam(id); // Safe execute cascade deletion paths
        return "redirect:/exams";
    }
}