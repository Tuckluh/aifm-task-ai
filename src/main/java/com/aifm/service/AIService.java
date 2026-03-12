package com.aifm.service;

import com.aifm.model.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${huggingface.api.key:#{environment.HF_API_KEY}}")
    private String hfApiKey;

    // ──────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────

    public String getAdvice(List<Task> tasks) {

        if (tasks.isEmpty()) {
            return "Start by adding a task. Momentum begins with clarity.";
        }

        long completed = tasks.stream().filter(Task::isCompleted).count();
        long overdue = tasks.stream().filter(Task::isOverdue).count();

        if (completed == tasks.size()) {
            return "✅ All tasks completed. Take a break — you've earned it.";
        }

        if (overdue > 0) {
            return "⚠️ You have " + overdue + " overdue task(s). Tackle the most urgent one first.";
        }

        tasks.stream()
                .filter(t -> !t.isCompleted())
                .findFirst()
                .ifPresent(t -> t.setWhyFirst(
                        "Highest focus score (" + t.getFocusScore() + ") — start here!"
                ));

        String taskList = tasks.stream()
                .filter(t -> !t.isCompleted())
                .map(t -> "- " + t.getTitle()
                        + " (difficulty: " + t.getDifficultyLabel()
                        + ", due: " + t.getFormattedDueDate() + ")")
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt =
                "You are a productivity coach. Give a short 2-3 sentence actionable tip for someone with these tasks:"
                        + taskList;

        return queryHuggingFace(prompt);
    }

    public String getDailyMotivation(List<Task> tasks) {

        if (tasks.isEmpty()) {
            return "✨ Every big achievement starts with one small task.";
        }

        long completed = tasks.stream().filter(Task::isCompleted).count();
        long overdue = tasks.stream().filter(Task::isOverdue).count();

        if (overdue > 0) {
            return "⏰ Time is your most valuable resource — use it wisely today.";
        }

        if (completed == tasks.size()) {
            return "🎉 You're ahead of schedule. Consistency is your superpower.";
        }

        if (completed > 0) {
            return "🔥 Progress builds momentum. Keep going!";
        }

        return queryHuggingFace(
                "Give me one punchy motivational quote about productivity. Just the quote."
        );
    }

    // ──────────────────────────────────────────
    // Hugging Face API
    // ──────────────────────────────────────────

    private String queryHuggingFace(String prompt) {

        if (hfApiKey == null || hfApiKey.isBlank()) {
            return getFallbackMessage();
        }

        try {

            String formattedPrompt = "<s>[INST] " + prompt + " [/INST]";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(hfApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "inputs", formattedPrompt,
                    "parameters", Map.of(
                            "max_new_tokens", 80,
                            "temperature", 0.7,
                            "return_full_text", false
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://router.huggingface.co/hf-inference/models/mistralai/Mistral-7B-Instruct-v0.2",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseResponse(response.getBody());
            }

        } catch (Exception e) {
            System.err.println("[AIService] HuggingFace call failed: " + e.getMessage());
        }

        return getFallbackMessage();
    }

    private String parseResponse(String json) {

        try {

            JsonNode root = objectMapper.readTree(json);

            if (root.isArray() && root.size() > 0) {

                String text = root.get(0)
                        .path("generated_text")
                        .asText("");

                if (!text.isBlank()) {
                    return text.trim();
                }
            }

        } catch (Exception e) {
            System.err.println("[AIService] Parse error: " + e.getMessage());
        }

        return getFallbackMessage();
    }

    private String getFallbackMessage() {
        return "🤖 AI is offline. Add your Hugging Face API key to enable AI tips.";
    }
}