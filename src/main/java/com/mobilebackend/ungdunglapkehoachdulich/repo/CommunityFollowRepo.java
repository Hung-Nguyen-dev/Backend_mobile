package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for follow relationships between users.
 */
public interface CommunityFollowRepo extends JpaRepository<CommunityFollow, Integer> {
    /**
     * Find follow relationship for follower and followee.
     */
    Optional<CommunityFollow> findByFollowerUserIdAndFolloweeUserId(Integer followerUserId, Integer followeeUserId);

    /**
     * Count followers for a user.
     */
    long countByFolloweeUserId(Integer followeeUserId);

    /**
     * Delete follow relationship for follower and followee.
     */
    void deleteByFollowerUserIdAndFolloweeUserId(Integer followerUserId, Integer followeeUserId);
}
