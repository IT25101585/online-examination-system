package com.examSystem.online_exam_system.model;

public enum ExamStatus {
    DRAFT, // exam is still being prepared, students can't see it
    PENDING,  //teacher submitted for admin approval
    PUBLISHED,  // exam is ready, students can see and take it
    REJECTED //admin rejected- needs revision
}

