package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPostInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostInteractionRepo extends JpaRepository<CommunityPostInteraction, Integer> {
    Optional<CommunityPostInteraction> findByPostIdAndUserIdAndActionType(Integer postId, Integer userId, String actionType);

    java.util.List<CommunityPostInteraction> findByUserIdAndActionType(Integer userId, String actionType);

    long countByPostIdAndActionType(Integer postId, String actionType);

    void deleteByPostIdAndUserIdAndActionType(Integer postId, Integer userId, String actionType);
}
