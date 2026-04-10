package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.user.UpdateProfileReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getMe(@RequestHeader("X-User-Id") Integer currentUserId) {
        return ResponseEntity.ok(userService.getUser(currentUserId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateProfile(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId,
            @RequestBody UpdateProfileReq req) {
        return ResponseEntity.ok(userService.updateProfile(currentUserId, userId, req));
    }

    @GetMapping("/{userId}/trips")
    public ResponseEntity<List<Trip>> getTrips(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getTrips(currentUserId, userId));
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<List<Post>> getPosts(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getPosts(currentUserId, userId));
    }

    @GetMapping("/{userId}/posts/{actionType}")
    public ResponseEntity<List<Post>> getPostsByAction(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer userId,
            @PathVariable String actionType) {
        return ResponseEntity.ok(userService.getPostsByAction(currentUserId, userId, actionType));
    }
}