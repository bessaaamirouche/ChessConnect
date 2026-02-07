package com.chessconnect.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Value("${app.maintenance.enabled:false}")
    private boolean maintenanceEnabled;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", maintenanceEnabled,
            "message", maintenanceEnabled
                ? "mychess est actuellement en maintenance. Les réservations et paiements sont temporairement suspendus."
                : ""
        ));
    }
}
