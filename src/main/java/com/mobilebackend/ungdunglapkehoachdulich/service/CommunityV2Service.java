package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityPostUpdateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.community.CommunityToggleRes;
import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityFollow;
import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPost;
import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPostInteraction;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityV2Service {

    private static final String ACTION_LIKE = "LIKE";
    private static final String ACTION_SAVE = "SAVE";

    private final CommunityPostRepo communityPostRepo;
    private final CommunityPostInteractionRepo communityPostInteractionRepo;
    private final CommunityFollowRepo communityFollowRepo;
    private final UserRepo userRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    @Transactional
    public CommunityPostRes createPost(Integer currentUserId, CommunityPostCreateReq req) {
        validateUserExists(currentUserId);
        validateCreateReq(req);

        Integer tripId = req.getTripId();
        Integer isTripLinked = 0;

        if (tripId != null) {
            Trip trip = tripRepo.findById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Trip khong ton tai"));

            boolean isOwner = trip.getUserId().equals(currentUserId);
            boolean isMember = tripMemberRepo.findByTripIdAndUserId(tripId, currentUserId)
                    .map(member -> member.getStatus() != null && member.getStatus() == 1)
                    .orElse(false);

            if (!isOwner && !isMember) {
                throw new SecurityException("Ban khong thuoc trip nay de dang bai lien ket trip");
            }
            isTripLinked = 1;
        }

        CommunityPost saved = communityPostRepo.save(CommunityPost.builder()
                .userId(currentUserId)
                .tripId(tripId)
                .isTripLinked(isTripLinked)
                .title(req.getTitle().trim())
                .content(req.getContent())
            .imageUrl(req.getImageUrl())
                .location(req.getLocation())
                .budget(req.getBudget())
                .createdAt(LocalDateTime.now())
                .build());

        return toRes(saved, currentUserId);
    }

    public List<CommunityPostRes> getFeed(Integer currentUserId) {
        List<CommunityPost> posts = communityPostRepo.findAllByOrderByCreatedAtDesc();
        return posts.stream().map(post -> toRes(post, currentUserId)).toList();
    }

    public CommunityPostRes getPost(Integer currentUserId, Integer postId) {
        CommunityPost post = findPost(postId);
        return toRes(post, currentUserId);
    }

    public List<CommunityPostRes> getPostsByTrip(Integer currentUserId, Integer tripId) {
        if (!tripRepo.existsById(tripId)) {
            throw new IllegalArgumentException("Trip khong ton tai");
        }
        List<CommunityPost> posts = communityPostRepo.findByTripIdOrderByCreatedAtDesc(tripId);
        return posts.stream().map(post -> toRes(post, currentUserId)).toList();
    }

    public List<CommunityPostRes> getPostsByUser(Integer currentUserId, Integer userId) {
        validateUserExists(userId);
        List<CommunityPost> posts = communityPostRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return posts.stream().map(post -> toRes(post, currentUserId)).toList();
    }

    public List<CommunityPostRes> getSavedPosts(Integer currentUserId, Integer userId) {
        if (currentUserId != null && !currentUserId.equals(userId)) {
            throw new SecurityException("Chi duoc xem saved post cua chinh minh");
        }
        validateUserExists(userId);

        List<CommunityPostInteraction> saves = communityPostInteractionRepo.findByUserIdAndActionType(userId, ACTION_SAVE);
        return saves.stream()
                .map(interaction -> communityPostRepo.findById(interaction.getPostId()).orElse(null))
                .filter(post -> post != null)
                .map(post -> toRes(post, userId))
                .toList();
    }

    @Transactional
    public CommunityPostRes updatePost(Integer currentUserId, Integer postId, CommunityPostUpdateReq req) {
        CommunityPost post = findPost(postId);
        if (!post.getUserId().equals(currentUserId)) {
            throw new SecurityException("Chi tac gia moi duoc sua bai");
        }
        if (req == null || isBlank(req.getTitle())) {
            throw new IllegalArgumentException("Thong tin cap nhat khong hop le");
        }

        post.setTitle(req.getTitle().trim());
        post.setContent(req.getContent());
        post.setImageUrl(req.getImageUrl());
        post.setLocation(req.getLocation());
        post.setBudget(req.getBudget());
        post = communityPostRepo.save(post);

        return toRes(post, currentUserId);
    }

    @Transactional
    public void deletePost(Integer currentUserId, Integer postId) {
        CommunityPost post = findPost(postId);
        if (!post.getUserId().equals(currentUserId)) {
            throw new SecurityException("Chi tac gia moi duoc xoa bai");
        }
        communityPostRepo.delete(post);
    }

    @Transactional
    public CommunityToggleRes toggleLike(Integer currentUserId, Integer postId) {
        return togglePostAction(currentUserId, postId, ACTION_LIKE);
    }

    @Transactional
    public CommunityToggleRes toggleSave(Integer currentUserId, Integer postId) {
        return togglePostAction(currentUserId, postId, ACTION_SAVE);
    }

    @Transactional
    public CommunityToggleRes toggleFollowUser(Integer currentUserId, Integer targetUserId) {
        validateUserExists(currentUserId);
        validateUserExists(targetUserId);

        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Khong the follow chinh minh");
        }

        CommunityFollow existing = communityFollowRepo
                .findByFollowerUserIdAndFolloweeUserId(currentUserId, targetUserId)
                .orElse(null);

        int active;
        if (existing == null) {
            communityFollowRepo.save(CommunityFollow.builder()
                    .followerUserId(currentUserId)
                    .followeeUserId(targetUserId)
                    .createdAt(LocalDateTime.now())
                    .build());
            active = 1;
        } else {
            communityFollowRepo.delete(existing);
            active = 0;
        }

        return CommunityToggleRes.builder()
                .userId(currentUserId)
                .targetId(targetUserId)
                .action("FOLLOW")
                .active(active)
                .totalCount(communityFollowRepo.countByFolloweeUserId(targetUserId))
                .build();
    }

    private CommunityToggleRes togglePostAction(Integer currentUserId, Integer postId, String actionType) {
        validateUserExists(currentUserId);
        findPost(postId);

        CommunityPostInteraction existing = communityPostInteractionRepo
                .findByPostIdAndUserIdAndActionType(postId, currentUserId, actionType)
                .orElse(null);

        int active;
        if (existing == null) {
            communityPostInteractionRepo.save(CommunityPostInteraction.builder()
                    .postId(postId)
                    .userId(currentUserId)
                    .actionType(actionType)
                    .createdAt(LocalDateTime.now())
                    .build());
            active = 1;
        } else {
            communityPostInteractionRepo.delete(existing);
            active = 0;
        }

        return CommunityToggleRes.builder()
                .userId(currentUserId)
                .targetId(postId)
                .action(actionType)
                .active(active)
                .totalCount(communityPostInteractionRepo.countByPostIdAndActionType(postId, actionType))
                .build();
    }

    private CommunityPost findPost(Integer postId) {
        return communityPostRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Community post khong ton tai"));
    }

    private void validateUserExists(Integer userId) {
        if (userId == null || !userRepo.existsById(userId)) {
            throw new IllegalArgumentException("User khong ton tai");
        }
    }

    private void validateCreateReq(CommunityPostCreateReq req) {
        if (req == null || isBlank(req.getTitle())) {
            throw new IllegalArgumentException("Thong tin bai viet khong hop le");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private CommunityPostRes toRes(CommunityPost post, Integer currentUserId) {
        long likeCount = communityPostInteractionRepo.countByPostIdAndActionType(post.getId(), ACTION_LIKE);
        long saveCount = communityPostInteractionRepo.countByPostIdAndActionType(post.getId(), ACTION_SAVE);

        int isLiked = 0;
        int isSaved = 0;

        if (currentUserId != null) {
            isLiked = communityPostInteractionRepo.findByPostIdAndUserIdAndActionType(post.getId(), currentUserId, ACTION_LIKE).isPresent() ? 1 : 0;
            isSaved = communityPostInteractionRepo.findByPostIdAndUserIdAndActionType(post.getId(), currentUserId, ACTION_SAVE).isPresent() ? 1 : 0;
        }

        return CommunityPostRes.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .tripId(post.getTripId())
                .isTripLinked(post.getIsTripLinked())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .location(post.getLocation())
                .budget(post.getBudget())
                .createdAt(post.getCreatedAt())
                .likeCount(likeCount)
                .saveCount(saveCount)
                .isLiked(isLiked)
                .isSaved(isSaved)
                .build();
    }
}
