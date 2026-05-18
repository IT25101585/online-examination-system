package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.*;
import com.examSystem.online_exam_system.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    // ---- SAVE RESULT ----
    // called automatically when student submits exam
    // teacherApproved = false by default
    // student cannot see result until teacher approves
    public Result saveResult(ExamAttempt attempt) {
        // if result already saved for this attempt, return it
        // prevents double-saving if student visits result page twice
        if (resultRepository.findByAttempt(attempt)
                .isPresent()) {
            return resultRepository
                    .findByAttempt(attempt).get();
        }

        Result result = new Result();
        result.setStudent(attempt.getStudent());
        result.setExam(attempt.getExam());
        result.setAttempt(attempt);
        result.setTotalScore(
                attempt.getTotalScore() != null ?
                        attempt.getTotalScore() : 0);
        result.setTotalMarks(
                attempt.getExam().getTotalMarks() != null ?
                        attempt.getExam().getTotalMarks() : 1.0);

        // guard against divide by zero
        double totalMarks =
                result.getTotalMarks() > 0 ?
                        result.getTotalMarks() : 1.0;
        double percentage =
                ((double) result.getTotalScore() /
                        totalMarks) * 100;
        result.setPercentage(
                Math.round(percentage * 10.0) / 10.0);
        result.setGrade(calculateGrade(percentage));
        result.setPassed(percentage >= 50);

        // not visible to student until teacher approves
        result.setTeacherApproved(false);

        return resultRepository.save(result);
    }

    // ---- APPROVE RESULT (teacher) ----
    // teacher can override the auto-calculated score
    // useful for short answer questions where wording differs
    public Result approveResult(Long resultId,
                                String teacherNote,
                                Integer overriddenScore) {
        Result result = getResultById(resultId);

        // if teacher overrode the score, recalculate everything
        if (overriddenScore != null &&
                !overriddenScore.equals(result.getTotalScore())) {
            result.setTotalScore(Double.valueOf(overriddenScore));

            double totalMarks =
                    result.getTotalMarks() > 0 ?
                            result.getTotalMarks() : 1.0;
            double percentage =
                    ((double) overriddenScore / totalMarks) * 100;
            result.setPercentage(
                    Math.round(percentage * 10.0) / 10.0);
            result.setGrade(calculateGrade(percentage));
            result.setPassed(percentage >= 50);
        }

        result.setTeacherApproved(true);
        result.setApprovedAt(LocalDateTime.now());
        if (teacherNote != null &&
                !teacherNote.trim().isEmpty()) {
            result.setTeacherNote(teacherNote);
        }
        return resultRepository.save(result);
    }

    // ---- GET PENDING REVIEW ----
    public List<Result> getPendingReviewResults() {
        return resultRepository.findByTeacherApproved(false);
    }

    public List<Result> getPendingReviewByExam(Exam exam) {
        return resultRepository
                .findByExamAndTeacherApproved(exam, false);
    }

    // ---- GET APPROVED RESULTS FOR STUDENT ----
    // what student sees in their history
    public List<Result> getApprovedResultsByStudent(
            User student) {
        return resultRepository
                .findByStudentAndTeacherApproved(student, true);
    }

    // ---- GET ALL RESULTS FOR STUDENT ----
    // what teacher/admin sees
    public List<Result> getResultsByStudent(User student) {
        return resultRepository.findByStudent(student);
    }

    // ---- GET ALL RESULTS FOR EXAM ----
    public List<Result> getResultsByExam(Exam exam) {
        return resultRepository.findByExam(exam);
    }

    // ---- GET RESULT BY ATTEMPT ----
    public Result getResultByAttempt(ExamAttempt attempt) {
        return resultRepository.findByAttempt(attempt)
                .orElseThrow(() ->
                        new RuntimeException("Result not found!"));
    }

    // ---- GET RESULT BY ID ----
    public Result getResultById(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Result not found!"));
    }

    // ---- PASS/FAIL COUNTS ----
    // counts ALL results (approved or not) for stat cards
    public long getPassCount(User student) {
        return resultRepository
                .findByStudent(student)
                .stream()
                .filter(r -> r.getPassed() != null &&
                        r.getPassed())
                .count();
    }

    public long getFailCount(User student) {
        return resultRepository
                .findByStudent(student)
                .stream()
                .filter(r -> r.getPassed() != null &&
                        !r.getPassed())
                .count();
    }

    // ---- GRADE CALCULATION ----
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }
}