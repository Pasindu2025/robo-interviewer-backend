package com.Robo.Interview.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "http://localhost:5173")
public class InterviewController {

    private final String apiKey = "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";
    private final String url = "https://api.groq.com/openai/v1/chat/completions";

    @PostMapping("/validate")
    public Map<String, String> validateAnswer(@RequestBody Map<String, Object> request) {

        String prevQuestion = (String) request.get("question");
        String answer = (String) request.get("answer");

        // Frontend count eka gannawa current stage eka track karanna
        Object countObj = request.get("totalQuestions");
        int count = (countObj != null) ? Integer.parseInt(countObj.toString()) : 1;

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> finalResponse = new HashMap<>();

        try {
            // 🧠 CONVERSATIONAL LOGIC: Rigid roadmap eka ain kala.
            String systemInstruction = "You are a Senior Technical Lead conducting a natural, high-level technical interview. " +
                    "CURRENT PROGRESS: Round " + count + " of 10. " +
                    "THE CANDIDATE JUST ANSWERED: '" + answer + "' to your question: '" + prevQuestion + "'. " +

                    "STRICT CONVERSATIONAL RULES: " +
                    "1. NO FIXED SCRIPT: Do not follow a set list of topics. Instead, let the conversation flow naturally. " +
                    "2. DIVE DEEPER: If the candidate mentions a specific technology or concept in their answer, ask a follow-up question about it to test their depth. " +
                    "3. DIVERSITY: If a topic is exhausted, pivot to a new area (e.g., from Frontend to Databases or Security). " +
                    "4. REALISM: Act like a human interviewer. If an answer is too short, ask for more details. If it's wrong, gently challenge them. " +
                    "5. NO REPETITION: Never ask the same question or topic twice. " +
                    "6. Round " + count + " Focus: Keep the difficulty appropriate for the current round, getting harder as we reach Round 10. " +

                    "Return ONLY JSON: {\"feedback\":\"...\",\"nextQuestion\":\"...\"}";

            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", "Feedback on my answer and ask the next question.")
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.8 // Randomness/Creativity wadi kala
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String aiRawContent = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode json = mapper.readTree(aiRawContent);

            finalResponse.put("feedback", json.path("feedback").asText());

            // Step 10 check
            if (count >= 10) {
                finalResponse.put("nextQuestion", "Excellent! You've completed all 10 technical rounds. We'll be in touch.");
                finalResponse.put("isFinished", "true");
            } else {
                finalResponse.put("nextQuestion", json.path("nextQuestion").asText());
                finalResponse.put("isFinished", "false");
            }

        } catch (Exception e) {
            System.err.println("Backend Error: " + e.getMessage());
            finalResponse.put("feedback", "I see. That's interesting.");
            finalResponse.put("nextQuestion", "Let's move on. Can you explain your experience with Git and version control?");
            finalResponse.put("isFinished", "false");
        }

        return finalResponse;
    }
}