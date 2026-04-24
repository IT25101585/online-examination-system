package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Question;
import com.examSystem.online_exam_system.model.QuestionType;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.ExamService;
import com.examSystem.online_exam_system.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ExamService examService;

    // ---- VIEW ALL QUESTIONS FOR AN EXAM ----
    // url: /questions/exam/1 — shows all questions for exam with id 1
    @GetMapping("/exam/{examId}")
    public String getQuestionsByExam(@PathVariable Long examId,
                                     HttpSession session,
                                     Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        // students cannot view the question bank
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied! Only admins and teachers can view questions.");
            return "users/accessdenied";
        }
        List<Question> questions = questionService.getQuestionsByExam(examId);
        model.addAttribute("questions", questions);
        model.addAttribute("exam", examService.getExamById(examId));
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("questionCount", questionService.countQuestions(examId));
        return "questions/all";
    }

    // ---- VIEW SINGLE QUESTION ----
    @GetMapping("/{id}")
    public String viewQuestion(@PathVariable Long id,
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
        Question question = questionService.getQuestionById(id);
        model.addAttribute("question", question);
        model.addAttribute("loggedInUser", loggedInUser);
        return "questions/view";
    }

    // ---- SHOW CREATE FORM ----
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
        model.addAttribute("question", new Question());
        model.addAttribute("examId", examId);
        model.addAttribute("questionTypes", QuestionType.values());
        model.addAttribute("exam", examService.getExamById(examId));
        return "questions/create";
    }

    // ---- HANDLE CREATE FORM SUBMIT ----
    @PostMapping("/create/{examId}")
    public String createQuestion(@PathVariable Long examId,
                                 @Valid @ModelAttribute Question question,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("examId", examId);
            model.addAttribute("questionTypes", QuestionType.values());
            model.addAttribute("exam", examService.getExamById(examId));
            return "questions/create";
        }
        questionService.addQuestion(question, examId);
        return "redirect:/questions/exam/" + examId;
    }

    // ---- SHOW EDIT FORM ----
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
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
        Question question = questionService.getQuestionById(id);
        model.addAttribute("question", question);
        model.addAttribute("questionTypes", QuestionType.values());
        model.addAttribute("loggedInUser", loggedInUser);
        return "questions/edit";
    }

    // ---- HANDLE EDIT FORM SUBMIT ----
    @PostMapping("/edit/{id}")
    public String updateQuestion(@PathVariable Long id,
                                 @Valid @ModelAttribute Question question,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("questionTypes", QuestionType.values());
            return "questions/edit";
        }
        Long examId = questionService.getQuestionById(id).getExam().getId();
        questionService.updateQuestion(id, question);
        return "redirect:/questions/exam/" + examId;
    }

    // ---- DELETE QUESTION ----
    // only admins and teachers can delete questions
    @GetMapping("/delete/{id}")
    public String deleteQuestion(@PathVariable Long id,
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
        Long examId = questionService.getQuestionById(id).getExam().getId();
        questionService.deleteQuestion(id);
        return "redirect:/questions/exam/" + examId;
    }
}