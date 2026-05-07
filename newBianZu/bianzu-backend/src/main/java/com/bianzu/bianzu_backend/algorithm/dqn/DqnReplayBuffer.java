package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnExperience;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

@Component
public class DqnReplayBuffer {

    private static final int DEFAULT_CAPACITY = 10000;

    private final Queue<DqnExperience> buffer = new ArrayDeque<>();

    public synchronized void add(DqnExperience experience) {
        if (experience == null) {
            return;
        }
        while (buffer.size() >= DEFAULT_CAPACITY) {
            buffer.poll();
        }
        buffer.offer(experience);
    }

    public synchronized List<DqnExperience> sample(int batchSize) {
        List<DqnExperience> items = new ArrayList<>(buffer);
        Collections.shuffle(items);
        return items.stream()
                .limit(Math.max(batchSize, 0))
                .toList();
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized void clear() {
        buffer.clear();
    }
}
