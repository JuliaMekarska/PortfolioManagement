package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String FASTAPI_URL = "http://127.0.0.1:5049/items";

    private String convId = null;

    @PostMapping("/question")
    public Map<String, String> askQuestion(@RequestParam String question) {
        try {
            if (convId == null) {
                Map<String, Object> createResp = restTemplate.postForObject(
                        FASTAPI_URL + "/conversations",
                        null,
                        Map.class
                );
                convId = (String) createResp.get("conversation_id");
            }

            Map<String, String> messageBody = Map.of("message", question);
            Map<String, Object> answerResp = restTemplate.postForObject(
                    FASTAPI_URL + "/conversations/" + convId + "/message",
                    messageBody,
                    Map.class
            );

            return Map.of(
                    "question", question,
                    "answer", (String) answerResp.get("answer")
            );

        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
