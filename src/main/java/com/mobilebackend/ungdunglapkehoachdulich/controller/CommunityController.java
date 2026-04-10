package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.post.InteractionStateRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.post.PostCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.post.PostUpdateReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @RequestBody PostCreateReq req) {
        return ResponseEntity.ok(communityService.createPost(currentUserId, req));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable Integer postId) {
        return ResponseEntity.ok(communityService.getPost(postId));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Post> updatePost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId,
            @RequestBody PostUpdateReq req) {
        return ResponseEntity.ok(communityService.updatePost(currentUserId, postId, req));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId) {
        communityService.deletePost(currentUserId, postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/interactions/{actionType}")
    public ResponseEntity<InteractionStateRes> toggleInteraction(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId,
            @PathVariable String actionType) {
        return ResponseEntity.ok(communityService.toggleInteraction(currentUserId, postId, actionType));
    }

    @DeleteMapping("/{postId}/interactions/{actionType}")
    public ResponseEntity<Void> removeInteraction(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId,
            @PathVariable String actionType) {
        communityService.removeInteraction(currentUserId, postId, actionType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/actions/{actionType}")
    public ResponseEntity<List<Post>> getPostsByAction(
            @PathVariable Integer userId,
            @PathVariable String actionType) {
        return ResponseEntity.ok(communityService.getPostsByAction(userId, actionType));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Post>> getPostsByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(communityService.getPostsByUser(userId));
    }
}