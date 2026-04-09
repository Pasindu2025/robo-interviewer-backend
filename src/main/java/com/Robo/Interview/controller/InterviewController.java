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
@CrossOrigin(origins = "*")
public class InterviewController {

    private final String apiKey = System.getenv("GROQ_API_KEY");
    private final String url = "https://api.groq.com/openai/v1/chat/completions";

    @PostMapping("/validate")
    public Map<String, String> validateAnswer(@RequestBody Map<String, Object> request) {

        String prevQuestion = (String) request.get("question");
        String answer = (String) request.get("answer");
        String category = (String) request.get("category"); // 👈 Frontend එකෙන් එන Category එක

        Object countObj = request.get("totalQuestions");
        int count = (countObj != null) ? Integer.parseInt(countObj.toString()) : 1;

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> finalResponse = new HashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            finalResponse.put("feedback", "Error: API Key is missing!");
            finalResponse.put("nextQuestion", "Set GROQ_API_KEY in Railway.");
            return finalResponse;
        }

        try {
            // AI එකට දෙන උපදෙස් මාලාව (Category එක අනුව)
            String systemInstruction = "You are a Senior Technical Lead. Target Role: " + (category != null ? category : "Software Engineer") + ". " +
                    "PROGRESS: Round " + count + " of 10. " +
                    "THE CANDIDATE ANSWERED: '" + answer + "' to: '" + prevQuestion + "'. " +
                    "INSTRUCTIONS: Provide brief feedback. If count < 10, ask the next technical question. " +
                    "If count == 10, provide a final summary of their performance and a score out of 100. " +
                    "Return ONLY JSON: {\"feedback\":\"...\",\"nextQuestion\":\"...\"}";

            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", "Evaluate answer and proceed.")
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.7
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

            if (count >= 10) {
                // රවුම් 10 ඉවර වුණාම feedback එක ඇතුළෙම score එක එනවා
                finalResponse.put("nextQuestion", "Session Completed. Thank you!");
                finalResponse.put("isFinished", "true");
            } else {
                finalResponse.put("nextQuestion", json.path("nextQuestion").asText());
                finalResponse.put("isFinished", "false");
            }

        } catch (Exception e) {
            finalResponse.put("feedback", "Backend Error: " + e.getMessage());
            finalResponse.put("nextQuestion", "Let's move on. Tell me more about your projects.");
            finalResponse.put("isFinished", "false");
        }

        return finalResponse;
    }
}