package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPostInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for like/save interactions on community posts.
 */
public interface CommunityPostInteractionRepo extends JpaRepository<CommunityPostInteraction, Integer> {
    /**
     * Find a specific interaction by user, post, and action type.
     */
    Optional<CommunityPostInteraction> findByPostIdAndUserIdAndActionType(Integer postId, Integer userId, String actionType);

    /**
     * List interactions for a user by action type.
     */
    java.util.List<CommunityPostInteraction> findByUserIdAndActionType(Integer userId, String actionType);

    /**
     * Count interactions for a post by action type.
     */
    long countByPostIdAndActionType(Integer postId, String actionType);

    /**
     * Delete a specific interaction by user, post, and action type.
     */
    void deleteByPostIdAndUserIdAndActionType(Integer postId, Integer userId, String actionType);
}
