package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.user.UpdateProfileReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.user.UpdateRoleReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.model.UserInteraction;
import com.mobilebackend.ungdunglapkehoachdulich.repo.PostRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.TripRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserInteractionRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final TripRepo tripRepo;
    private final PostRepo postRepo;
    private final UserInteractionRepo userInteractionRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryStorageService cloudinaryStorageService;

    public User getUser(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));
    }

    @Transactional
    public User updateProfile(Integer currentUserId, Integer targetUserId, UpdateProfileReq req) {
        authorizeSelfOrAdmin(currentUserId, targetUserId);

        if (req == null) {
            throw new IllegalArgumentException("Thong tin cap nhat khong hop le");
        }

        User user = getUser(targetUserId);

        if (req.getUsername() != null && !req.getUsername().trim().isEmpty() && !req.getUsername().equals(user.getUsername())) {
            if (userRepo.existsByUsername(req.getUsername().trim())) {
                throw new IllegalArgumentException("Username da ton tai");
            }
            user.setUsername(req.getUsername().trim());
        }

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty() && !req.getEmail().equals(user.getEmail())) {
            if (userRepo.existsByEmail(req.getEmail().trim())) {
                throw new IllegalArgumentException("Email da ton tai");
            }
            user.setEmail(req.getEmail().trim());
        }

        if (req.getFullName() != null) {
            user.setFullName(req.getFullName());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui long nhap mat khau hien tai");
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new SecurityException("Mat khau hien tai khong dung");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return userRepo.save(user);
    }

    @Transactional
    public String uploadAvatar(Integer currentUserId, Integer targetUserId, MultipartFile image) throws Exception {
        authorizeSelfOrAdmin(currentUserId, targetUserId);

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Anh khong hop le");
        }

        String contentType = image.getContentType() == null ? "" : image.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phai la anh");
        }

        User user = getUser(targetUserId);
        String avatarUrl = cloudinaryStorageService.uploadImage(image, "avatars");
        user.setAvatarUrl(avatarUrl);
        userRepo.save(user);
        return avatarUrl;
    }

    public List<Trip> getTrips(Integer currentUserId, Integer targetUserId) {
        authorizeSelfOrAdmin(currentUserId, targetUserId);
        return tripRepo.findByUserIdOrderByIdDesc(targetUserId);
    }

    public List<Post> getPosts(Integer currentUserId, Integer targetUserId) {
        authorizeSelfOrAdmin(currentUserId, targetUserId);
        return postRepo.findByUserIdOrderByIdDesc(targetUserId);
    }

    public List<Post> getPostsByAction(Integer currentUserId, Integer targetUserId, String actionType) {
        authorizeSelfOrAdmin(currentUserId, targetUserId);

        List<UserInteraction> interactions = userInteractionRepo.findByUserIdAndActionType(targetUserId, normalizeActionType(actionType));
        return interactions.stream()
                .map(interaction -> postRepo.findById(interaction.getPostId()).orElse(null))
                .filter(post -> post != null)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers(Integer currentUserId) {
        requireAdmin(currentUserId);
        return userRepo.findAll();
    }

    @Transactional
    public User updateRole(Integer currentUserId, Integer targetUserId, UpdateRoleReq req) {
        requireAdmin(currentUserId);
        if (req == null || req.getRole() == null || req.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("Role khong hop le");
        }

        User user = getUser(targetUserId);
        user.setRole(req.getRole().trim().toUpperCase());
        return userRepo.save(user);
    }

    private void authorizeSelfOrAdmin(Integer currentUserId, Integer targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new IllegalArgumentException("userId khong duoc de trong");
        }
        if (currentUserId.equals(targetUserId)) {
            return;
        }
        requireAdmin(currentUserId);
    }

    private void requireAdmin(Integer currentUserId) {
        User currentUser = getUser(currentUserId);
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new SecurityException("Ban khong co quyen truy cap chuc nang nay");
        }
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            throw new IllegalArgumentException("actionType khong hop le");
        }
        return actionType.trim().toUpperCase();
    }
}