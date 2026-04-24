package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.ExamAttempt;
import com.examSystem.online_exam_system.model.Question;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.ExamAttemptService;
import com.examSystem.online_exam_system.service.ExamService;
import com.examSystem.online_exam_system.service.QuestionService;
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
    private ExamAttemptService examAttemptService;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    // ---- SHOW START PAGE ----
    // student sees exam details before starting
    @GetMapping("/start/{examId}")
    public String showStartPage(@PathVariable Long examId,
                                HttpSession session,
                                Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        // only students can take exams
        if (!loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Only students can take exams!");
            return "users/accessdenied";
        }
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("questionCount", questionService.countQuestions(examId));
        return "attempts/start";
    }

    // ---- START EXAM ----
    // creates the attempt and takes student to the exam
    @PostMapping("/start/{examId}")
    public String startExam(@PathVariable Long examId,
                            HttpSession session,
                            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        try {
            User loggedInUser = SessionUtils.getLoggedInUser(session);
            ExamAttempt attempt = examAttemptService.startExam(loggedInUser, examId);
            return "redirect:/attempts/take/" + attempt.getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("exam", examService.getExamById(examId));
            return "attempts/start";
        }
    }

    // ---- TAKE EXAM ----
    // shows the exam questions for the student to answer
    @GetMapping("/take/{attemptId}")
    public String takeExam(@PathVariable Long attemptId,
                           HttpSession session,
                           Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        ExamAttempt attempt = examAttemptService.getAttemptById(attemptId);
        List<Question> questions = questionService.getQuestionsByExam(
                attempt.getExam().getId()
        );
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("exam", attempt.getExam());
        return "attempts/take";
    }

    // ---- SUBMIT EXAM ----
    // receives all answers and calculates score
    @PostMapping("/submit/{attemptId}")
    public String submitExam(@PathVariable Long attemptId,
                             @RequestParam Map<String, String> answers,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        try {
            ExamAttempt attempt = examAttemptService.submitExam(attemptId, answers);
            return "redirect:/attempts/result/" + attempt.getId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "attempts/take";
        }
    }

    // ---- VIEW RESULT ----
    // shows the student their score after submission
    @GetMapping("/result/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        ExamAttempt attempt = examAttemptService.getAttemptById(attemptId);
        model.addAttribute("attempt", attempt);
        model.addAttribute("exam", attempt.getExam());
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));

        // calculate percentage
        double percentage = ((double) attempt.getTotalScore() /
                attempt.getExam().getTotalMarks()) * 100;
        model.addAttribute("percentage", Math.round(percentage));
        return "attempts/submit";
    }

    // ---- VIEW ALL ATTEMPTS FOR AN EXAM (teacher/admin) ----
    @GetMapping("/exam/{examId}")
    public String viewAttemptsByExam(@PathVariable Long examId,
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
        model.addAttribute("attempts", examAttemptService.getAttemptsByExam(examId));
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "attempts/all";
    }
}