package com.devops.greenkube.service;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.custom.Quantity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
public class WasteAnalyzer {

    @Autowired
    private NotificationService notificationService;
    private final LeaderElectionService leaderService;
    private final PrometheusService prometheusService;
    private final AppsV1Api appsV1Api;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private final BlockingQueue<V1Deployment> workQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void startWorkerThread(){
        new Thread(() -> {
            while(true){
                try{
                    V1Deployment dep = workQueue.take();
                    threadPool.submit(() -> analyzeLogic(dep, "1h"));
                } catch( InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void enqueueForAnalysis(V1Deployment dep){
        workQueue.offer(dep);
    }

    @Scheduled(fixedRate = 300000)
    public void scheduledClusterAudit() {
        if(!leaderService.isCurrentLeader()){return;}
        System.out.println("🕰️ Starting Scheduled Historical Waste Audit...");
        try {

            V1DeploymentList deployments = appsV1Api.listDeploymentForAllNamespaces().execute();

            for (V1Deployment dep : deployments.getItems()) {
                threadPool.submit(() -> analyzeLogic(dep, "24h"));
            }
            System.out.println("✅ Audit Complete.");
        } catch(ApiException e){
            System.err.println("Kubernetes API error: "+ e.getResponseBody());
        }catch (Exception e) {
            System.err.println("Failed to fetch deployments for audit: " + e.getMessage());
        }
    }



    public void analyzeLogic(V1Deployment dep, String timeWindow) {
        String ns = dep.getMetadata().getNamespace();
        String name = dep.getMetadata().getName();

        if (ns.equals("kube-system") || ns.equals("monitoring") || ns.equals("argocd")) {
            return;
        }


        try {
            if (dep.getSpec().getTemplate().getSpec().getContainers().isEmpty()) return;
            var resources = dep.getSpec().getTemplate().getSpec().getContainers().get(0).getResources();
            if (resources == null || resources.getLimits() == null) return;

            // --- MEMORY ANALYSIS ---
            if (resources.getLimits().containsKey("memory")) {
                Quantity memLimit = resources.getLimits().get("memory");
                double limitMB = memLimit.getNumber().doubleValue() / 1024 / 1024;
                double maxUsageMB = prometheusService.getMaxMemoryUsageBytes(ns, name, timeWindow) / 1024 / 1024;

                if (maxUsageMB > 0) {
                    double memUtil = (maxUsageMB / limitMB) * 100.0;
                    if (memUtil < 20.0) {
                        System.out.printf("🚨 MEMORY WASTE [%s]: Limit=%.0fMB, Peak=%.0fMB (%.1f%% utilized in %s)\n", name, limitMB, maxUsageMB, memUtil, timeWindow);
                        String msg = String.format("MEMORY WASTE: [%s]\nLimit: '%.0fMB' | Peak: '%.0fMB' ('%.1f%%' utilized)", name, limitMB, maxUsageMB, memUtil);
                        notificationService.sendAlert(msg);
                    }
                }
            }

            // --- CPU ANALYSIS ---
            if (resources.getLimits().containsKey("cpu")) {
                Quantity cpuLimit = resources.getLimits().get("cpu");
                double limitCores = cpuLimit.getNumber().doubleValue(); // Converts "500m" to 0.5
                double maxUsageCores = prometheusService.getMaxCpuUsageCores(ns, name, timeWindow);

                if (maxUsageCores > 0) {
                    double cpuUtil = (maxUsageCores / limitCores) * 100.0;
                    if (cpuUtil < 10.0) {
                        System.out.printf("🚨 CPU IDLE WASTE [%s]: Limit=%.2f Cores, Peak=%.3f Cores (%.1f%% utilized in %s)\n", name, limitCores, maxUsageCores, cpuUtil, timeWindow);
                        String msg = String.format("CPU IDLE : [%s]\nLimit: '%.2f Cores' | Peak: '%.3f Cores' ('%.1f%%' utilized)", name, limitCores, maxUsageCores, cpuUtil);
                        notificationService.sendAlert(msg);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error analyzing " + name + ": " + e.getMessage());
        }
    }
}