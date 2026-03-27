package com.javatechie.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class EventAuditService {

    private static final int MAX_EVENTS = 50;

    private final Deque<String> events = new ArrayDeque<>();

    public synchronized void record(String topic, String payload) {
        String entry = "%s | %s | %s".formatted(LocalDateTime.now(), topic, payload);
        events.addFirst(entry);
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public synchronized List<String> recentEvents() {
        return new ArrayList<>(events);
    }
}
