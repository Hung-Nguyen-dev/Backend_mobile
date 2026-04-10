package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepo extends JpaRepository<CommunityPost, Integer> {
    List<CommunityPost> findAllByOrderByCreatedAtDesc();

    List<CommunityPost> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<CommunityPost> findByTripIdOrderByCreatedAtDesc(Integer tripId);
}
