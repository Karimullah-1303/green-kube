package com.devops.greenkube.service;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.custom.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WasteAnalyzer {

    private final PrometheusService prometheusService;

    public void analyze(V1Deployment dep) {
        String ns = dep.getMetadata().getNamespace();
        String name = dep.getMetadata().getName();

        // 1. SKIP SYSTEM APPS (Reduces noise)
        if (ns.equals("kube-system") || ns.equals("monitoring") || ns.equals("argocd")) {
            return; // Silently ignore system stuff
        }

        // 2. Get Configured Limit
        try {
            if (dep.getSpec().getTemplate().getSpec().getContainers().isEmpty()) return;

            var resources = dep.getSpec().getTemplate().getSpec().getContainers().get(0).getResources();
            if (resources == null || resources.getLimits() == null || !resources.getLimits().containsKey("memory")) {
                // System.out.println("[SKIP] " + name + " has no memory limits"); // Commented out to reduce noise
                return;
            }

            Quantity limitQuantity = resources.getLimits().get("memory");
            double limitBytes = limitQuantity.getNumber().doubleValue();

            // 3. Get Actual Usage
            double maxUsageBytes = prometheusService.getMaxMemoryUsageBytes(ns, name, "1h");

            if (maxUsageBytes == 0) {
                return; // No data yet
            }

            // 4. Calculate Waste
            double utilization = (maxUsageBytes / limitBytes) * 100.0;

            // FIXED PRINTF LINE: Used String.format to be safer
            String msg = String.format("ANALYSIS [%s/%s]: Limit=%.2f MB | MaxUsage=%.2f MB | Utilization=%.2f%%",
                    ns, name, limitBytes / 1024 / 1024, maxUsageBytes / 1024 / 1024, utilization);

            System.out.println(msg);

            if (utilization < 20.0) {
                System.out.println("🚨 WASTE DETECTED! Recommendation: Downsize " + name);
            }

        } catch (Exception e) {
            System.err.println("Error analyzing " + name);
            e.printStackTrace();
        }
    }
}