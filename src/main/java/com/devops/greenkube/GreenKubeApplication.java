package com.devops.greenkube;


import com.devops.greenkube.service.LeaderElectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class GreenKubeApplication {
    @Autowired
    LeaderElectionService leaderElectionService;

    public static void main(String[] args) {

        SpringApplication.run(GreenKubeApplication.class, args);
        System.out.println("GreenKubeApplication started");
    }

}
