package com.fitness.userservice.controller;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private UserService userservice;

    @GetMapping("/{userid}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userid){
        return ResponseEntity.ok(userservice.getUserProfile(userid));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUserProfile(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(userservice.register(request));
    }

    @GetMapping("/{userid}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userid){
        return ResponseEntity.ok(userservice.existsById(userid));
    }
}
