package com.examSystem.online_exam_system.config;

import com.examSystem.online_exam_system.model.Role;
import com.examSystem.online_exam_system.model.User;
import jakarta.servlet.http.HttpSession;

// a utility class with static helper methods for checking roles
// "static" means we can call these methods without creating an object
public class SessionUtils {

    // gets the currently logged in user from the session
    // returns null if nobody is logged in
    public static User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    // checks if anyone is logged in at all
    public static boolean isLoggedIn(HttpSession session) {
        return getLoggedInUser(session) != null;
    }

    // checks if the logged in user is an ADMIN
    public static boolean isAdmin(HttpSession session) {
        User user = getLoggedInUser(session);
        return user != null && user.getRole() == Role.ADMIN;
    }

    // checks if the logged in user owns the profile with this id
    // used to check if a student/teacher can edit their OWN profile
    public static boolean isOwner(HttpSession session, Long profileId) {
        User user = getLoggedInUser(session);
        return user != null && user.getId().equals(profileId);
    }

    // checks if the logged in user can access a profile
    // allowed if they are admin OR they own the profile
    public static boolean canAccessProfile(HttpSession session, Long profileId) {
        return isAdmin(session) || isOwner(session, profileId);
    }
}