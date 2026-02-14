package com.devops.greenkube.service;

import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.Lock;
import io.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

@Service
public class LeaderElectionService {

    private boolean isLeader = false; // THE MASTER FLAG

    @PostConstruct
    public void startElection() {
        // 1. Identify this specific pod.
        // If we scale to 3 replicas, each needs a unique name so they can fight for the lock.
        String tempPodName = System.getenv("HOSTNAME");
        if (tempPodName == null || tempPodName.isEmpty()) {
            tempPodName = "local-machine-" + UUID.randomUUID().toString();
        }

        final String podName = tempPodName;

        // 2. Define the Lock (The Lease object)
        // This targets the "coordination.k8s.io" API to create/update our Conch Shell
        Lock lock = new LeaseLock(
                "default",
                "green-kube-lock",
                podName
        );

        // 3. Configure the Timers using LeaderElectionConfig
        LeaderElectionConfig config = new LeaderElectionConfig(
                lock,
                Duration.ofSeconds(15), // LeaseDuration: How long until Standbys take over
                Duration.ofSeconds(10), // RenewDeadline: Leader tries to renew before this
                Duration.ofSeconds(2)   // RetryPeriod: The Heartbeat frequency
        );

        // 4. Start the Election in a background thread
        new Thread(() -> {
            // FIXED: The constructor ONLY takes the config object now!
            LeaderElector elector = new LeaderElector(config);

            elector.run(
                    () -> {
                        System.out.println("👑 I AM THE LEADER! (" + podName + ") Starting duties...");
                        isLeader = true;
                    },
                    () -> {
                        System.out.println("🛑 I LOST LEADERSHIP! (" + podName + ") Stopping duties...");
                        isLeader = false;
                    }
            );
        }).start();
    }

    // Other classes will ask this method if they are allowed to work
    public boolean isCurrentLeader() {
        return isLeader;
    }
}