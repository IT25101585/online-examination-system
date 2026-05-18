package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/")
    public String landing(HttpSession session) {
        if (SessionUtils.isLoggedIn(session)) {
            return "redirect:/dashboard";
        }
        return "index";
    }
}

