package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.user.UpdateRoleReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@RequestHeader("X-User-Id") Integer currentUserId) {
        return ResponseEntity.ok(userService.getAllUsers(currentUserId));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<User> updateRole(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId,
            @RequestBody UpdateRoleReq req) {
        return ResponseEntity.ok(userService.updateRole(currentUserId, userId, req));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId) {
        userService.deleteUser(currentUserId, userId);
        return ResponseEntity.noContent().build();
    }
}