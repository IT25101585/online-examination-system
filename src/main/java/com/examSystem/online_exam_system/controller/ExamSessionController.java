package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sessions")
public class ExamSessionController {

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ExamService examService;

    // ---- SESSIONS LANDING ----
    @GetMapping
    public String sessionsLanding(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }

        examSessionService.autoCloseSessions();

        // teachers see their own exams (any status)
        // admins see all exams
        List<Exam> exams;
        if (SessionUtils.isAdmin(session)) {
            exams = examService.getAllExams();
        } else {
            exams = examService.getExamsByCreator(loggedInUser);
        }

        java.util.Map<Long, Integer> examSessionCounts =
                new java.util.HashMap<>();
        for (Exam exam : exams) {
            examSessionCounts.put(exam.getId(),
                    examSessionService.getSessionsByExam(
                            exam.getId()).size());
        }

        model.addAttribute("exams", exams);
        model.addAttribute("examSessionCounts", examSessionCounts);
        model.addAttribute("loggedInUser", loggedInUser);
        return "sessions/selectexam";
    }

    // ---- VIEW ALL SESSIONS FOR AN EXAM ----
    @GetMapping("/exam/{examId}")
    public String getSessionsByExam(@PathVariable Long examId,
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

        List<ExamSession> sessions =
                examSessionService.getSessionsByExam(examId);
        model.addAttribute("sessions", sessions);
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "sessions/all";
    }

    // ---- SHOW CREATE SESSION FORM ----
    // teachers can create sessions on ANY of their exams (even draft)
    @GetMapping("/create/{examId}")
    public String showCreateForm(@PathVariable Long examId,
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
        Exam exam = examService.getExamById(examId);
        model.addAttribute("exam", exam);
        model.addAttribute("loggedInUser", loggedInUser);
        return "sessions/create";
    }

    // ---- HANDLE CREATE SESSION ----
    @PostMapping("/create/{examId}")
    public String createSession(
            @PathVariable Long examId,
            @RequestParam @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false) String label,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        try {
            ExamSession examSession =
                    examSessionService.createSession(examId, startTime, label);
            return "redirect:/sessions/preview/" + examSession.getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("exam", examService.getExamById(examId));
            model.addAttribute("loggedInUser",
                    SessionUtils.getLoggedInUser(session));
            return "sessions/create";
        }
    }

    // ---- PREVIEW SESSION QUESTIONS ----
    @GetMapping("/preview/{sessionId}")
    public String previewQuestions(@PathVariable Long sessionId,
                                   HttpSession session,
                                   Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        ExamSession examSession =
                examSessionService.getSessionById(sessionId);
        List<SessionQuestion> questions =
                examSessionService.getSessionQuestions(sessionId);
        model.addAttribute("examSession", examSession);
        model.addAttribute("questions", questions);
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        return "sessions/preview";
    }

    // ---- REGENERATE QUESTIONS ----
    @GetMapping("/regenerate/{sessionId}")
    public String regenerateQuestions(@PathVariable Long sessionId,
                                      HttpSession session,
                                      Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        try {
            examSessionService.regenerateQuestions(sessionId);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/sessions/preview/" + sessionId;
    }

    // ---- SHOW EDIT SESSION QUESTION FORM ----
    @GetMapping("/question/edit/{questionId}")
    public String showEditQuestion(@PathVariable Long questionId,
                                   HttpSession session,
                                   Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        model.addAttribute("question",
                examSessionService.getSessionQuestionById(questionId));
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        return "sessions/editquestion";
    }

    // ---- HANDLE EDIT SESSION QUESTION ----
    @PostMapping("/question/edit/{questionId}")
    public String updateSessionQuestion(
            @PathVariable Long questionId,
            @ModelAttribute SessionQuestion updated,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        SessionQuestion existing =
                examSessionService.getSessionQuestionById(questionId);
        examSessionService.updateSessionQuestion(questionId, updated);
        return "redirect:/sessions/preview/" +
                existing.getSession().getId();
    }

    // ---- VIEW SESSION ----
    @GetMapping("/{sessionId}")
    public String viewSession(@PathVariable Long sessionId,
                              HttpSession session,
                              Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        examSessionService.autoCloseSessions();
        ExamSession examSession =
                examSessionService.getSessionById(sessionId);
        model.addAttribute("examSession", examSession);
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        model.addAttribute("isClosed", examSession.isOver());
        return "sessions/view";
    }

    // ---- DELETE SESSION ----
    @GetMapping("/delete/{sessionId}")
    public String deleteSession(@PathVariable Long sessionId,
                                HttpSession session,
                                Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        ExamSession examSession =
                examSessionService.getSessionById(sessionId);
        Long examId = examSession.getExam().getId();
        examSessionService.deleteSession(sessionId);
        return "redirect:/sessions/exam/" + examId;
    }
}