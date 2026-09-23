package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.LoginRequest;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Employee not found"));
        
        String token = jwtUtil.generateToken(employee.getId(), employee.getRole());
        return Map.of("token", token);
    }
}
