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
@RequestMapping("/modules") // Base URL pathway for all module management routes
public class ModuleController {

    @Autowired
    private ModuleService moduleService;

    /**
     * Displays a list of all course modules registered in the system.
     * Accessible by any authenticated logged-in user (Admin, Teacher, or Student).
     */
    @GetMapping
    public String getAllModules(HttpSession session, Model model) {
        // Security Check: Ensure user is authenticated before serving data
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        List<Module> modules = moduleService.getAllModules();
        model.addAttribute("modules", modules);
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));

        return "modules/all"; // Renders Thymeleaf view: templates/modules/all.html
    }

    /**
     * Renders a blank form to create a new module.
     * Restricted strictly to ADMIN users only.
     */
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        // Authorization Check: Only explicit Admins are allowed past this block
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }

        model.addAttribute("module", new Module()); // Binds empty command object for form binding
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));

        return "modules/create"; // Renders templates/modules/create.html
    }

    /**
     * Processes form submissions to save and instantiate a new module entry.
     */
    @PostMapping("/create")
    public String createModule(@Valid @ModelAttribute Module module, // Triggers JSR-380 validation constraints
                               BindingResult result, // Catches input validation field errors
                               HttpSession session,
                               Model model) {
        // Authorization Check: Secure the form submission endpoint from unauthorized API hits
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }

        // If form validation fails (e.g., blank name field), reload page with explicit error styles
        if (result.hasErrors()) {
            return "modules/create";
        }

        try {
            moduleService.createModule(module);
            return "redirect:/modules?created=true"; // Redirects with a URL query flag for success alerts
        } catch (RuntimeException e) {
            // Handles database business errors (e.g., trying to add a duplicate module name)
            model.addAttribute("error", e.getMessage());
            return "modules/create";
        }
    }

    /**
     * Displays the module modification form populated with specific old data.
     * Restricted strictly to ADMIN users only.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        // Authorization Check: Restrict access to administrators
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }

        model.addAttribute("module", moduleService.getModuleById(id)); // Populate form fields with current values
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));

        return "modules/edit"; // Renders templates/modules/edit.html
    }

    /**
     * Processes incoming form changes to modify a module's information details.
     */
    @PostMapping("/edit/{id}")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute Module module,
                               BindingResult result,
                               HttpSession session,
                               Model model) {
        // Authorization Check: Double security verification during execution lifecycle
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }

        if (result.hasErrors()) {
            return "modules/edit";
        }

        try {
            moduleService.updateModule(id, module);
            return "redirect:/modules?updated=true"; // Append query string flag for client-side feedback toast
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "modules/edit";
        }
    }

    /**
     * Triggers a destructive operation to drop and delete a course module from records.
     * Restricted strictly to ADMIN users only.
     */
    @GetMapping("/delete/{id}")
    public String deleteModule(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        // Authorization Check: Block malicious URL manipulation or accidental teacher access
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }

        moduleService.deleteModule(id); // Execute deletion strategy inside service layer
        return "redirect:/modules";
    }
}