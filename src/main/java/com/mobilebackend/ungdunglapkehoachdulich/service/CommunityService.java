package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.post.InteractionStateRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.post.PostCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.post.PostUpdateReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.model.UserInteraction;
import com.mobilebackend.ungdunglapkehoachdulich.repo.PostRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserInteractionRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final List<String> ALLOWED_ACTION_TYPES = List.of("LIKE", "SAVE", "FOLLOW");

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final UserInteractionRepo userInteractionRepo;

    @Transactional
    public Post createPost(Integer userId, PostCreateReq req) {
        validateUser(userId);
        validatePostReq(req == null ? null : req.getTitle());

        Post post = Post.builder()
                .userId(userId)
                .title(req.getTitle().trim())
                .content(req.getContent())
                .location(req.getLocation())
                .budget(req.getBudget())
                .build();

        return postRepo.save(post);
    }

    @Transactional
    public Post updatePost(Integer currentUserId, Integer postId, PostUpdateReq req) {
        Post post = getPost(postId);
        authorizePostOwnerOrAdmin(currentUserId, post);
        validatePostReq(req == null ? null : req.getTitle());

        post.setTitle(req.getTitle().trim());
        post.setContent(req.getContent());
        post.setLocation(req.getLocation());
        post.setBudget(req.getBudget());

        return postRepo.save(post);
    }

    @Transactional
    public void deletePost(Integer currentUserId, Integer postId) {
        Post post = getPost(postId);
        authorizePostOwnerOrAdmin(currentUserId, post);
        postRepo.delete(post);
    }

    public Post getPost(Integer postId) {
        return postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post khong ton tai"));
    }

    public List<Post> getPostsByUser(Integer userId) {
        validateUser(userId);
        return postRepo.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public InteractionStateRes toggleInteraction(Integer userId, Integer postId, String actionType) {
        validateUser(userId);
        getPost(postId);
        String normalizedActionType = normalizeActionType(actionType);

        UserInteraction existing = userInteractionRepo
                .findByUserIdAndPostIdAndActionType(userId, postId, normalizedActionType)
                .orElse(null);

        boolean active;
        if (existing != null) {
            userInteractionRepo.delete(existing);
            active = false;
        } else {
            userInteractionRepo.save(UserInteraction.builder()
                    .userId(userId)
                    .postId(postId)
                    .actionType(normalizedActionType)
                    .build());
            active = true;
        }

        return InteractionStateRes.builder()
                .userId(userId)
                .postId(postId)
                .actionType(normalizedActionType)
                .active(active)
                .totalCount(userInteractionRepo.countByPostIdAndActionType(postId, normalizedActionType))
                .build();
    }

    @Transactional
    public void removeInteraction(Integer userId, Integer postId, String actionType) {
        validateUser(userId);
        getPost(postId);
        userInteractionRepo.deleteByUserIdAndPostIdAndActionType(userId, postId, normalizeActionType(actionType));
    }

    public List<Post> getPostsByAction(Integer userId, String actionType) {
        validateUser(userId);
        String normalizedActionType = normalizeActionType(actionType);
        return userInteractionRepo.findByUserIdAndActionType(userId, normalizedActionType).stream()
                .map(interaction -> postRepo.findById(interaction.getPostId()).orElse(null))
                .filter(post -> post != null)
                .toList();
    }

    private void validatePostReq(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Thong tin post khong hop le");
        }
    }

    private void validateUser(Integer userId) {
        if (userId == null || !userRepo.existsById(userId)) {
            throw new IllegalArgumentException("User khong ton tai");
        }
    }

    private void authorizePostOwnerOrAdmin(Integer currentUserId, Post post) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("userId khong duoc de trong");
        }
        if (post.getUserId().equals(currentUserId)) {
            return;
        }

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new SecurityException("Ban khong co quyen sua/xoa post nay");
        }
    }

    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            throw new IllegalArgumentException("actionType khong hop le");
        }
        String normalized = actionType.trim().toUpperCase();
        if (!ALLOWED_ACTION_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Chi ho tro LIKE, SAVE, FOLLOW");
        }
        return normalized;
    }
}