//Controller for handling all account related actions
//view account settings
//update account details
//changing user password
//display the changed password


package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.service.UserService;
import jakarta.servlet.http.HttpSession;   //represent user's server side session
import jakarta.validation.Valid;  //triggers validation based on annotation
import org.springframework.beans.factory.annotation.Autowired;  //marks the class as a Spring MVC controller
import org.springframework.stereotype.Controller; //Automatically injects an object managed by Spring
import org.springframework.ui.Model; //sends data from controller to the thymeleaf view
import org.springframework.validation.BindingResult;  //stored validation errors produced by @Valid
import org.springframework.web.bind.annotation.*; //includes mapping + requesting parameter + model attributes

@Controller  //registers AccountController class as a web controller
@RequestMapping("/account")  //sets a base URL for all methods
public class AccountController {

    //dependency injection: allows the controller to use service layer methods
    @Autowired
    private UserService userService;

    // ---- VIEW ACCOUNT SETTINGS ----

    @GetMapping("/settings") //handles HTTP GET request to: "/account/settings"

    //parameters access session data and pass data to the view respectively
    public String showSettings(HttpSession session, Model model) {

        //login validation: if user is not logged in, redirect to login page
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }

        //retrieves loggedInUser object from the session
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        //load fresh user data: get the latest user data from the DB
        model.addAttribute("user", userService.getUserById(loggedInUser.getId()));

        //add session user to model (for display purposes)
        model.addAttribute("loggedInUser", loggedInUser);

        //return view loads: "templates/account/settings.html"
        return "account/settings";
    }

    // ---- UPDATE ACCOUNT SETTINGS ----


    @PostMapping("/settings") //handles form submission from the settings page

    //@Valid: triggers validation rules defined in User
    //ModelAttribute: binds form fields to a User object
    public String updateSettings(@Valid @ModelAttribute User user,
                                 BindingResult result, //contains validation errors
                                 HttpSession session,
                                 Model model) {

        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        //validation check: if failed, return to same page + show errors
        if (result.hasErrors()) {
            model.addAttribute("loggedInUser", loggedInUser);
            return "account/settings";
        }
        try {
            //security config related
            // keep the original role — users can't change their own role by manipulating html
            User existing = userService.getUserById(loggedInUser.getId());
            user.setRole(existing.getRole());

            //update user: saves updated data to DB
            userService.updateUser(loggedInUser.getId(), user);

            // update session with new details
            session.setAttribute("loggedInUser",
                    userService.getUserById(loggedInUser.getId()));

            //success msg displays
            model.addAttribute("success", "Account updated successfully!");
            model.addAttribute("loggedInUser",
                    SessionUtils.getLoggedInUser(session));
            model.addAttribute("user",
                    userService.getUserById(loggedInUser.getId()));
            return "account/settings";

        //exception handling: catches errors (i.e., duplicated emails)
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

    //display password change page
    @PostMapping("/changepassword")

    //@RequestParam: binds a single form field to a method parameter
    public String changePassword(@RequestParam String currentPassword,
    //first param: equivalent to <input name= "currentPassword"> in html
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 HttpSession session,
                                 Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);

        //add logged-in user
        model.addAttribute("loggedInUser", loggedInUser);

        try {
            // verify current password is correct: if invalid, throw exception
            userService.loginUser(loggedInUser.getEmail(), currentPassword);
        } catch (RuntimeException e) {
            model.addAttribute("error", "Current password is incorrect!");
            return "account/changepassword";
        }

        // password length validation: check new password is at least 8 characters
        if (newPassword.length() < 8) {
            model.addAttribute("error",
                    "New password must be at least 8 characters!");
            return "account/changepassword";
        }

        // confirm password validation: check new passwords match
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("error", "New passwords do not match!");
            return "account/changepassword";
        }

        // update password: changes only the password while preserving other fields
        User existing = userService.getUserById(loggedInUser.getId());
        existing.setPassword(newPassword);
        userService.updateUser(loggedInUser.getId(), existing);

        // update session: refreshes session data
        session.setAttribute("loggedInUser",
                userService.getUserById(loggedInUser.getId()));

        //success message
        model.addAttribute("success", "Password changed successfully!");
        return "account/changepassword";
    }
}

