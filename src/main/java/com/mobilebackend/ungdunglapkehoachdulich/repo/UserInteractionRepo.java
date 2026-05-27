package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user interactions on legacy posts.
 */
public interface UserInteractionRepo extends JpaRepository<UserInteraction, Integer> {
    /**
     * Find a specific interaction by user, post, and action type.
     */
    Optional<UserInteraction> findByUserIdAndPostIdAndActionType(Integer userId, Integer postId, String actionType);

    /**
     * List interactions by user and action type.
     */
    List<UserInteraction> findByUserIdAndActionType(Integer userId, String actionType);

    /**
     * List interactions by post and action type.
     */
    List<UserInteraction> findByPostIdAndActionType(Integer postId, String actionType);

    /**
     * Count interactions by post and action type.
     */
    long countByPostIdAndActionType(Integer postId, String actionType);

    /**
     * Delete interaction by user, post, and action type.
     */
    void deleteByUserIdAndPostIdAndActionType(Integer userId, Integer postId, String actionType);
}