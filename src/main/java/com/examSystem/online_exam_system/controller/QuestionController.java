package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.model.Module;
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
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ExamService examService;

    // ---- QUESTION BANK LANDING ----
    // shows all modules — teacher picks one to add/view questions
    @GetMapping("/bank")
    public String questionBankLanding(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (loggedInUser.getRole().name().equals("STUDENT")) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        List<Module> modules = moduleService.getAllModules();

        // build question count per module
        java.util.Map<Long, Long> moduleCounts =
                new java.util.HashMap<>();
        java.util.Map<Long, Long> mcqCounts =
                new java.util.HashMap<>();
        java.util.Map<Long, Long> tfCounts =
                new java.util.HashMap<>();
        java.util.Map<Long, Long> saCounts =
                new java.util.HashMap<>();
        for (Module m : modules) {
            moduleCounts.put(m.getId(),
                    questionService.countQuestions(m.getId()));
            mcqCounts.put(m.getId(),
                    questionService.countByModuleAndType(
                            m.getId(), QuestionType.MCQ));
            tfCounts.put(m.getId(),
                    questionService.countByModuleAndType(
                            m.getId(), QuestionType.TRUE_FALSE));
            saCounts.put(m.getId(),
                    questionService.countByModuleAndType(
                            m.getId(), QuestionType.SHORT_ANSWER));
        }
        model.addAttribute("modules", modules);
        model.addAttribute("moduleCounts", moduleCounts);
        model.addAttribute("mcqCounts", mcqCounts);
        model.addAttribute("tfCounts", tfCounts);
        model.addAttribute("saCounts", saCounts);
        model.addAttribute("loggedInUser", loggedInUser);
        return "questions/bank";
    }

    // ---- VIEW ALL QUESTIONS FOR A MODULE ----
    @GetMapping("/module/{moduleId}")
    public String getQuestionsByModule(
            @PathVariable Long moduleId,
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
        List<Question> questions =
                questionService.getQuestionsByModule(moduleId);
        Module module = moduleService.getModuleById(moduleId);

        model.addAttribute("questions", questions);
        model.addAttribute("module", module);
        model.addAttribute("questionCount",
                questionService.countQuestions(moduleId));
        model.addAttribute("loggedInUser", loggedInUser);
        return "questions/all";
    }

    // ---- SHOW CREATE FORM ----
    @GetMapping("/create/{moduleId}")
    public String showCreateForm(@PathVariable Long moduleId,
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
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("module",
                moduleService.getModuleById(moduleId));
        model.addAttribute("questionTypes", QuestionType.values());
        return "questions/create";
    }

    // ---- HANDLE CREATE FORM ----
    @PostMapping("/create/{moduleId}")
    public String createQuestion(
            @PathVariable Long moduleId,
            @Valid @ModelAttribute Question question,
            BindingResult result,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("moduleId", moduleId);
            model.addAttribute("module",
                    moduleService.getModuleById(moduleId));
            model.addAttribute("questionTypes", QuestionType.values());
            return "questions/create";
        }
        questionService.addQuestion(question, moduleId);
        return "redirect:/questions/module/" + moduleId;
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
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("questionTypes", QuestionType.values());
        model.addAttribute("loggedInUser", loggedInUser);
        return "questions/edit";
    }

    // ---- HANDLE EDIT FORM ----
    @PostMapping("/edit/{id}")
    public String updateQuestion(
            @PathVariable Long id,
            @Valid @ModelAttribute Question question,
            BindingResult result,
            @RequestParam(required = false) Long moduleId,
            HttpSession session,
            Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("questionTypes", QuestionType.values());
            return "questions/edit";
        }
        Long originalModuleId =
                questionService.getQuestionById(id).getModule().getId();
        questionService.updateQuestion(id, question,
                moduleId != null ? moduleId : originalModuleId);
        return "redirect:/questions/module/" +
                (moduleId != null ? moduleId : originalModuleId);
    }

    // ---- DELETE QUESTION ----
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
        Long moduleId =
                questionService.getQuestionById(id).getModule().getId();
        questionService.deleteQuestion(id);
        return "redirect:/questions/module/" + moduleId;
    }
}