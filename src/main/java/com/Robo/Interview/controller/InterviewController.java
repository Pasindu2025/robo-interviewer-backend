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
@CrossOrigin(origins = "*") // 👈 මේක "*" කළාම ඕනෑම තැනක සිට (Netlify) වැඩ කරනවා
public class InterviewController {

    // Railway එකේ අපි දාපු GROQ_API_KEY එක මෙතනින් කියවනවා
    private final String apiKey = System.getenv("GROQ_API_KEY");
    private final String url = "https://api.groq.com/openai/v1/chat/completions";

    @PostMapping("/validate")
    public Map<String, String> validateAnswer(@RequestBody Map<String, Object> request) {

        String prevQuestion = (String) request.get("question");
        String answer = (String) request.get("answer");

        Object countObj = request.get("totalQuestions");
        int count = (countObj != null) ? Integer.parseInt(countObj.toString()) : 1;

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> finalResponse = new HashMap<>();

        // API Key එක නැත්නම් error එකක් පෙන්වන්න (Debugging සඳහා)
        if (apiKey == null || apiKey.isEmpty()) {
            finalResponse.put("feedback", "Error: API Key is missing in Railway Variables!");
            finalResponse.put("nextQuestion", "Please set GROQ_API_KEY in Railway dashboard.");
            return finalResponse;
        }

        try {
            String systemInstruction = "You are a Senior Technical Lead conducting a natural, high-level technical interview. " +
                    "CURRENT PROGRESS: Round " + count + " of 10. " +
                    "THE CANDIDATE JUST ANSWERED: '" + answer + "' to your question: '" + prevQuestion + "'. " +
                    "Return ONLY JSON: {\"feedback\":\"...\",\"nextQuestion\":\"...\"}";

            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", "Feedback on my answer and ask the next question.")
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.8
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
                finalResponse.put("nextQuestion", "Excellent! You've completed all 10 technical rounds.");
                finalResponse.put("isFinished", "true");
            } else {
                finalResponse.put("nextQuestion", json.path("nextQuestion").asText());
                finalResponse.put("isFinished", "false");
            }

        } catch (Exception e) {
            finalResponse.put("feedback", "Backend Error: " + e.getMessage());
            finalResponse.put("nextQuestion", "Let's move on. Can you explain your experience with Git?");
            finalResponse.put("isFinished", "false");
        }

        return finalResponse;
    }
}