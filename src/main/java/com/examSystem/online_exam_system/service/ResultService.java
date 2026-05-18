package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.ResultRepository;
import com.examSystem.online_exam_system.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    // ---- CALCULATE AND SAVE RESULT ----
    // takes a completed attempt and saves a proper Result record
    public Result saveResult(ExamAttempt attempt) {
        // check if result already exists for this attempt
        if (resultRepository.findByAttempt(attempt).isPresent()) {
            return resultRepository.findByAttempt(attempt).get();
        }

        Result result = new Result();
        result.setStudent(attempt.getStudent());
        result.setExam(attempt.getExam());
        result.setAttempt(attempt);
        result.setTotalScore(attempt.getTotalScore());
        result.setTotalMarks(attempt.getExam().getTotalMarks());

        // calculate percentage
        double percentage = ((double) attempt.getTotalScore() /
                attempt.getExam().getTotalMarks()) * 100;
        result.setPercentage(Math.round(percentage * 10.0) / 10.0);

        // calculate grade based on percentage
        result.setGrade(calculateGrade(percentage));

        // pass if 50% or above
        result.setPassed(percentage >= 50);

        // not approved yet — teacher must review first
        result.setTeacherApproved(false);

        return resultRepository.save(result);
    }

    // ---- APPROVE RESULT (teacher) ----
    // teacher reviews and approves — student can now see result
    public Result approveResult(Long resultId, String teacherNote,
                                Integer overriddenScore) {
        Result result = getResultById(resultId);

        // if teacher overrode the score, recalculate
        if (overriddenScore != null &&
                !overriddenScore.equals(result.getTotalScore())) {
            result.setTotalScore(Double.valueOf(overriddenScore));

            double percentage = ((double) overriddenScore /
                    result.getTotalMarks()) * 100;
            result.setPercentage(Math.round(percentage * 10.0) / 10.0);
            result.setGrade(calculateGrade(percentage));
            result.setPassed(percentage >= 50);
        }

        result.setTeacherApproved(true);
        result.setApprovedAt(LocalDateTime.now());
        if (teacherNote != null && !teacherNote.trim().isEmpty()) {
            result.setTeacherNote(teacherNote);
        }
        return resultRepository.save(result);
    }

    // ---- GET PENDING REVIEW RESULTS ----
    public List<Result> getPendingReviewResults() {
        return resultRepository.findByTeacherApproved(false);
    }

    // ---- GET PENDING REVIEW FOR EXAM ----
    public List<Result> getPendingReviewByExam(Exam exam) {
        return resultRepository.findByExamAndTeacherApproved(exam, false);
    }

    // ---- GET APPROVED RESULTS FOR STUDENT ----
    // students only see approved results
    public List<Result> getApprovedResultsByStudent(User student) {
        return resultRepository.findByStudentAndTeacherApproved(student, true);
    }

    // ---- CALCULATE GRADE ----
    // returns a letter grade based on percentage
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    // ---- GET RESULT BY ID ----
    public Result getResultById(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Result not found!"));
    }

    // ---- GET ALL RESULTS FOR A STUDENT ----
    // used for exam history page
    public List<Result> getResultsByStudent(User student) {
        return resultRepository.findByStudent(student);
    }

    // ---- GET ALL RESULTS FOR AN EXAM ----
    // used by teachers/admins to see all student results
    public List<Result> getResultsByExam(Exam exam) {
        return resultRepository.findByExam(exam);
    }

    // ---- GET RESULT BY ATTEMPT ----
    public Result getResultByAttempt(ExamAttempt attempt) {
        return resultRepository.findByAttempt(attempt)
                .orElseThrow(() -> new RuntimeException("Result not found!"));
    }

    // ---- GET PASS COUNT FOR STUDENT ----
    public long getPassCount(User student) {
        return resultRepository.countByStudentAndPassed(student, true);
    }

    // ---- GET FAIL COUNT FOR STUDENT ----
    public long getFailCount(User student) {
        return resultRepository.countByStudentAndPassed(student, false);
    }
}
