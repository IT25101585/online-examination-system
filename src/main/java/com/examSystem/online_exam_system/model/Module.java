package com.examSystem.online_exam_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Represents a subject module (e.g., "OOP", "Data Structures").
 * Created by admin only. Questions are tagged to a module.
 * Teachers can filter the question bank by module when creating exams.
 */
@Entity
@Table(name = "modules") // Specifies the database table name for modules
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @NotBlank(message = "Module name is required")
    @Column(nullable = false, unique = true) // Database constraints: Cannot be null and must be unique
    private String name; // Name of the module (e.g., Information Technology, Discrete Mathematics)

    @Column(columnDefinition = "TEXT") // Allows storing longer descriptions in the database
    private String description; // Brief overview or syllabus details of the module

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Timestamp indicating when the module was added to the system

    @PrePersist // Lifecycle callback that runs automatically right before saving a new record
    protected void onCreate() {
        createdAt = LocalDateTime.now(); // Captures and assigns the current system date and time
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}