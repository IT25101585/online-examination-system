package com.examSystem.online_exam_system.controller;

import com.examSystem.online_exam_system.config.SessionUtils;
import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ResultService resultService;

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session)) {
            return "redirect:/users/login";
        }
        User loggedInUser = SessionUtils.getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        switch (loggedInUser.getRole()) {
            case ADMIN:
                model.addAttribute("totalUsers",
                        userService.getAllUsers().size());
                model.addAttribute("totalExams",
                        examService.getAllExams().size());
                model.addAttribute("totalTeachers",
                        userService.countByRole(Role.TEACHER));
                model.addAttribute("totalStudents",
                        userService.countByRole(Role.STUDENT));
                model.addAttribute("recentLogins",
                        userService.getRecentLogins());
                model.addAttribute("pendingExams",
                        examService.getPendingExams());
                model.addAttribute("pendingCount",
                        examService.getPendingExams().size());
                break;

            case TEACHER:
                examSessionService.autoCloseSessions();
                List<Exam> myExams =
                        examService.getExamsByCreator(loggedInUser);

                long pendingApproval = myExams.stream()
                        .filter(e -> e.getStatus() == ExamStatus.PENDING)
                        .count();
                long publishedExams = myExams.stream()
                        .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                        .count();

                List<Exam> rejectedExams = myExams.stream()
                        .filter(e -> e.getStatus() == ExamStatus.REJECTED)
                        .collect(Collectors.toList());
                List<Exam> approvedExams = myExams.stream()
                        .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                        .collect(Collectors.toList());

                List<ExamSession> allMySessions = myExams.stream()
                        .flatMap(e -> examSessionService
                                .getSessionsByExam(e.getId()).stream())
                        .collect(Collectors.toList());

                List<ExamSession> activeSessions =
                        allMySessions.stream()
                                .filter(ExamSession::isActive)
                                .collect(Collectors.toList());
                List<ExamSession> upcomingSessions =
                        allMySessions.stream()
                                .filter(ExamSession::isUpcoming)
                                .collect(Collectors.toList());

                List<Result> pendingGrading =
                        resultService.getPendingReviewResults()
                                .stream()
                                .filter(r -> myExams.stream()
                                        .anyMatch(e -> e.getId()
                                                .equals(r.getExam().getId())))
                                .collect(Collectors.toList());

                model.addAttribute("myExams", myExams);
                model.addAttribute("totalMyExams", myExams.size());
                model.addAttribute("pendingApproval", pendingApproval);
                model.addAttribute("publishedExams", publishedExams);
                model.addAttribute("totalStudents",
                        userService.countByRole(Role.STUDENT));
                model.addAttribute("pendingGradingCount",
                        pendingGrading.size());
                model.addAttribute("activeSessions", activeSessions);
                model.addAttribute("upcomingSessions",
                        upcomingSessions);
                model.addAttribute("pendingGrading", pendingGrading);
                model.addAttribute("rejectedExams", rejectedExams);
                model.addAttribute("approvedExams", approvedExams);
                break;

            case STUDENT:
                examSessionService.autoCloseSessions();
                List<Exam> publishedExamList =
                        examService.getPublishedExams();
                List<ExamSession> allSessions =
                        publishedExamList.stream()
                                .flatMap(e -> examSessionService
                                        .getSessionsByExam(e.getId()).stream())
                                .collect(Collectors.toList());

                List<ExamSession> activeStudentSessions =
                        allSessions.stream()
                                .filter(ExamSession::isActive)
                                .collect(Collectors.toList());
                List<ExamSession> upcomingStudentSessions =
                        allSessions.stream()
                                .filter(ExamSession::isUpcoming)
                                .collect(Collectors.toList());

                model.addAttribute("availableExams",
                        publishedExamList);
                model.addAttribute("totalAvailable",
                        publishedExamList.size());
                model.addAttribute("activeSessions",
                        activeStudentSessions);
                model.addAttribute("upcomingSessions",
                        upcomingStudentSessions);
                break;
        }
        return "dashboard";
    }
}

