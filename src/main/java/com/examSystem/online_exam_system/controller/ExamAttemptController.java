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
import java.util.Map;

@Controller
@RequestMapping("/attempts")
public class ExamAttemptController {

    @Autowired
    private ExamAttemptService examAttemptService;  //Connects ExamAttemptService automatically

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamSessionService examSessionService;

    // ---- SHOW START PAGE ----
    @GetMapping("/start/{examId}")
    public String showStartPage(@PathVariable Long examId,
                                HttpSession httpSession,
                                Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) { //Check login
            return "redirect:/users/login"; //Redirects to login page
        }
        User loggedInUser =
                SessionUtils.getLoggedInUser(httpSession); //Gets current user from session
        if (!loggedInUser.getRole().name().equals("STUDENT")) { //Check role
            model.addAttribute("error",
                    "Only students can take exams!");
            return "users/accessdenied";
        }

        // auto close expired sessions
        examSessionService.autoCloseSessions();

        //Gets exam details from database
        Exam exam = examService.getExamById(examId);

        // find an active session for this exam
        List<ExamSession> sessions =
                examSessionService.getSessionsByExam(examId);

        //Finds first active session
        ExamSession activeSession = sessions.stream()
                .filter(ExamSession::isActive)
                .findFirst()
                .orElse(null);

        //If no active session
        if (activeSession == null) {
            model.addAttribute("error",
                    "No active session for this exam right now. " +
                            "Please check the schedule.");
            model.addAttribute("exam", exam);
            model.addAttribute("loggedInUser", loggedInUser);
            return "attempts/start";
        }

        // get total question count from active session
        List<SessionQuestion> sessionQuestions =
                examSessionService.getSessionQuestions(
                        activeSession.getId()); //Gets all questions

        //Send data to page,Adds data to HTML page
        model.addAttribute("exam", exam);
        model.addAttribute("activeSession", activeSession);
        model.addAttribute("questionCount",
                sessionQuestions.size());
        model.addAttribute("loggedInUser", loggedInUser);
        //Open page
        return "attempts/start";
    }

    // ---- START EXAM ----
    //Runs when student clicks Start Exam button
    @PostMapping("/start/{examId}")
    public String startExam(@PathVariable Long examId, //Check login same as before
                            HttpSession httpSession,
                            Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) {
            return "redirect:/users/login";
        }
        try {
            User loggedInUser =
                    SessionUtils.getLoggedInUser(httpSession);

            // find active session
            examSessionService.autoCloseSessions();
            List<ExamSession> sessions =
                    examSessionService.getSessionsByExam(examId);
            ExamSession activeSession = sessions.stream()
                    .filter(ExamSession::isActive)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "No active session found!"));

            ExamAttempt attempt = examAttemptService
                    .startExam(loggedInUser, examId,
                            activeSession.getId());
            return "redirect:/attempts/take/" + attempt.getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("exam",
                    examService.getExamById(examId));
            return "attempts/start";
        }
    }

    // ---- TAKE EXAM ----
    @GetMapping("/take/{attemptId}")
    public String takeExam(@PathVariable Long attemptId,
                           HttpSession httpSession,
                           Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) {
            return "redirect:/users/login";
        }
        ExamAttempt attempt =
                examAttemptService.getAttemptById(attemptId);

        // get session questions for THIS attempt's exam session
        List<SessionQuestion> questions = null;
        try {
            questions = examSessionService.getSessionQuestions(
                    attempt.getExamSessionId());
        } catch (Exception e) {
            // fallback empty
        }

        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("exam", attempt.getExam());
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(httpSession));
        return "attempts/take";
    }

    // ---- SUBMIT EXAM ----
    // after submit, go to results/{attemptId}
    // ResultController.viewResult handles the
    // pending approval check for students
    @PostMapping("/submit/{attemptId}")
    public String submitExam(
            @PathVariable Long attemptId,
            @RequestParam java.util.Map<String, String> answers,
            HttpSession httpSession,
            Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) {
            return "redirect:/users/login";
        }
        try {
            ExamAttempt attempt =
                    examAttemptService.submitExam(attemptId, answers);
            // go to results controller which checks approval status
            return "redirect:/results/" + attempt.getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "attempts/take";
        }
    }

    // ---- VIEW RESULT ----
    @GetMapping("/result/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             HttpSession httpSession,
                             Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) {
            return "redirect:/users/login";
        }
        ExamAttempt attempt =
                examAttemptService.getAttemptById(attemptId);
        model.addAttribute("attempt", attempt);
        model.addAttribute("exam", attempt.getExam());
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(httpSession));

        double percentage = ((double) attempt.getTotalScore() /
                attempt.getExam().getTotalMarks()) * 100;
        model.addAttribute("percentage", Math.round(percentage));
        return "attempts/submit";
    }

    // ---- VIEW ALL ATTEMPTS FOR AN EXAM ----
    @GetMapping("/exam/{examId}")
    public String viewAttemptsByExam(
            @PathVariable Long examId,
            HttpSession httpSession,
            Model model) {
        if (!SessionUtils.isLoggedIn(httpSession)) {
            return "redirect:/users/login";
        }
        User loggedInUser =
                SessionUtils.getLoggedInUser(httpSession);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        model.addAttribute("attempts",
                examAttemptService.getAttemptsByExam(examId));
        model.addAttribute("exam",
                examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "attempts/all";
    }
}
