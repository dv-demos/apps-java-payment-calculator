package com.gradle.sdlcdemo.loancalculator.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!cloud && !kubernetes")
public class MemoryHitCounterService implements HitCounterService {

    private long hitCount = 0;

    @Override
    public long incrementCounter() {
        return ++hitCount;
    }

    @Override
    public void resetCount() {
        hitCount = 0;
    }
}
