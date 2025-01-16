package com.gradle.sdlcdemo.loancalculator.service;

import org.springframework.stereotype.Service;

@Service
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
