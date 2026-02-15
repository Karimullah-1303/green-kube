package com.devops.greenkube.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Value("${slack.webhook.url}")
    private String slackUrl;
    private final RestTemplate restTemplate;
    public NotificationService() { this.restTemplate = new RestTemplate();
    }

    public void sendAlert(String message) {
        try {
           Map<String, String> payload = new HashMap<>();
           payload.put("text", message);

            restTemplate.postForObject(slackUrl+"/alerts", payload, String.class);
            System.out.println("Sent Alert to Slack");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send Slack alert: " + e.getMessage());
        }
    }
}