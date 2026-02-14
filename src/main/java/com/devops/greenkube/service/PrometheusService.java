package com.devops.greenkube.service;

import com.devops.greenkube.config.K8sConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class PrometheusService {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private K8sConfig k8sConfig;

    public double getMaxMemoryUsageBytes(String namespace, String appName, String duration) {
        try {
            // Added max() wrapper to ensure we get a single highest value across all replicas
            String query = String.format("max(max_over_time(container_memory_usage_bytes{namespace=\"%s\", pod=~\"%s-.*\"}[%s]))", namespace, appName, duration);

            String url = K8sConfig.PROMETHEUS_URL + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("data").path("result");

            if (result.isArray() && result.size() > 0) {
                // Parse the PromQL scalar result (e.g., [1707482400.0, "154321000"])
                return Double.parseDouble(result.get(0).path("value").get(1).asText());
            }
        } catch(Exception e) {
            System.err.println("Prometheus Error for " + appName + ": " + e.getMessage());
        }
        return 0.0;
    }

    public double getMaxCpuUsageCores(String namespace, String appName, String duration){
        try{
            String query = String.format("max(max_over_time(rate(container_cpu_usage_seconds_total{namespace=\"%s\", pod=~\"%s-.*\"}[5m])[%s:5m]))", namespace, appName, duration);

            String url = k8sConfig.PROMETHEUS_URL + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("data").path("result");

            if(result.isArray() && result.size() > 0) {
                return Double.parseDouble(result.get(0).path("value").get(1).asText());

            }
        } catch (Exception e) {
            System.err.println("Prometheus CPU error for " + appName + ": " + e.getMessage());
        }
        return 0.0;
    }

}
