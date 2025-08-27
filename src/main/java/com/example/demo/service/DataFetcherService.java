/*
package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class DataFetcherService {

    @PostConstruct
    public void fetchData() throws Exception {
        String scriptPath = "src/main/resources/fetch_data.py";
        ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code " + exitCode);
        }
        System.out.println("Python data fetch completed.");
    }
}



 */