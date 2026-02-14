package com.devops.greenkube;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class GreenKubeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenKubeApplication.class, args);
        System.out.println("GreenKubeApplication started");
    }

}
