package com.gradle.sdlcdemo.loancalculator.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class CrashService {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // calls System.exit after a 2-second delay
    public void crashIt() {
        executor.schedule(() -> System.exit(22), 2, TimeUnit.SECONDS);
    }
}
