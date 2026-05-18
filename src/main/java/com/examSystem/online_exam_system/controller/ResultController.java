package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/results")
public class ResultController {

    @Autowired
    private ResultService resultService;

    @Autowired
    private ExamAttemptService examAttemptService;

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamSessionService examSessionService;

    // ---- VIEW RESULT ----
    // students can only see this after teacher approves
    // this endpoint is called after student submits attempt
    @GetMapping("/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        ExamAttempt attempt =
                examAttemptService.getAttemptById(attemptId);

        // auto-save result when student submits
        // teacherApproved defaults to false inside saveResult
        Result result = resultService.saveResult(attempt);

        // students cannot see result until teacher approves
        if (loggedInUser.getRole().name().equals("STUDENT") &&
                !result.getTeacherApproved()) {
            model.addAttribute("loggedInUser", loggedInUser);
            return "results/pendingapproval";
        }

        model.addAttribute("result", result);
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/view";
    }

    // ---- VIEW EXAM HISTORY ----
    // students only see approved results
    // teachers/admins see all
    @GetMapping("/history")
    public String viewHistory(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        List<Result> results;
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            // students only see teacher-approved results
            results = resultService
                    .getApprovedResultsByStudent(loggedInUser);
        } else {
            // teachers and admins see all results
            results = resultService
                    .getResultsByStudent(loggedInUser);
        }

        long passCount = resultService.getPassCount(loggedInUser);
        long failCount = resultService.getFailCount(loggedInUser);

        model.addAttribute("results", results);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);
        return "results/history";
    }

    // ---- VIEW ALL RESULTS FOR AN EXAM (teacher/admin) ----
    @GetMapping("/exam/{examId}")
    public String viewResultsByExam(@PathVariable Long examId,
                                    HttpSession session,
                                    Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        examSessionService.autoCloseSessions();
        Exam exam = examService.getExamById(examId);
        model.addAttribute("results",
                resultService.getResultsByExam(exam));
        model.addAttribute("exam", exam);
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/all";
    }

    // ---- GRADING REVIEW PAGE ----
    // teacher reviews each question and student answer
    // side by side before approving
    @GetMapping("/grade/{resultId}")
    public String gradeResult(@PathVariable Long resultId,
                              HttpSession session,
                              Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }

        Result result = resultService.getResultById(resultId);
        ExamAttempt attempt = result.getAttempt();

        // load session questions so teacher can compare
        // each question against the student's answer
        List<SessionQuestion> sessionQuestions = null;
        try {
            if (attempt.getExamSessionId() != null) {
                sessionQuestions =
                        examSessionService.getSessionQuestions(
                                attempt.getExamSessionId());
            }
        } catch (Exception e) {
            // session questions not found — still show the page
        }

        model.addAttribute("result", result);
        model.addAttribute("attempt", attempt);
        model.addAttribute("sessionQuestions", sessionQuestions);
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/grade";
    }

    // ---- HANDLE APPROVE RESULT ----
    // teacher approves — student can now see result
    // teacher can override score for any question
    // (especially useful for short answer questions)
    @PostMapping("/approve/{resultId}")
    public String approveResult(
            @PathVariable Long resultId,
            @RequestParam(required = false) String teacherNote,
            @RequestParam(required = false) Integer overriddenScore,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        resultService.approveResult(
                resultId, teacherNote, overriddenScore);
        Result result = resultService.getResultById(resultId);
        return "redirect:/results/exam/" +
                result.getExam().getId() + "?approved=true";
    }

    // ---- VIEW PENDING GRADING ----
    @GetMapping("/pending")
    public String viewPendingGrading(HttpSession session,
                                     Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        model.addAttribute("pendingResults",
                resultService.getPendingReviewResults());
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/pending";
    }
}