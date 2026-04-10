package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityFollowRepo extends JpaRepository<CommunityFollow, Integer> {
    Optional<CommunityFollow> findByFollowerUserIdAndFolloweeUserId(Integer followerUserId, Integer followeeUserId);

    long countByFolloweeUserId(Integer followeeUserId);

    void deleteByFollowerUserIdAndFolloweeUserId(Integer followerUserId, Integer followeeUserId);
}
