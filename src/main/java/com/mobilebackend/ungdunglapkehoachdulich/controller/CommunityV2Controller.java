package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostUpdateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityToggleRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.CloudinaryStorageService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CommunityV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v2/community")
@RequiredArgsConstructor
public class CommunityV2Controller {

    private final CommunityV2Service communityV2Service;
    private final CloudinaryStorageService cloudinaryStorageService;

    /**
     * Upload image for community posts.
     */
    @PostMapping(value = "/uploads/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @RequestPart("image") MultipartFile image) throws Exception {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Yeu cau dang nhap");
        }
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Anh khong hop le");
        }

        String contentType = image.getContentType() == null ? "" : image.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phai la anh");
        }

        String imageUrl = cloudinaryStorageService.uploadImage(image, "community");

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    /**
     * Create a new community post.
     */
    @PostMapping("/posts")
    public ResponseEntity<CommunityPostRes> createPost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @RequestBody CommunityPostCreateReq req) {
        return ResponseEntity.ok(communityV2Service.createPost(currentUserId, req));
    }

    /**
     * Get community feed.
     */
    @GetMapping("/posts")
    public ResponseEntity<List<CommunityPostRes>> getFeed(
            @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId) {
        return ResponseEntity.ok(communityV2Service.getFeed(currentUserId));
    }

    /**
     * Get a single community post.
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityPostRes> getPost(
            @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId,
            @PathVariable Integer postId) {
        return ResponseEntity.ok(communityV2Service.getPost(currentUserId, postId));
    }

    /**
     * Update a community post.
     */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<CommunityPostRes> updatePost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId,
            @RequestBody CommunityPostUpdateReq req) {
        return ResponseEntity.ok(communityV2Service.updatePost(currentUserId, postId, req));
    }

    /**
     * Delete a community post.
     */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId) {
        communityV2Service.deletePost(currentUserId, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get posts linked to a trip.
     */
    @GetMapping("/trips/{tripId}/posts")
    public ResponseEntity<List<CommunityPostRes>> getPostsByTrip(
            @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId,
            @PathVariable Integer tripId) {
        return ResponseEntity.ok(communityV2Service.getPostsByTrip(currentUserId, tripId));
    }

    /**
     * Get posts by user.
     */
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<CommunityPostRes>> getPostsByUser(
            @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId,
            @PathVariable Integer userId) {
        return ResponseEntity.ok(communityV2Service.getPostsByUser(currentUserId, userId));
    }

    /**
     * Get saved posts of a user (self only).
     */
    @GetMapping("/users/{userId}/saved")
    public ResponseEntity<List<CommunityPostRes>> getSavedPosts(
            @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId,
            @PathVariable Integer userId) {
        return ResponseEntity.ok(communityV2Service.getSavedPosts(currentUserId, userId));
    }

    /**
     * Toggle like for a post.
     */
    @PostMapping("/posts/{postId}/likes/toggle")
    public ResponseEntity<CommunityToggleRes> toggleLike(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId) {
        return ResponseEntity.ok(communityV2Service.toggleLike(currentUserId, postId));
    }

    /**
     * Toggle save for a post.
     */
    @PostMapping("/posts/{postId}/saves/toggle")
    public ResponseEntity<CommunityToggleRes> toggleSave(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer postId) {
        return ResponseEntity.ok(communityV2Service.toggleSave(currentUserId, postId));
    }

    /**
     * Toggle follow/unfollow a user.
     */
    @PostMapping("/users/{targetUserId}/follow/toggle")
    public ResponseEntity<CommunityToggleRes> toggleFollow(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer targetUserId) {
        return ResponseEntity.ok(communityV2Service.toggleFollowUser(currentUserId, targetUserId));
    }
}
