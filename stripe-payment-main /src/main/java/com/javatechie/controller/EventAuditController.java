package com.javatechie.controller;

import com.javatechie.service.EventAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventAuditController {

    private final EventAuditService eventAuditService;

    public EventAuditController(EventAuditService eventAuditService) {
        this.eventAuditService = eventAuditService;
    }

    @GetMapping("/recent")
    public List<String> recentEvents() {
        return eventAuditService.recentEvents();
    }
}
