package com.devops.greenkube.service;

import com.google.protobuf.Api;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.Lock;
import io.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@Service
public class LeaderElectionService {

    private final ApiClient apiClient;
    private boolean isLeader = false;


    public LeaderElectionService(ApiClient apiClient) {
        System.out.println("Leader Election Service Started");
        this.apiClient = apiClient;
        Configuration.setDefaultApiClient(apiClient);
    }

    @PostConstruct
    public void startElection() {
        System.out.println("Start Election started");

        String tempPodName = System.getenv("HOSTNAME");
        if (tempPodName == null || tempPodName.isEmpty()) {
            tempPodName = "local-machine-" + UUID.randomUUID().toString();
        }

        final String podName = tempPodName;

        System.out.println("Pod name: " + podName);
        Lock lock = new LeaseLock(
                "default",
                "green-kube-lock",
                podName,
                apiClient
        );


        LeaderElectionConfig config = new LeaderElectionConfig(
                lock,
                Duration.ofSeconds(15),
                Duration.ofSeconds(10),
                Duration.ofSeconds(2)
        );


        new Thread(() -> {
            System.out.println("Thread started");

            LeaderElector elector = new LeaderElector(config);

            elector.run(
                    () -> {
                        System.out.println("👑 I AM THE LEADER! (" + podName + ") Starting duties...");
                        isLeader = true;
                    },
                    () -> {
                        System.out.println("I am No a leader! (" + podName + ") Stopping duties...");
                        isLeader = false;
                    }
            );
        }).start();
    }


    public boolean isCurrentLeader() {
        return isLeader;
    }
}