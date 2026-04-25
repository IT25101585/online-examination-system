package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private UserService userService;

    // ---- VIEW ACCOUNT SETTINGS ----
    @GetMapping("/settings")
    public String showSettings(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        model.addAttribute("user", userService.getUserById(loggedInUser.getId()));
        model.addAttribute("loggedInUser", loggedInUser);
        return "account/settings";
    }

    // ---- UPDATE ACCOUNT SETTINGS ----
    @PostMapping("/settings")
    public String updateSettings(@Valid @ModelAttribute User user,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        if (result.hasErrors()) {
            model.addAttribute("loggedInUser", loggedInUser);
            return "account/settings";
        }
        try {
            // keep the original role — users can't change their own role
            User existing = userService.getUserById(loggedInUser.getId());
            user.setRole(existing.getRole());
            userService.updateUser(loggedInUser.getId(), user);

            // update session with new details
            session.setAttribute("loggedInUser",
                    userService.getUserById(loggedInUser.getId()));
            model.addAttribute("success", "Account updated successfully!");
            model.addAttribute("loggedInUser",
                    SessionUtils.getLoggedInUser(session));
            model.addAttribute("user",
                    userService.getUserById(loggedInUser.getId()));
            return "account/settings";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("loggedInUser", loggedInUser);
            return "account/settings";
        }
    }

    // ---- SHOW CHANGE PASSWORD FORM ----
    @GetMapping("/changepassword")
    public String showChangePassword(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
        return "account/changepassword";
    }

    // ---- HANDLE CHANGE PASSWORD ----
    @PostMapping("/changepassword")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        try {
            // verify current password is correct
            userService.loginUser(loggedInUser.getEmail(), currentPassword);
        } catch (RuntimeException e) {
            model.addAttribute("error", "Current password is incorrect!");
            return "account/changepassword";
        }

        // check new password is at least 8 characters
        if (newPassword.length() < 8) {
            model.addAttribute("error",
                    "New password must be at least 8 characters!");
            return "account/changepassword";
        }

        // check new passwords match
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("error", "New passwords do not match!");
            return "account/changepassword";
        }

        // update password
        User existing = userService.getUserById(loggedInUser.getId());
        existing.setPassword(newPassword);
        userService.updateUser(loggedInUser.getId(), existing);

        // update session
        session.setAttribute("loggedInUser",
                userService.getUserById(loggedInUser.getId()));
        model.addAttribute("success", "Password changed successfully!");
        return "account/changepassword";
    }
}