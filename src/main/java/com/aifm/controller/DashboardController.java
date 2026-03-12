package com.aifm.controller;

import com.aifm.model.FocusSession;
import com.aifm.model.Task;
import com.aifm.repository.FocusSessionRepository;
import com.aifm.repository.TaskRepository;
import com.aifm.service.AIService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class DashboardController {

    private final TaskRepository taskRepository;
    private final FocusSessionRepository sessionRepository;
    private final AIService aiService;

    public DashboardController(TaskRepository taskRepository,
                               FocusSessionRepository sessionRepository,
                               AIService aiService) {
        this.taskRepository    = taskRepository;
        this.sessionRepository = sessionRepository;
        this.aiService         = aiService;
    }

    // ──────────────────────────────────────────
    // Dashboard
    // ──────────────────────────────────────────

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Task> tasks = taskRepository.findAll();

        // Sort: completed last → higher focus score → earlier due date
        tasks.sort((a, b) -> {
            if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
            int cmp = Integer.compare(b.getFocusScore(), a.getFocusScore());
            if (cmp != 0) return cmp;
            if (a.getDueDate() == null && b.getDueDate() == null) return 0;
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        List<FocusSession> sessions = sessionRepository.findAll();

        // Safe stats
        long total   = tasks.size();
        long done    = tasks.stream().filter(Task::isCompleted).count();
        long overdue = tasks.stream().filter(Task::isOverdue).count();

        // AI advice — wrapped in try/catch so a failure never crashes the page
        String aiAdvice;
        String dailyQuote;
        try {
            aiAdvice   = aiService.getAdvice(tasks);
            dailyQuote = aiService.getDailyMotivation(tasks);
        } catch (Exception e) {
            aiAdvice   = "AI is resting for now… try again later!";
            dailyQuote = "Stay focused. Small steps lead to big results.";
        }

        model.addAttribute("tasks",          tasks);
        model.addAttribute("sessions",       sessions);
        model.addAttribute("aiAdvice",       aiAdvice);
        model.addAttribute("dailyQuote",     dailyQuote);
        model.addAttribute("totalTasks",     total);
        model.addAttribute("completedTasks", done);
        model.addAttribute("overdueTasks",   overdue);

        return "dashboard";
    }

    // ──────────────────────────────────────────
    // Task CRUD
    // ──────────────────────────────────────────

    @PostMapping("/add-task")
    public String addTask(@RequestParam String title,
                          @RequestParam int difficulty,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate,
                          @RequestParam(defaultValue = "0") int estimatedMinutes) {
        Task task = new Task();
        task.setTitle(title);
        task.setDifficulty(difficulty);
        task.setDueDate(dueDate);
        task.setEstimatedMinutes(estimatedMinutes);
        task.setCompleted(false);
        taskRepository.save(task);
        return "redirect:/";
    }

    @PostMapping("/complete-task")
    public String completeTask(@RequestParam Long id) {
        taskRepository.findById(id).ifPresent(task -> {
            task.setCompleted(true);
            taskRepository.save(task);
        });
        return "redirect:/";
    }

    @PostMapping("/delete-completed")
    public String deleteCompletedTasks() {
        List<Task> completed = taskRepository.findAll().stream()
                .filter(Task::isCompleted)
                .toList();

        // Delete linked focus sessions first to avoid FK constraint error
        for (Task task : completed) {
            sessionRepository.deleteAll(sessionRepository.findByTaskId(task.getId()));
        }

        taskRepository.deleteAll(completed);
        return "redirect:/";
    }

    // ──────────────────────────────────────────
    // Focus Sessions
    // ──────────────────────────────────────────

    @PostMapping("/log-session")
    public String logSession(@RequestParam int duration,
                             @RequestParam int distractions,
                             @RequestParam(required = false) Long taskId) {
        FocusSession session = new FocusSession();
        session.setDuration(duration);
        session.setDistractions(distractions);

        if (taskId != null) {
            taskRepository.findById(taskId).ifPresent(session::setTask);
        }

        sessionRepository.save(session);
        return "redirect:/";
    }
}