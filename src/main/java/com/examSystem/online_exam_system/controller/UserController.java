package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.Role;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller  //this class handles Web requests
@RequestMapping("/users")  //base URL; all methods inside this class starts with '/users'
public class UserController {

    @Autowired
    private UserService userService;

    //displaying registration form
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "users/register";
    }

    // ---- HANDLE REGISTRATION FORM SUBMIT ----
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "users/register";
        }
        try {
            userService.registerUser(user);
            return "redirect:/users/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "users/register";
        }
    }

    // ---- SHOW LOGIN FORM ----
    @GetMapping("/login")
    public String showLoginForm() {
        return "users/login";
    }

    // ---- HANDLE LOGIN FORM SUBMIT ----
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {
        try {
            User user = userService.loginUser(email, password);
            session.setAttribute("loggedInUser", user);
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "users/login";
        }
    }

    // ---- LOGOUT ----
    // clears the session so the user is logged out
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/users/login";
    }

    // ---- VIEW PROFILE ----
    // only accessible if logged in AND (admin OR own profile)
    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id,
                              HttpSession session,
                              Model model) {
        // if not logged in, send to login page
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        // if not admin and not the owner, show access denied
        if (!SessionUtils.canAccessProfile(session, id)) {
            model.addAttribute("error", "Access denied! You can only view your own profile.");
            return "users/accessdenied";
        }
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        // pass the logged in user so we can show their name in the page
        model.addAttribute("loggedInUser", SessionUtils.getLoggedInUser(session));
        return "users/profile";
    }

    // ---- SHOW EDIT FORM ----
    // only accessible if logged in AND (admin OR own profile)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (!SessionUtils.canAccessProfile(session, id)) {
            model.addAttribute("error", "Access denied! You can only edit your own profile.");
            return "users/accessdenied";
        }
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        // only admins can change roles — pass this flag to the template
        model.addAttribute("isAdmin", SessionUtils.isAdmin(session));
        model.addAttribute("roles", Role.values());
        return "users/edit";
    }

    // ---- HANDLE EDIT FORM SUBMIT ----
    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute User user,
                             BindingResult result,
                             HttpSession session,
                             Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        if (!SessionUtils.canAccessProfile(session, id)) {
            model.addAttribute("error", "Access denied!");
            return "users/accessdenied";
        }
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("isAdmin", SessionUtils.isAdmin(session));
            return "users/edit";
        }
        try {
            // if not admin, keep the original role (can't promote yourself!)
            if (!SessionUtils.isAdmin(session)) {
                User existing = userService.getUserById(id);
                user.setRole(existing.getRole());
            }
            userService.updateUser(id, user);
            // update the session with new details if editing own profile
            if (SessionUtils.isOwner(session, id)) {
                session.setAttribute("loggedInUser", userService.getUserById(id));
            }
            return "redirect:/users/profile/" + id;
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "users/edit";
        }
    }

    // ---- VIEW ALL USERS (admin only) ----
    @GetMapping("/all")
    public String getAllUsers(HttpSession session, Model model) {
        // only admins can see all users
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/all";
    }

    // ---- DELETE USER (admin only) ----
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id,
                             HttpSession session,
                             Model model) {
        // only admins can delete users
        if (!SessionUtils.isAdmin(session)) {
            model.addAttribute("error", "Access denied! Admins only.");
            return "users/accessdenied";
        }
        // prevent admin from deleting themselves
        if (SessionUtils.isOwner(session, id)) {
            model.addAttribute("error", "You cannot delete your own account!");
            List<User> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "users/all";
        }
        userService.deleteUser(id);
        return "redirect:/users/all";
    }
}