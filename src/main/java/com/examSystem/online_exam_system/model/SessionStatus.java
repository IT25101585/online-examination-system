package com.examSystem.online_exam_system.model;

public enum SessionStatus {
    SCHEDULED,  // hasn't started yet
    ACTIVE,     // currently running
    CLOSED      // time is up, no more submissions
}

