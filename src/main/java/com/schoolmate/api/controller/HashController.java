package com.schoolmate.api.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HashController {
    
    @GetMapping("/generate-hashes")
    public Map<String, String> generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return Map.of(
            "admin123", encoder.encode("admin123"),
            "prof123", encoder.encode("prof123"),
            "apod123", encoder.encode("apod123")
        );
    }
}
