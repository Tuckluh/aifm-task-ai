package com.aifm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private boolean completed;

    private LocalDateTime dueDate;

    @Column(nullable = false)
    private int estimatedMinutes;

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    // ===== COMPUTED / TRANSIENT FIELDS =====

    @Transient
    public int getFocusScore() {
        int score = difficulty * 10;
        if (isOverdue())        score += 30;
        else if (isDueSoon())   score += 15;
        if (estimatedMinutes > 0 && estimatedMinutes <= 30) score += 10;
        return score;
    }

    @Transient
    public String getPriorityLevel() {
        int s = getFocusScore();
        if (s >= 60) return "🔴 High";
        if (s >= 35) return "🟡 Medium";
        return "🟢 Low";
    }

    @Transient
    public String getDifficultyLabel() {
        return switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Normal";
            case 3 -> "Medium";
            case 4 -> "Hard";
            case 5 -> "Very Hard";
            default -> String.valueOf(difficulty);
        };
    }

    @Transient
    public String getFormattedDueDate() {
        if (dueDate == null) return "No due date";
        return dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a"));
    }

    @Transient
    public String getEstimatedLabel() {
        if (estimatedMinutes <= 0) return "—";
        if (estimatedMinutes < 60) return estimatedMinutes + " min";
        int h = estimatedMinutes / 60, m = estimatedMinutes % 60;
        return m == 0 ? h + " hr" : h + " hr " + m + " min";
    }

    @Transient
    public boolean isOverdue() {
        if (dueDate == null || completed) return false;
        return dueDate.isBefore(LocalDateTime.now());
    }

    @Transient
    public boolean isDueSoon() {
        if (dueDate == null || completed) return false;
        return !isOverdue() && dueDate.isBefore(LocalDateTime.now().plusHours(24));
    }

    // Set by AIService before tasks are passed to the view
    @Transient
    private String whyFirst;
    public String getWhyFirst() { return whyFirst; }
    public void setWhyFirst(String w) { this.whyFirst = w; }
}