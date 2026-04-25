package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.ExamAttempt;
import com.examSystem.online_exam_system.model.Result;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.ExamAttemptService;
import com.examSystem.online_exam_system.service.ExamService;
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
        List<Result> results = resultService.getResultsByStudent(loggedInUser);

        model.addAttribute("results", results);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("passCount", resultService.getPassCount(loggedInUser));
        model.addAttribute("failCount", resultService.getFailCount(loggedInUser));
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
        List<Result> results = resultService.getResultsByExam(
                examService.getExamById(examId)
        );
        model.addAttribute("results", results);
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "results/all";
    }
}