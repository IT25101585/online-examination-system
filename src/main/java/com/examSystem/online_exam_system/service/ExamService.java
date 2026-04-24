package com.examSystem.online_exam_system.service;

import com.examSystem.online_exam_system.model.Exam;
import com.examSystem.online_exam_system.model.ExamStatus;
import com.examSystem.online_exam_system.model.User;
import com.examSystem.online_exam_system.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    // ---- CREATE EXAM ----
    // saves a new exam to the database
    // automatically sets the creator and status to DRAFT
    public Exam createExam(Exam exam, User createdBy) {
        exam.setCreatedBy(createdBy);
        exam.setStatus(ExamStatus.DRAFT);
        return examRepository.save(exam);
    }

    // ---- GET ALL EXAMS ----
    // returns every exam — for admins only
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    // ---- GET PUBLISHED EXAMS ----
    // returns only published exams — for students
    public List<Exam> getPublishedExams() {
        return examRepository.findByStatus(ExamStatus.PUBLISHED);
    }

    // ---- GET EXAMS BY CREATOR ----
    // returns all exams created by a specific teacher/admin
    public List<Exam> getExamsByCreator(User createdBy) {
        return examRepository.findByCreatedBy(createdBy);
    }

    // ---- GET EXAM BY ID ----
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found!"));
    }

    // ---- UPDATE EXAM ----
    public Exam updateExam(Long id, Exam updatedExam) {
        Exam existing = getExamById(id);
        existing.setTitle(updatedExam.getTitle());
        existing.setDescription(updatedExam.getDescription());
        existing.setDurationMins(updatedExam.getDurationMins());
        existing.setTotalMarks(updatedExam.getTotalMarks());
        return examRepository.save(existing);
    }

    // ---- PUBLISH EXAM ----
    // changes status from DRAFT to PUBLISHED
    // students can now see and take this exam
    public Exam publishExam(Long id) {
        Exam exam = getExamById(id);
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    // ---- UNPUBLISH EXAM ----
    // changes status back to DRAFT
    // hides the exam from students
    public Exam unpublishExam(Long id) {
        Exam exam = getExamById(id);
        exam.setStatus(ExamStatus.DRAFT);
        return examRepository.save(exam);
    }

    // ---- DELETE EXAM ----
    public void deleteExam(Long id) {
        getExamById(id);
        examRepository.deleteById(id);
    }
}