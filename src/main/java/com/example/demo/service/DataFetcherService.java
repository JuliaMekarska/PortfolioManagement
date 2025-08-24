/*
package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class DataFetcherService {

    @PostConstruct
    public void fetchData() throws Exception {
        // Path to your Python script; adjust as needed
        String scriptPath = "src/main/resources/fetch_data.py";
        ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Optionally read output/errors
        // InputStreamReader reader = new InputStreamReader(process.getInputStream());
        // ...

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // handle errors
            throw new RuntimeException("Python script exited with code " + exitCode);
        }
        System.out.println("Python data fetch completed.");
    }
}



 */