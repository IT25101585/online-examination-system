package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.service.ExamAttemptService;
import com.examSystem.online_exam_system.service.ExamService;
import com.examSystem.online_exam_system.service.ExamSessionService;
import com.examSystem.online_exam_system.service.ResultService;
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

    // ---- VIEW SINGLE RESULT ----
    // automatically saves the result if not already saved
    @GetMapping("/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        ExamAttempt attempt = examAttemptService.getAttemptById(attemptId);

        // save result if not already saved
        Result result = resultService.saveResult(attempt);

        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // students can only see approved results
        if (loggedInUser.getRole().name().equals("STUDENT") &&
                !result.getTeacherApproved()) {
            model.addAttribute("loggedInUser", loggedInUser);
            return "results/pendingapproval";
        }

        model.addAttribute("result", result);
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
        return "results/view";
    }

    // ---- VIEW EXAM HISTORY (student) ----
    // shows all results for the logged in student
    @GetMapping("/history")
    public String viewHistory(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // students only see approved results
        List<Result> results = loggedInUser.getRole().name()
                .equals("STUDENT")
                ? resultService.getApprovedResultsByStudent(loggedInUser)
                : resultService.getResultsByStudent(loggedInUser);

        model.addAttribute("results", results);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("passCount", resultService.getPassCount(loggedInUser));
        model.addAttribute("failCount", resultService.getFailCount(loggedInUser));
        return "results/history";
    }

    // ---- VIEW ALL RESULTS FOR AN EXAM (teacher/admin) ----
    // only shows results if at least one session is closed
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

        // auto close expired sessions first
        // (injected via ExamSessionService)
        List<Result> results = resultService.getResultsByExam(
                examService.getExamById(examId)
        );
        model.addAttribute("results", results);
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/all";
    }


    // ---- GRADING REVIEW PAGE ----
    // teacher reviews a specific student's attempt before approving
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

        // get the session questions so teacher can compare
        // answers against the actual questions shown to student
        List<SessionQuestion> sessionQuestions = null;
        try {
            // try to get session questions if they exist
            sessionQuestions = examSessionService
                    .getSessionQuestions(attempt.getId());
        } catch (Exception e) {
            // fallback — session questions not found
        }

        model.addAttribute("result", result);
        model.addAttribute("attempt", attempt);
        model.addAttribute("sessionQuestions", sessionQuestions);
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/grade";
    }

    // ---- HANDLE APPROVE RESULT ----
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
        resultService.approveResult(resultId, teacherNote, overriddenScore);
        Result result = resultService.getResultById(resultId);
        return "redirect:/results/exam/" + result.getExam().getId()
                + "?approved=true";
    }

    // ---- VIEW PENDING GRADING (teacher) ----
    @GetMapping("/pending")
    public String viewPendingGrading(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        List<Result> pending = resultService.getPendingReviewResults();
        model.addAttribute("pendingResults", pending);
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/pending";
    }
}

