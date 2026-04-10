package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepo extends JpaRepository<UserInteraction, Integer> {
    Optional<UserInteraction> findByUserIdAndPostIdAndActionType(Integer userId, Integer postId, String actionType);

    List<UserInteraction> findByUserIdAndActionType(Integer userId, String actionType);

    List<UserInteraction> findByPostIdAndActionType(Integer postId, String actionType);

    long countByPostIdAndActionType(Integer postId, String actionType);

    void deleteByUserIdAndPostIdAndActionType(Integer userId, Integer postId, String actionType);
}