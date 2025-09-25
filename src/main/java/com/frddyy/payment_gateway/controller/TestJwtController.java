package com.frddyy.payment_gateway.controller;

import com.frddyy.payment_gateway.service.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"test", "dev"})
@RestController
@RequestMapping("/api/test")
public class TestJwtController {

    private final JwtService jwtService;

    public TestJwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/token/{username}")
    public String generateToken(@PathVariable String username) {
        return jwtService.generateToken(username);
    }
}
