package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for community post queries.
 */
public interface CommunityPostRepo extends JpaRepository<CommunityPost, Integer> {
    /**
     * Get all posts ordered by creation time.
     */
    List<CommunityPost> findAllByOrderByCreatedAtDesc();

    /**
     * Get posts authored by a user.
     */
    List<CommunityPost> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * Get posts linked to a trip.
     */
    List<CommunityPost> findByTripIdOrderByCreatedAtDesc(Integer tripId);
}
