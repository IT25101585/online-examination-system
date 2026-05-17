package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Module;
import com.examSystem.online_exam_system.service.ModuleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/modules")
public class ModuleController {

    @Autowired
    private ModuleService moduleService;

    // ---- VIEW ALL MODULES ----
    @GetMapping
    public String getAllModules(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        List<Module> modules = moduleService.getAllModules();
        model.addAttribute("modules", modules);
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        return "modules/all";
    }

    // ---- SHOW CREATE FORM (admin only) ----
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        model.addAttribute("module", new Module());
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        return "modules/create";
    }

    // ---- HANDLE CREATE FORM ----
    @PostMapping("/create")
    public String createModule(@Valid @ModelAttribute Module module,
                               BindingResult result,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        if (result.hasErrors()) {
            return "modules/create";
        }
        try {
            moduleService.createModule(module);
            return "redirect:/modules?created=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "modules/create";
        }
    }

    // ---- SHOW EDIT FORM (admin only) ----
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        model.addAttribute("module", moduleService.getModuleById(id));
        model.addAttribute("loggedInUser",
                SessionUtils.getLoggedInUser(session));
        return "modules/edit";
    }

    // ---- HANDLE EDIT FORM ----
    @PostMapping("/edit/{id}")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute Module module,
                               BindingResult result,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        if (result.hasErrors()) {
            return "modules/edit";
        }
        try {
            moduleService.updateModule(id, module);
            return "redirect:/modules?updated=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "modules/edit";
        }
    }

    // ---- DELETE MODULE (admin only) ----
    @GetMapping("/delete/{id}")
    public String deleteModule(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        moduleService.deleteModule(id);
        return "redirect:/modules";
    }
}